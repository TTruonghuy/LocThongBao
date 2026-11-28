package com.example.locthongbao.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.locthongbao.data.local.NotificationEntity
import com.example.locthongbao.util.formatNotificationDateTime
import com.example.locthongbao.util.getAppIcon
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun AppNotificationGroupCard(
    group: AppNotificationGroup,
    onViewAll: (AppNotificationGroup) -> Unit = {},
    onCollapseRevealIfNeeded: () -> Unit = {},
    previewLimit: Int = 2,
    expandedLimit: Int = 5,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
) {
    val context = LocalContext.current
    var isGroupExpanded by remember(group.packageName) { mutableStateOf(false) }

    val iconBitmap = remember(group.packageName) {
        getAppIcon(group.packageName, context.packageManager)
    }

    val visibleItems = remember(group.notifications, isGroupExpanded, previewLimit, expandedLimit) {
        val targetLimit = if (isGroupExpanded) expandedLimit else previewLimit
        group.notifications.take(targetLimit)
    }

    val remainingCount = (group.count - visibleItems.size).coerceAtLeast(0)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.10f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Header nhóm: [Icon app] Tên app - Số thông báo - [v]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onCollapseRevealIfNeeded()
                        isGroupExpanded = !isGroupExpanded
                    }
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = group.appName,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(20.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = group.appName.take(1).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = group.appName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .background(
                            color = Color.Gray.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Text(
                    text = "${group.count} thông báo",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (isGroupExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (isGroupExpanded) "Thu gọn nhóm" else "Mở rộng nhóm",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // 2. Các dòng thông báo trong nhóm
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                visibleItems.forEach { item ->
                    GroupedNotificationRow(
                        item = item,
                        onRowClick = onCollapseRevealIfNeeded
                    )
                }
            }

            // 3. Nút "Xem tất cả" nằm cuối bên phải
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onViewAll(group) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (remainingCount > 0) {
                            "Xem tất cả (+$remainingCount)"
                        } else {
                            "Xem tất cả"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeableGroupCard(
    group: AppNotificationGroup,
    isRevealed: Boolean,
    onRevealChange: (packageName: String?, revealed: Boolean) -> Unit,
    onViewAll: (AppNotificationGroup) -> Unit,
    onDeleteGroup: (AppNotificationGroup) -> Unit,
    onToggleSpamGroup: (AppNotificationGroup) -> Unit = {},
    containerColor: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier,
    isBlockedScreen: Boolean = false
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val revealWidthDp = 104.dp
    val revealWidthPx = with(density) { revealWidthDp.toPx() }
    val touchSlopPx = with(density) { 14.dp.toPx() }

    val offsetX = remember(group.packageName) { Animatable(0f) }

    LaunchedEffect(isRevealed) {
        val target = if (isRevealed) -revealWidthPx else 0f
        if (offsetX.value != target) {
            offsetX.animateTo(target, animationSpec = tween(durationMillis = 160))
        }
    }

    val collapseReveal: () -> Unit = {
        if (isRevealed) {
            onRevealChange(group.packageName, false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Lớp nền phía sau kéo full chiều cao nhóm để đồng bộ chiều cao thẻ;
        // 2 nút bên trong được căn chính giữa theo chiều cao của chính thẻ nhóm.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(end = 6.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(revealWidthDp - 10.dp)
            ) {
                // Nút "Ghim" (tông dịu, chưa làm chức năng ghim)
//                Box(
//                    contentAlignment = Alignment.Center,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(36.dp)
//                        .clip(RoundedCornerShape(8.dp))
//                        .background(Color.White)
//                        .clickable(enabled = isRevealed) {
//                            onRevealChange(group.packageName, false)
//                            onToggleSpamGroup(group)
//                        }
//                ) {
////                    Text(
////                        text = if (isBlockedScreen) "Không phải rác" else "Đây là rác",
////                        color = Color.Black,
////                        style = MaterialTheme.typography.labelMedium,
////                        fontWeight = FontWeight.SemiBold
////                    )
//                }

                // Nút "Xoá" (pastel đỏ nhạt dịu mắt, cao hơn nút Ghim, giữ nguyên chức năng xoá)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .clickable(enabled = isRevealed) {
                            onRevealChange(group.packageName, false)
                            onDeleteGroup(group)
                        }
                ) {
                    Text(
                        text = "Xoá",
                        color = Color(0xFFC62828),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Lớp thẻ chính trượt ngang khi người dùng kéo ngang rõ rệt
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = offsetX.value.roundToInt(), y = 0) }
                .pointerInput(group.packageName, isRevealed) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalDx = 0f
                        var totalDy = 0f
                        var isHorizontalSwipe = false

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (!change.pressed) {
                                if (isHorizontalSwipe) {
                                    val shouldOpen = offsetX.value <= -revealWidthPx * 0.45f
                                    onRevealChange(group.packageName, shouldOpen)
                                    scope.launch {
                                        offsetX.animateTo(
                                            targetValue = if (shouldOpen) -revealWidthPx else 0f,
                                            animationSpec = tween(durationMillis = 160)
                                        )
                                    }
                                }
                                break
                            }

                            val delta = change.position - change.previousPosition
                            totalDx += delta.x
                            totalDy += delta.y

                            if (!isHorizontalSwipe) {
                                if (abs(totalDy) > touchSlopPx && abs(totalDy) >= abs(totalDx)) {
                                    break
                                }
                                if (abs(totalDx) > touchSlopPx && abs(totalDx) > abs(totalDy) * 1.35f) {
                                    isHorizontalSwipe = true
                                }
                            }

                            if (isHorizontalSwipe) {
                                change.consume()
                                val nextValue = (offsetX.value + delta.x).coerceIn(-revealWidthPx, 0f)
                                scope.launch {
                                    offsetX.snapTo(nextValue)
                                }
                            }
                        }
                    }
                }
        ) {
            AppNotificationGroupCard(
                group = group,
                containerColor = containerColor,
                onCollapseRevealIfNeeded = collapseReveal,
                onViewAll = { selectedGroup ->
                    if (isRevealed) {
                        onRevealChange(group.packageName, false)
                    } else {
                        onViewAll(selectedGroup)
                    }
                }
            )
        }
    }
}

