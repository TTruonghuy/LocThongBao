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
import com.example.locthongbao.ui.components.buttons.MainAppContainer
import com.example.locthongbao.ui.permission.PermissionScreen
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

                var hasNotificationAccess by remember {
                    mutableStateOf(PermissionUtil.isNotificationListenerEnable(context))
                }
                var hasBatteryIgnore by remember {
                    mutableStateOf(PermissionUtil.isIgnoringBatteryOptimizations(context))
                }
                var enteredApp by remember { mutableStateOf(false) }

                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            val granted = PermissionUtil.isNotificationListenerEnable(context)
                            val batteryOk = PermissionUtil.isIgnoringBatteryOptimizations(context)
                            hasNotificationAccess = granted
                            hasBatteryIgnore = batteryOk

                            // Nếu mất quyền thì quay lại màn xin quyền
                            if (!granted || !batteryOk) {
                                enteredApp = false
                            }
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
                    val canEnterApp = hasNotificationAccess && hasBatteryIgnore

                    if (!canEnterApp) {
                        PermissionScreen(
                            isServiceRunning = isServiceRunning,
                            onPermissionGranted = {
                                runCatching {
                                    NotiListenerService.reviveService(this@MainActivity)
                                }
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