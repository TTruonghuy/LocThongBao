package com.example.locthongbao.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.example.locthongbao.data.local.AppDatabase
import com.example.locthongbao.data.local.NotificationEntity
import com.example.locthongbao.domain.model.FilterCategory
import com.example.locthongbao.domain.model.NotificationFilter
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

class NotiListenerService : NotificationListenerService() {

    companion object {
        private const val CHANNEL_ID = "loc_thong_bao_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val DELETE_SPAM = true

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        private val _lastCancelTrace = MutableStateFlow<String?>(null)
        val lastCancelTrace: StateFlow<String?> = _lastCancelTrace

        private val _lastError = MutableStateFlow<String?>(null)
        val lastError: StateFlow<String?> = _lastError

        @Volatile
        var lastAdResult: String? = null

        // Nguồn quảng cáo hệ thống: xử lý gỡ khay trực tiếp, không lưu DB
        private val AD_SOURCES = setOf(
            "com.miui.msa.global",
            "com.miui.systemadsolution",
            "com.xiaomi.mipicks"
        )

        fun reviveService(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestRebind(ComponentName(context, NotiListenerService::class.java))
                } catch (_: Exception) {
                }
            }
        }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, e -> report("coroutine", e) }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + exceptionHandler)
    private val saveMutex = Mutex()
    private val recentCancel = ConcurrentHashMap<String, Long>()

    private fun report(tag: String, e: Throwable) {
        _lastError.value = "$tag: ${e::class.simpleName}: ${e.message}"
    }

    private fun isBlockedAdOrMsaPackage(p: String?) = p?.trim()?.lowercase() in AD_SOURCES

    private fun resolveAppName(packageName: String): String =
        try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (_: Exception) {
            packageName
        }

    private fun safeCancel(key: String) {
        try {
            cancelNotification(key)
        } catch (e: Exception) {
            report("cancel", e)
        }
    }

    private fun buildEntity(sbn: StatusBarNotification): NotificationEntity {
        val n = sbn.notification
        val extras = n.extras
        val subst = extras.getCharSequence("android.substName")?.toString()
        return NotificationEntity(
            notificationKey = sbn.key,
            packageName = sbn.packageName,
            appName = subst ?: resolveAppName(sbn.packageName),
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty(),
            text = (extras.getCharSequence(Notification.EXTRA_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString().orEmpty(),
            subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
            postTime = sbn.postTime,
            category = n.category,
            channelId = n.channelId,
            channelName = null,
            flags = n.flags,
            isOngoing = sbn.isOngoing,
            isClearable = sbn.isClearable,
            isGroupSummary = (n.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            groupKey = sbn.groupKey,
            userId = sbn.user.hashCode(),
            color = n.color,
            extrasJson = null,
            isFiltered = false,
            filterReason = "",
            isManualOverride = false
        )
    }

    override fun onCreate() {
        super.onCreate()
        try {
            createNotificationChannel()
            startForegroundServiceNotification()
        } catch (e: Exception) {
            report("fgs", e)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceRunning.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceRunning.value = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, NotiListenerService::class.java))
            } catch (e: Exception) {
                report("rebind", e)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dịch vụ Lọc Thông Báo Ngầm",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Giúp ứng dụng duy trì chạy ngầm ổn định" }
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LocThongBao đang chạy")
            .setContentText("Đang giám sát và lọc thông báo ngầm...")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.packageName == packageName) return

        try {
            if (isBlockedAdOrMsaPackage(sbn.packageName)) {
                scope.launch {
                    val (status, detail) = tryRemove(sbn)
                    val trace = "${sbn.packageName} (MSA): $status ($detail)"
                    _lastCancelTrace.value = trace
                    lastAdResult = trace
                    delay(300)
                    cleanupOrphanSummaries(sbn.packageName)
                }
                return
            }

            val n = sbn.notification
            val extras = n.extras
            val maxProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
            val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
            if (maxProgress > 0 || indeterminate || n.category == Notification.CATEGORY_PROGRESS) return

            val entity = buildEntity(sbn)
            val result = NotificationFilter.evaluate(applicationContext, entity)

            when (result.category) {
                FilterCategory.IGNORED -> return
                FilterCategory.SPAM -> processSpam(sbn, entity, result.reason)
                FilterCategory.PASSED -> scope.launch {
                    save(entity.copy(isFiltered = false, filterReason = result.reason))
                }
            }
        } catch (e: Exception) {
            report("posted", e)
        }
    }

    private fun processSpam(
        sbn: StatusBarNotification,
        entity: NotificationEntity,
        why: String
    ) {
        scope.launch {
            save(entity.copy(isFiltered = true, filterReason = "PENDING|$why"))

            val (status, detail) = if (DELETE_SPAM) tryRemove(sbn) else "OFF" to ""
            val trace = "${sbn.packageName}: $status ($detail)"
            _lastCancelTrace.value = trace
            lastAdResult = trace

            val finalReason = when (status) {
                "NOT_CLEARABLE" -> "$why - không thể bỏ qua do: không clearable"
                "CANCEL_IGNORED" -> "$why - không thể bỏ qua do: hệ thống/ROM bỏ qua lệnh gỡ"
                "REPOSTED" -> "$why - không thể bỏ qua do: thông báo bị đẩy lại ngay"
                else -> "$status|$why"
            }

            try {
                AppDatabase.getDatabase(applicationContext).notificationDao()
                    .setReason(entity.notificationKey, entity.postTime, finalReason)
            } catch (e: Exception) {
                report("setReason", e)
            }

            delay(300)
            cleanupOrphanSummaries(sbn.packageName)
        }
    }

    private fun activeState(sbn: StatusBarNotification): String {
        val now = try {
            activeNotifications?.firstOrNull { it.key == sbn.key }
        } catch (e: Exception) {
            report("active", e)
            null
        }
        return when {
            now == null -> "GONE"
            now.postTime > sbn.postTime -> "REPOSTED"
            else -> "CANCEL_IGNORED"
        }
    }

    private suspend fun tryRemove(sbn: StatusBarNotification): Pair<String, String> {
        val n = sbn.notification
        val detail = "clearable=${sbn.isClearable} flags=0x${n.flags.toString(16)} ch=${n.channelId}"
        if (!sbn.isClearable) return "NOT_CLEARABLE" to detail

        recentCancel[sbn.packageName] = System.currentTimeMillis()
        safeCancel(sbn.key)
        delay(800)
        when (activeState(sbn)) {
            "GONE" -> return "REMOVED" to detail
            "REPOSTED" -> return "REPOSTED" to detail
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                snoozeNotification(sbn.key, 24L * 60 * 60 * 1000)
            } catch (e: Exception) {
                report("snooze", e)
            }
            delay(800)
            if (activeState(sbn) == "GONE") return "SNOOZED" to detail
        }
        return "CANCEL_IGNORED" to detail
    }

    private fun hasText(s: StatusBarNotification): Boolean {
        val e = s.notification.extras
        return !e.getCharSequence(Notification.EXTRA_TITLE).isNullOrBlank() ||
                !e.getCharSequence(Notification.EXTRA_TEXT).isNullOrBlank()
    }

    private fun cleanupOrphanSummaries(pkg: String) {
        val last = recentCancel[pkg] ?: return
        if (System.currentTimeMillis() - last > 15_000) return

        val actives = try {
            activeNotifications?.filter { it.packageName == pkg }
        } catch (e: Exception) {
            report("cleanup", e)
            null
        } ?: return

        fun isSummary(s: StatusBarNotification) =
            (s.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        val hasContent = actives.any { !isSummary(it) && hasText(it) }
        if (!hasContent) {
            actives.filter { (isSummary(it) || !hasText(it)) && it.isClearable }
                .forEach { safeCancel(it.key) }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val pkg = sbn?.packageName ?: return
        if (pkg == packageName) return
        scope.launch {
            delay(300)
            cleanupOrphanSummaries(pkg)
        }
    }

    private suspend fun save(n: NotificationEntity) {
        saveMutex.withLock {
            try {
                val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                val last = dao.latestByKey(n.notificationKey)
                when {
                    last == null -> dao.insertNotification(n)
                    last.title == n.title && last.text == n.text -> dao.update(n.copy(id = last.id))
                    last.title.shape() == n.title.shape() &&
                            last.text.shape() == n.text.shape() -> dao.update(n.copy(id = last.id))
                    else -> dao.insertNotification(n)
                }
            } catch (e: Exception) {
                report("save", e)
            }
        }
    }
}

private val DIGITS = Regex("\\d+([.,]\\d+)*")
private fun String.shape() = replace(DIGITS, "#")