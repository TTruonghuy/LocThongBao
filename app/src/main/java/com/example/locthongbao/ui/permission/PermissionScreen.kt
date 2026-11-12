package com.example.locthongbao.ui.permission

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.locthongbao.util.PermissionUtil
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner



@Composable
fun PermissionScreen(
    isServiceRunning: Boolean = true, // Nhận trạng thái service từ ngoài truyền vào
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(PermissionUtil.isNotificationListenerEnable(context))
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    var isBatteryGranted by remember {
        mutableStateOf(PermissionUtil.isIgnoringBatteryOptimizations(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isGranted = PermissionUtil.isNotificationListenerEnable(context)
                isBatteryGranted = PermissionUtil.isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // ⭐ XÁC ĐỊNH THỰC TRẠNG:
    // Quyền đã bật (isGranted = true) NHƯNG Service bị chết ngầm (!isServiceRunning)
    val isServiceMuted = isGranted && !isServiceRunning
    var showReasonDialog by remember { mutableStateOf(false) }

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
                    text = "⚠️ Dịch vụ đã bị hệ thống tắt!  ",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )

               // Spacer(modifier = Modifier.width(6.dp)) // Khoảng cách nhỏ giữa chữ cảnh báo và nút bấm

                TextButton(
                    onClick = { showReasonDialog = true },
                    contentPadding = PaddingValues(0.dp) // Xóa padding mặc định của nút để nó khít vào dòng
                ) {
                    Text(
                        text = "Xem lý do",
                        style = MaterialTheme.typography.bodyMedium, // Chỉnh size chữ cho khớp với câu cảnh báo
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
            Text(
                text = "\n\nVui lòng Tắt và Bật lại quyền truy cập thông báo bên dưới để khởi động lại.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,

            )}
        } else {
            Text(
                text = "Cần quyền truy cập thông báo để hoạt động",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
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
                        text =
                                "Để tiết kiệm pin và giải phóng bộ nhớ, hệ điều hành trên thiết bị của bạn (đặc biệt là Xiaomi, Oppo, Vivo...) có cơ chế tự động đóng các tiến trình chạy ngầm sau một thời gian không mở ứng dụng.\n\n" +
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

        // Bước 1: Nút cấp quyền / Bật tắt lại quyền
        if (!isGranted || isServiceMuted) {
            Button(
                onClick = {
                    PermissionUtil.openNotificationAccessSettings(context)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = if (isServiceMuted) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(text = if (isServiceMuted) "Mở Cài đặt để Tắt & Bật lại quyền" else "Cấp quyền Truy cập thông báo")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Bước 2: Nút cấp quyền Không hạn chế pin
        if (isGranted && !isBatteryGranted && !isServiceMuted) {
            Button(
                onClick = {
                    PermissionUtil.requestIgnoreBatteryOptimizations(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "2. Bật Không hạn chế pin")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Khi đã đủ điều kiện -> Nút vào app
        if (isGranted && isBatteryGranted && !isServiceMuted) {
            Button(
                onClick = {
                    onPermissionGranted()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Hoàn tất & Bắt đầu sử dụng")
            }
        }
    }
}