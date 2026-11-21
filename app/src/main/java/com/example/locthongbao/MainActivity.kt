package com.example.locthongbao

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.locthongbao.data.service.NotiListenerService
import com.example.locthongbao.ui.components.MainAppContainer
import com.example.locthongbao.ui.permission.PermissionScreen
import com.example.locthongbao.ui.screens.notifications.MainDashboardScreen
import com.example.locthongbao.ui.theme.LocThongBaoTheme
import com.example.locthongbao.util.PermissionUtil

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocThongBaoTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current

                val isServiceRunning by NotiListenerService.isServiceRunning.collectAsState()
                val isGranted = PermissionUtil.isNotificationListenerEnable(this)

                var hasPermission by remember {
                    mutableStateOf(PermissionUtil.isNotificationListenerEnable(context))
                }

                // Kiểm tra lại quyền khi quay lại app từ màn hình Cài đặt hệ thống
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

                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Thiếu quyền hoặc service chưa chạy thì giữ ở màn xin quyền
                    if (!hasPermission || !isGranted || !isServiceRunning) {
                        PermissionScreen(
                            isServiceRunning = isServiceRunning,
                            onPermissionGranted = {
                                NotiListenerService.reviveService(this)
                            }
                        )
                    } else {
                        MainAppContainer()
                    }
                }
            }
        }
    }
}