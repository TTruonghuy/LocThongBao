package com.example.locthongbao.data.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotiListenerService : NotificationListenerService () {

    companion object {
        private const val TAG = "CleanNotiService"
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        Log.d("CleanNotiDebug", "🚨 HOẠT ĐỘNG! Hệ thống vừa gọi onNotificationPosted từ app: ${sbn?.packageName}")
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        val postTime = sbn.postTime
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        val category = notification.category ?: "other"
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""
        val isOngoing = sbn.isOngoing

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(
                applicationContext,
                "Đã bắt TB từ: $packageName - $title",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

//        val notificationItem = NotificationModel(
//            packageName = packageName,
//            appName = getAppDisplayName(packageName),
//            title = title,
//            text = text,
//            timestamp = postTime,
//            category = category,
//            subText = subText
//        )

        Log.d(TAG, "📦 App:  | Nhãn (Category): $category")
        Log.d(TAG, "📌 Nhóm/SubText: $subText | Đang ghim: $isOngoing")
        Log.d(TAG, "📌 Tiêu đề: $title")
        Log.d(TAG, "💬 Nội dung: $text")
    }

//    private fun getAppDisplayName(packageName: String): String{
//        return try {
//            val pm = packageManager
//            val appInfo = pm.getApplicationInfo(packageName,0)
//            pm.getApplicationLabel(appInfo).toString()
//        } catch (e: Exception){
//            packageName
//        }
//    }

}