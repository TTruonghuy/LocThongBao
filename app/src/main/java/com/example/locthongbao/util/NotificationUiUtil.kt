package com.example.locthongbao.util

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.Calendar
import java.util.Locale

// Lấy icon app từ packageName, lỗi thì trả null
fun getAppIcon(packageName: String, packageManager: PackageManager): ImageBitmap? {
    return try {
        val drawable = packageManager.getApplicationIcon(packageName)
        drawableToImageBitmap(drawable)
    } catch (e: Exception) {
        null
    }
}

// Đổi Drawable sang ImageBitmap để Compose render; AdaptiveIcon thì vẽ lên Canvas rồi mới convert
private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap.asImageBitmap()
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
}

// Format timestamp thành HH:mm theo giờ máy
fun formatNotificationTime(postTime: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = postTime }
    return String.format(
        Locale.getDefault(), "%02d:%02d",
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE)
    )
}