@Composable
private fun GroupedNotificationRow(
    item: NotificationEntity,
    onRowClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isExpanded by remember(item.id) { mutableStateOf(false) }

    val displayTitle = when {
        item.title.isNotBlank() -> item.title
        item.text.isNotBlank() -> item.text.lineSequence().firstOrNull().orEmpty()
        else -> "(Không có tiêu đề)"
    }

    val displayBody = when {
        item.text.isNotBlank() -> item.text
        item.subText?.isNotBlank() == true -> item.subText
        else -> ""
    }

    val formattedTime = remember(item.postTime) {
        formatNotificationDateTime(item.postTime)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onRowClick()
                isExpanded = !isExpanded
            },
        shape = RoundedCornerShape(8.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = if (isExpanded) 3 else 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    softWrap = false
                )

                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Default.KeyboardArrowUp
                    } else {
                        Icons.Default.KeyboardArrowDown
                    },
                    contentDescription = if (isExpanded) "Thu gọn thông báo" else "Mở rộng thông báo",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (displayBody.isNotBlank()) {
                Text(
                    text = displayBody,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

//            AnimatedVisibility(visible = isExpanded && !item.filterReason.isNullOrBlank()) {
//                Text(
//                    text = "Phân loại: ${item.filterReason}",
//                    style = MaterialTheme.typography.labelSmall,
//                    color = if (item.isFiltered) Color(0xFFEF5350) else Color(0xFF2E7D32),
//                    fontWeight = FontWeight.Medium,
//                    modifier = Modifier.padding(top = 2.dp)
//                )
//            }
        }
    }
}