package com.example.locthongbao.ui.screens.blocknotifications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.locthongbao.ui.MainViewModel
import com.example.locthongbao.ui.components.NotificationCardItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedNotificationsScreen(
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val blockedNotifications by viewModel.blockedNotifications.collectAsState()
    val list = blockedNotifications
    val hasItems = !list.isNullOrEmpty()
    val listState = rememberLazyListState()

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Xoá tất cả thông báo") },
            text = { Text("Bạn có chắc muốn xoá toàn bộ thông báo trong danh sách này không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        viewModel.clearBlockedNotifications()
                    }
                ) {
                    Text("Xoá")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Huỷ")
                }
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        CenterAlignedTopAppBar(
            title = { Text("Đã chặn") }
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Đã chặn: ${list?.size ?: 0}",
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                when {
                    list == null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    list.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Chưa có thông báo bị chặn",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(
                                items = list,
                                key = { item -> item.id }
                            ) { item ->
                                NotificationCardItem(item = item)
                            }
                        }
                    }
                }
            }

            if (hasItems) {
                ExtendedFloatingActionButton(
                    onClick = { showConfirmDialog = true },
                    icon = {

                    },
                    text = { Text("Xoá") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp)
                )
            }
        }
    }
}