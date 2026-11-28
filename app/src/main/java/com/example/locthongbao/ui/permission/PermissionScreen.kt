package com.example.locthongbao.ui.permission

import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.locthongbao.util.PermissionUtil
import kotlinx.coroutines.delay

@Composable
fun PermissionScreen(
    isServiceRunning: Boolean = true,
    listenerComponentName: ComponentName? = null,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isGranted by remember {
        mutableStateOf(PermissionUtil.isNotificationListenerEnable(context))
    }
    var isBatteryGranted by remember {
        mutableStateOf(PermissionUtil.isIgnoringBatteryOptimizations(context))
    }
    // Tránh báo "bị hệ thống tắt" oan ngay khoảnh khắc vừa mở/cài lại app
    var hasCheckedAfterResume by remember { mutableStateOf(false) }
    var resumeTrigger by remember { mutableIntStateOf(0) }
    var showReasonDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = PermissionUtil.isNotificationListenerEnable(context)
                isBatteryGranted = PermissionUtil.isIgnoringBatteryOptimizations(context)
                resumeTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(resumeTrigger, isGranted, isServiceRunning) {
        if (resumeTrigger == 0) return@LaunchedEffect
        hasCheckedAfterResume = false

        if (isGranted && !isServiceRunning && listenerComponentName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching {
                NotificationListenerService.requestRebind(listenerComponentName)
            }
        }

        // Chờ ngắn để hệ thống bind lại service sau khi quay về từ Cài đặt hoặc sau khi cài đặt app
        delay(500L)
        isGranted = PermissionUtil.isNotificationListenerEnable(context)
        isBatteryGranted = PermissionUtil.isIgnoringBatteryOptimizations(context)
        hasCheckedAfterResume = true
    }

    val isServiceMuted = isGranted && !isServiceRunning && hasCheckedAfterResume
    val isAllReady = isGranted && isBatteryGranted && !isServiceMuted

    val headerTitle = when {
        isServiceMuted -> "Dịch vụ đang tạm ngưng"
        !isGranted -> "Bước 1/2: Cấp quyền đọc thông báo"
        !isBatteryGranted -> "Bước 2/2: Tắt tối ưu pin cho ứng dụng"
        else -> "Đã sẵn sàng hoạt động"
    }

    val headerDescription = when {
        isServiceMuted -> "Vui lòng Tắt và Bật lại quyền truy cập thông báo bên dưới để khởi động lại dịch vụ."
        !isGranted -> "Ứng dụng cần quyền Truy cập thông báo để nhận và lọc tin nhắn/thông báo tự động."
        !isBatteryGranted -> "Cho phép chạy không hạn chế pin giúp bộ lọc hoạt động ổn định khi tắt màn hình."
        else -> "Tất cả quyền cần thiết đã được cấp. Nhấn nút bên dưới để vào ứng dụng."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isServiceMuted) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️ $headerTitle  ",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )

                TextButton(
                    onClick = { showReasonDialog = true },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "Xem lý do",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = headerDescription,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                text = headerTitle,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = headerDescription,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (showReasonDialog) {
            AlertDialog(
                onDismissRequest = { showReasonDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Vì sao dịch vụ bị tạm ngưng?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                text = {
                    Text(
                        text = "Để tiết kiệm pin và giải phóng bộ nhớ, hệ điều hành trên thiết bị của bạn (đặc biệt là Xiaomi, Oppo, Vivo...) có cơ chế tự động đóng các tiến trình chạy ngầm sau một thời gian không mở ứng dụng.\n\n" +
                                "Do quy định bảo mật của Android, ứng dụng không được phép tự động kích hoạt lại dịch vụ này mà cần bạn thao tác Tắt và Bật lại công tắc quyền thủ công.\n\n" +
                                "💡 Mẹo hạn chế lặp lại: Hãy bật \"Tự khởi động\" (Autostart) và chọn \"Không hạn chế\" trong phần cài đặt Pin của ứng dụng.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showReasonDialog = false }) {
                        Text("Đã hiểu")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isGranted || isServiceMuted) {
            Button(
                onClick = { PermissionUtil.openNotificationAccessSettings(context) },
                modifier = Modifier.fillMaxWidth(),
                colors = if (isServiceMuted) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    text = if (isServiceMuted) {
                        "Mở Cài đặt để Tắt & Bật lại quyền"
                    } else {
                        "1. Cấp quyền Truy cập thông báo"
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isGranted && !isBatteryGranted && !isServiceMuted) {
            Button(
                onClick = { PermissionUtil.requestIgnoreBatteryOptimizations(context) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "2. Bật Không hạn chế pin")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isAllReady) {
            Button(
                onClick = onPermissionGranted,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Hoàn tất & Bắt đầu sử dụng")
            }
        }
    }
}