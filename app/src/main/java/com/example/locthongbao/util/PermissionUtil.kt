package com.example.locthongbao.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.os.PowerManager


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

    // Kiểm tra xem app đã được loại trừ khỏi tối ưu hóa pin chưa (Không hạn chế)
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return powerManager.isIgnoringBatteryOptimizations(context.packageName)
        }
        return true
    }

    // Yêu cầu người dùng bật Không hạn chế pin
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Cách 1: Mở thẳng popup hệ thống yêu cầu bỏ qua tối ưu pin
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.e("PermissionUtil", "Không mở được popup tối ưu pin, chuyển sang App Info", e)
                // Cách 2 (Fallback): Mở trang thông tin ứng dụng để người dùng tự chỉnh thủ công
                openAppSettings(context)
            }
        }
    }

    // Mở trang Cài đặt chi tiết của ứng dụng (App Info) để người dùng tự cấp quyền pin / tự khởi động
    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("PermissionUtil", "Không mở được App Info", e)
        }
    }
}