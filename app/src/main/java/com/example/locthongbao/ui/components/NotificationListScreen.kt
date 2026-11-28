package com.example.locthongbao.ui.components

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp


import com.example.locthongbao.data.local.NotificationEntity
import com.example.locthongbao.ui.components.buttons.EdgeDeleteDockedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    notifications: List<NotificationEntity>,
    emptyMessage: String,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    showAppHeaderOnItem: Boolean = true,
    onDeleteItem: ((NotificationEntity) -> Unit)? = null,
    onToggleSpamItem: ((NotificationEntity) -> Unit)? = null,
    isBlockedScreen: Boolean = false
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var revealedItemId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && revealedItemId != null) {
            revealedItemId = null
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Xoá tất cả") },
            text = { Text("Bạn có chắc muốn xoá toàn bộ thông báo trong mục này không?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onClearAll()
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
        modifier = modifier.fillMaxSize(),


    ) {
        if (title != null) {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại"
                            )
                        }
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emptyMessage)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 12.dp,
                        bottom = 88.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = notifications,
                        key = { it.id }
                    ) { item ->
                        NotificationCardItem(
                            item = item,
                            showAppHeader = showAppHeaderOnItem,
                            isRevealed = revealedItemId == item.id,
                            onRevealChange = { revealed ->
                                revealedItemId = if (revealed) item.id else null
                            },
                            onDelete = onDeleteItem?.let { deleteCallback ->
                                {
                                    revealedItemId = null
                                    deleteCallback(item)
                                }
                            },
                            onToggleSpam = onToggleSpamItem?.let { toggleCallback ->
                                {
                                    revealedItemId = null
                                    toggleCallback(item)
                                }
                            },
                            isBlockedScreen = isBlockedScreen
                        )
                    }
                }
            }

            if (notifications.isNotEmpty()) {
                EdgeDeleteDockedButton(
                    onClick = { showConfirmDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 12.dp)
                )
            }
        }
    }
}