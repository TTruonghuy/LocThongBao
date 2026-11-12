package com.example.locthongbao

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.service.notification.NotificationListenerService
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.locthongbao.ui.MainViewModel
import com.example.locthongbao.ui.permission.PermissionScreen
import com.example.locthongbao.ui.theme.LocThongBaoTheme
import com.example.locthongbao.util.PermissionUtil
import com.example.locthongbao.data.local.NotificationEntity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import java.util.Calendar
import java.util.Locale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.example.locthongbao.domain.model.NotificationFilter
import com.example.locthongbao.data.service.NotiListenerService


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //NotiListenerService.reviveService(this)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val lifecycleOwner = LocalLifecycleOwner.current

            val isServiceRunning by NotiListenerService.isServiceRunning.collectAsState()
            val isGranted = PermissionUtil.isNotificationListenerEnable(this)

            //ktra trạng thái quyền
            var hasPermission by remember {
                mutableStateOf(PermissionUtil.isNotificationListenerEnable(context))
            }

            //load qyền sau khi cập nhật
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        hasPermission = PermissionUtil.isNotificationListenerEnable(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            //chuyển hướng xử lý quyền
            Surface(
                modifier = Modifier.fillMaxSize(),
                //color = MaterialTheme.colorScheme.background { }
            ) {
                if (!hasPermission || !isGranted || !isServiceRunning) {
                    PermissionScreen (
                        isServiceRunning = isServiceRunning,
                        onPermissionGranted = {
                            com.example.locthongbao.data.service.NotiListenerService.reviveService(this)
                        }
                    )
                } else {
                    MainDashboardScreen()
                }

            }
        }
    }
}



@Composable
fun MainDashboardScreen(viewModel: MainViewModel = viewModel()) {

    val notifications by viewModel.notifications.collectAsState()
    val listState = rememberLazyListState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Tránh bị che bởi thanh trạng thái của máy
            .padding(16.dp)
    ) {

            Text(
                text = "Thông báo",
                style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center, // Canh chữ ra giữa khung của nó
                modifier = Modifier.fillMaxWidth()
            )


        Spacer(modifier = Modifier.height(14.dp))
        Text (
                text = "Tất cả: ${notifications?.size ?:0}",
        style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(5.dp))
        val list = notifications

        when { list == null  -> {
            Box( modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)
            {
                CircularProgressIndicator()
            }
        }
            list.isEmpty() -> {
               Box (Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                   Text(
                       text = "Danh sách trống",
                       textAlign = TextAlign.Center,
                       style = MaterialTheme.typography.bodyLarge,
                       color = MaterialTheme.colorScheme.onSurfaceVariant
                   )
               }
            }
            else -> {
            //  có dữ liệu -> ra danh sách cuộn
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items (
                    items = list,
                    key = {it.id}
                ) {
                    item -> NotificationCardItem(item = item)
                }
            }
            }
        }
    }
}


@Composable
fun NotificationCardItem(item: NotificationEntity) {
    val context = LocalContext.current
    val reason = item.filterReason ?: ""
    val isSpam = item.isFiltered

    // Kiểm tra xem có thuộc nhóm IGNORED (Hệ thống, tiến trình, ngầm...) dựa vào reason hay không
    val isIgnored = reason == "Chạy ngầm" || reason == "Hệ thống" || reason == "Trống" || reason == "Thông báo tổng"

    // Phân định 3 màu nền giao diện cho 3 nhóm test
    val backgroundColor = when {
        isIgnored -> Color(0xFFFFF9C4) // Màu 1: Vàng nhạt (Hệ thống / Tiến trình / Ignored)
        isSpam -> Color(0xFFFFEBEE)    // Màu 2: Đỏ nhạt (Spam / Rác)
        else -> Color(0xFFF1F8E9)      // Màu 3: Xanh nhạt (Hợp lệ / Passed)
    }

    val badgeColor = when {
        isIgnored -> Color(0xFFFBC02D) // Vàng đậm
        isSpam -> Color(0xFFEF5350)    // Đỏ đậm
        else -> Color(0xFF66BB6A)      // Xanh đậm
    }

    // Cache lại icon theo packageName của từng item
    val iconBitmap = remember(item.packageName) {
        getAppIcon(item.packageName, context.packageManager)
    }

    val formattedTime = remember(item.postTime) {
        formatNotificationTime(item.postTime)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        // 2 hàng
        Column(
            modifier = Modifier.fillMaxWidth() .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            //hàng trên chia 3 cột
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.appName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                if (item.appName.isNotBlank()){
                    Text(
                        text = item.appName,
                        style = MaterialTheme.typography.titleSmall,
                   //     color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        modifier = Modifier
                            .background(
                                color = Color.Gray.copy(alpha = 0.1f), // Nền nhạt mờ
                                shape = RoundedCornerShape(6.dp) // Bo góc
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)

                    )
                }

                if (item.title.isNotBlank()) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(15f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Thời gian
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // hàng nội dung
            if (item.text.isNotBlank()) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium
                    //color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Dòng nhỏ hiển thị lý do lọc (giúp dễ test phân loại)
            if (!item.filterReason.isNullOrBlank()) {
                Text(
                    text = "Phân loại: ${item.filterReason}",
                    style = MaterialTheme.typography.labelSmall,
                    color = badgeColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

//lấy icon từ packagenName
fun getAppIcon(packageName: String, packageManager: PackageManager): ImageBitmap? {
    return try {
        val drawable = packageManager.getApplicationIcon(packageName)
        drawableToImageBitmap(drawable)
    } catch (e: Exception) {
        null
    }
}

private fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
    if (drawable is BitmapDrawable) {
        return drawable.bitmap.asImageBitmap()
    }
    val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
    val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap.asImageBitmap()
} //xử lý icon

fun formatNotificationTime(postTime: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = postTime }
    return String.format(
        Locale.getDefault(), "%02d:%02d",
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE)
    )
} // chuyen thoi gian