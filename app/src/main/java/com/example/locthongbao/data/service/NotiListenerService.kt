package com.example.locthongbao.data.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

import com.example.locthongbao.data.local.AppDatabase
import com.example.locthongbao.data.local.NotificationEntity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.locthongbao.domain.model.NotificationFilter
import kotlinx.coroutines.SupervisorJob
import android.os.Build
import android.content.ComponentName
import androidx.core.app.NotificationCompat
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.example.locthongbao.domain.model.FilterCategory
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class NotiListenerService : NotificationListenerService () {

    companion object {
        private const val TAG = "CleanNotiService"
        private const val CHANNEL_ID = "loc_thong_bao_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val DEBUG_TOAST = true
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

        @Volatile var lastAdResult: String? = null          // hiện lên màn hình debug

        private val AD_SOURCES = setOf(                     // khớp CHÍNH XÁC, không dùng contains
            "com.miui.msa.global", "com.miui.systemadsolution", "com.xiaomi.mipicks"
        )
        private val BLOCKABLE_PACKAGES = setOf(
            "com.shopee.vn", "com.lazada.android", "com.ss.android.ugc.trill"
        )
        private val NEVER_BLOCK_CATEGORIES = setOf(
            Notification.CATEGORY_CALL, Notification.CATEGORY_MESSAGE, Notification.CATEGORY_EMAIL,
            Notification.CATEGORY_ALARM, Notification.CATEGORY_REMINDER, Notification.CATEGORY_EVENT,
            Notification.CATEGORY_NAVIGATION, Notification.CATEGORY_TRANSPORT
        )

        fun reviveService(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val componentName = ComponentName(context, NotiListenerService::class.java)
                    requestRebind(componentName)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() )

    private fun isBlockedAdOrMsaPackage(p: String?) = p?.trim()?.lowercase() in AD_SOURCES

    private fun canBlock(sbn: StatusBarNotification): Boolean =
        sbn.isClearable &&
                sbn.packageName in BLOCKABLE_PACKAGES &&
                sbn.notification.category !in NEVER_BLOCK_CATEGORIES


    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceRunning.value = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceRunning.value = false
    }

    override fun onCreate() {
        super.onCreate()
        // Bắt buộc tạo Notification Channel và đưa service lên Foreground ngay khi khởi động
        try {
            createNotificationChannel()
            startForegroundServiceNotification()
        } catch (e: Exception) {
            //ServiceDebug.lastError = "FGS: " + e.stackTraceToString().take(600)
        }
    }

    // 1. Tạo Kênh thông báo (Bắt buộc từ Android 8.0 / API 26+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dịch vụ Lọc Thông Báo Ngầm",
                NotificationManager.IMPORTANCE_LOW // Để thông báo chạy âm thầm, không phát âm thanh làm phiền
            ).apply {
                description = "Giúp ứng dụng duy trì chạy ngầm ổn định"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    // 2. Khởi chạy Foreground Service kèm thông báo trên thanh Status Bar
    private fun startForegroundServiceNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LocThongBao đang chạy")
            .setContentText("Đang giám sát và lọc thông báo ngầm...")
            // Dùng tạm icon có sẵn của hệ thống (hoặc đổi thành R.drawable.ten_icon_cua_ban sau)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.packageName == packageName) return   // bỏ thông báo của chính app
        if (isBlockedAdOrMsaPackage(sbn.packageName)) {
            handleAdSource(sbn)
            return
        }


        val flags = sbn.notification.flags
        // Thông báo tóm tắt nhóm: không lưu, nhưng dọn nếu nhóm đã hết thông báo có nội dung
        if ((flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            scope.launch { delay(300); cleanupOrphanSummaries(sbn.packageName) }
            return
        }

        //  Rác cấu trúc
        if (!sbn.isClearable || (flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        //ChannelCollector.record(sbn.packageName, sbn.notification.channelId)
        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.trim() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()?.trim() ?: ""
        if (title.isEmpty() && text.isEmpty()) return
        try {
            handle(sbn)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error handling notification from ${sbn.packageName}", e)
        }
    }


    private fun handleAdSource(sbn: StatusBarNotification) {
        scope.launch {
            val sb = StringBuilder("AD ${sbn.packageName} clearable=${sbn.isClearable} flags=${sbn.notification.flags}")
            if (sbn.isClearable) {
                cancelNotification(sbn.key)
                delay(800)
                val still = getActiveNotifications()?.any { it.key == sbn.key } == true
                sb.append(" -> cancel: ").append(if (still) "VẪN CÒN TRONG KHAY" else "ĐÃ MẤT")
            } else sb.append(" -> không vuốt bỏ được")
            lastAdResult = sb.toString()
            android.util.Log.d(TAG, sb.toString())
            delay(300)
            cleanupOrphanSummaries(sbn.packageName)
        }
    }

    private fun handle(sbn: StatusBarNotification){
        val notification = sbn.notification
        val extras = sbn.notification.extras

        val maxProgress = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val indeterminate = extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
        val hasProgress = maxProgress > 0 || indeterminate

        if (hasProgress || notification.category == Notification.CATEGORY_PROGRESS) return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val appName = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        } catch(e: Exception){
            sbn.packageName
        }

        val entity = NotificationEntity(
            notificationKey = sbn.key,
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            subText = subText,
            postTime = sbn.postTime,
            category = notification.category,
            channelId = notification.channelId,
            channelName = null,
            flags = notification.flags,
            isOngoing = sbn.isOngoing,
            isClearable = sbn.isClearable,                       // đã sửa
            isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0,
            groupKey = sbn.groupKey,
            userId = sbn.user.hashCode(),
            color = notification.color,
            extrasJson = null,
            isFiltered = false,
            filterReason = ""
        )


            // Đánh giá thông báo qua bộ lọc 3 nhóm
            val result = NotificationFilter.evaluate(applicationContext, entity)
            when (result.category){
                FilterCategory.IGNORED ->{
                    return
                }
                FilterCategory.SPAM -> {
                    scope.launch(Dispatchers.IO) {
                        save(entity.copy(isFiltered = true, filterReason = result.reason))
                        if (canBlock(sbn)) {
                            delay(200)
                            cancelNotification(sbn.key)
                            delay(300)
                            cleanupOrphanSummaries(sbn.packageName)
                        }
                    }
                }

                FilterCategory.PASSED -> {

                    // Nhóm 3: Hợp lệ -> Lưu DB với cờ isFiltered = false
                    val finalEntity = entity.copy(
                        isFiltered = false,
                        filterReason = result.reason
                    )
                    scope.launch(Dispatchers.IO){
                        save(finalEntity)
                    }
                }
        }

    }

    private fun hasText(s: StatusBarNotification): Boolean {
        val e = s.notification.extras
        return !e.getCharSequence(Notification.EXTRA_TITLE).isNullOrBlank() ||
                !e.getCharSequence(Notification.EXTRA_TEXT).isNullOrBlank()
    }

    private fun cleanupOrphanSummaries(pkg: String) {
        if (pkg !in BLOCKABLE_PACKAGES && pkg !in AD_SOURCES) return
        val actives = getActiveNotifications()?.filter { it.packageName == pkg } ?: return
        fun isSummary(s: StatusBarNotification) =
            (s.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0

        val hasContent = actives.any { !isSummary(it) && hasText(it) }
        if (!hasContent) {
            actives.filter { (isSummary(it) || !hasText(it)) && it.isClearable }
                .forEach { cancelNotification(it.key) }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val pkg = sbn?.packageName ?: return
        if (pkg == packageName) return
        scope.launch { delay(300); cleanupOrphanSummaries(pkg) }
    }

    private val saveMutex = Mutex()

    private suspend fun save(n: NotificationEntity) {
        saveMutex.withLock {
            try {
                val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
                val last = dao.latestByKey(n.notificationKey)
                when {
                    last == null -> {
                        dao.insertNotification(n)
                        android.util.Log.d(TAG, "Inserted new: ${n.packageName} | ${n.title}")
                    }
                    last.title == n.title && last.text == n.text -> {
                        dao.update(n.copy(id= last.id))
                        android.util.Log.d(TAG, "Skipped duplicate: ${n.packageName} | ${n.title}")
                    }
                    last.title.shape() == n.title.shape() &&
                            last.text.shape() == n.text.shape() -> {
                        dao.update(n.copy(id = last.id))
                        android.util.Log.d(TAG, "Updated shaped: ${n.packageName} | ${n.title}")
                    }
                    else -> {
                        dao.insertNotification(n)
                        android.util.Log.d(TAG, "Inserted changed: ${n.packageName} | ${n.title}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Save failed for ${n.notificationKey}", e)
            }
        }
    }
}


private val DIGITS = Regex("\\d+([.,]\\d+)*")
private fun String.shape() = replace(DIGITS, "#")


