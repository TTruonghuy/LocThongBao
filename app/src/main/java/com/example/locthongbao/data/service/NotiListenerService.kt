package com.example.locthongbao.data.service

import android.app.Notification
import android.content.Intent
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.toString


class NotiListenerService : NotificationListenerService () {

    companion object {
        private const val TAG = "CleanNotiService"
        private const val CHANNEL_ID = "loc_thong_bao_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val DEBUG_TOAST = true
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning

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
    private val errorHandler = CoroutineExceptionHandler { _, e ->
        ServiceDebug.lastError = "DB: " + e.stackTraceToString().take(600)
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + errorHandler)

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
            ServiceDebug.lastError = "FGS: " + e.stackTraceToString().take(600)
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
        ServiceDebug.postedCount++
        try {
            if (DEBUG_TOAST){
                android.widget.Toast.makeText(
                    applicationContext,
                    "Bắt được thông báo từ: ${sbn.packageName}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            handle(sbn)
        } catch (e: Exception) {
            ServiceDebug.lastError = "POST: " + e.stackTraceToString().take(600)
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
                    if(sbn.isClearable) {
                        cancelNotification(sbn.key)
                    }
                    val finalEntity = entity.copy(
                        isFiltered = true,
                        filterReason = result.reason
                    )
                    scope.launch  (Dispatchers.IO) {
                    AppDatabase.getDatabase(applicationContext).notificationDao()
                        .insertNotification(finalEntity)
                    }
                }
                FilterCategory.PASSED -> {

                    // Nhóm 3: Hợp lệ -> Lưu DB với cờ isFiltered = false
                    val finalEntity = entity.copy(
                        isFiltered = false,
                        filterReason = result.reason
                    )
                    scope.launch(Dispatchers.IO){
                        AppDatabase.getDatabase(applicationContext).notificationDao()
                            .insertNotification(finalEntity)
                    }

                }







        }

    }

    private val saveMutex = Mutex()

    private suspend fun save(n: NotificationEntity) {
        saveMutex.withLock {
            val dao = AppDatabase.getDatabase(applicationContext).notificationDao()
            val last = dao.latestByKey(n.notificationKey)
            when {
                last == null -> dao.insertNotification(n)
                last.title == n.title && last.text == n.text -> Unit          // đăng lại y hệt
                last.title.shape() == n.title.shape() &&
                        last.text.shape() == n.text.shape() ->
                    dao.update(n.copy(id = last.id))                          // chỉ khác số
                else -> dao.insertNotification(n)                             // nội dung thật sự khác
            }
        }
    }
}

private val DIGITS = Regex("\\d+([.,]\\d+)*")
private fun String.shape() = replace(DIGITS, "#")


