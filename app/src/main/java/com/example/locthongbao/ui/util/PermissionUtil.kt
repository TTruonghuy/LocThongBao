package com.example.locthongbao.ui.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object PermissionUtil {
    fun isNotificationListenerEnable(context: Context): Boolean {
        val packageName = context.packageName
        try {
            val enabledPackage = NotificationManagerCompat.getEnabledListenerPackages(context)
            if (enabledPackage.contains(packageName)) {
                return true
            }
        } catch (e: Exception) {
            Log.e("APP", "Lỗi check quyen ", e)
        }
        return false
    }
    fun openNotificationAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply{
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception){
            Log.e("CleanNoti", "Lỗi không mở được cài đặt", e)
        }
    }
}