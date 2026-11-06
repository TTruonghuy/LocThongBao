package com.example.locthongbao.ui.permission

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.locthongbao.ui.util.PermissionUtil
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp


@Composable
fun PermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    var isGranted by remember {
        mutableStateOf(PermissionUtil.isNotificationListenerEnable(context))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text= "Cần quyền truy cập thông báo để hoạt động",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = {
                PermissionUtil.openNotificationAccessSettings(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text= "Cấp quyền truy cập thông báo")
        }
        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = {
                isGranted = PermissionUtil.isNotificationListenerEnable(context)
                if(isGranted) {
                    onPermissionGranted()
                }
            })
        {
            Text(
                text = if (isGranted) "Đã cấp quyền, bấm để tiếp tục"
                else "Kiểm tra lại trạng thái quyền"
            )
        }
    }
}