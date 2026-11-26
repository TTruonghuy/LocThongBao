package com.example.locthongbao.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.locthongbao.ui.components.buttons.SwipeActionButton
import com.example.locthongbao.util.formatNotificationDateTime
import com.example.locthongbao.util.getAppIcon
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val SwipeDeleteRed = Color(0xFFC62828)

@Composable
fun NotificationCardItem(
    item: NotificationEntity,
    modifier: Modifier = Modifier,
    showAppHeader: Boolean = true,
    onDelete: (() -> Unit)? = null,
    isRevealed: Boolean = false,
    onRevealChange: ((revealed: Boolean) -> Unit)? = null
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val reason = item.filterReason ?: ""
    val isSpam = item.isFiltered

    val formattedTime = remember(item.postTime) {
        formatNotificationDateTime(item.postTime)
    }

    val isIgnored = reason == "Chạy ngầm" ||
            reason == "Hệ thống" ||
            reason == "Trống" ||
            reason == "Thông báo tổng"

    val backgroundColor = when {
        isIgnored -> Color(0xFFF8FCF9)
        isSpam -> Color(0xFFFFFAFA)
        else -> Color(0xFFF8FCF9)
    }

    val badgeColor = when {
        isIgnored -> Color(0xFFFBC02D)
        isSpam -> Color(0xFFEF5350)
        else -> Color(0xFF66BB6A)
    }

    val iconBitmap = remember(item.packageName) {
        getAppIcon(item.packageName, context.packageManager)
    }

    // Chỉ bật kéo sang trái khi có hành động xoá
    val canSwipe = onDelete != null
    val revealWidthDp = 76.dp
    val revealWidthPx = with(density) { revealWidthDp.toPx() }
    val touchSlopPx = with(density) { 14.dp.toPx() }

    // State nội bộ nếu nơi gọi không quản lý revealed tập trung
    var internalRevealed by remember(item.id) { mutableStateOf(false) }
    val effectiveRevealed = if (onRevealChange != null) isRevealed else internalRevealed

    val setRevealed: (Boolean) -> Unit = { next ->
        if (onRevealChange != null) {
            onRevealChange(next)
        } else {
            internalRevealed = next
        }
    }


    val offsetX = remember(item.id) { Animatable(0f) }

    LaunchedEffect(effectiveRevealed, canSwipe) {
        val target = if (canSwipe && effectiveRevealed) -revealWidthPx else 0f
        if (offsetX.value != target) {
            offsetX.animateTo(target, animationSpec = tween(durationMillis = 160))
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    ) {
        // Lớp nền nút "Xoá" hiện ra ở bên phải khi kéo thẻ sang trái
        if (canSwipe) {
            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(end = 6.dp)
            ) {
                SwipeActionButton(
                    text = "Xoá",
                    textColor = SwipeDeleteRed,
                    enabled = effectiveRevealed,
                    height = 40.dp,
                    containerColor = Color.White,
                    onClick = {
                        setRevealed(false)
                        onDelete()
                    },
                    modifier = Modifier.width(revealWidthDp - 10.dp)
                )
            }
        }

        // Lớp thẻ chính trượt ngang sang trái
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(x = offsetX.value.roundToInt(), y = 0) }
                .then(
                    if (canSwipe) {
                        Modifier.pointerInput(item.id, effectiveRevealed) {
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
                                            setRevealed(shouldOpen)
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
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (effectiveRevealed) {
                            Modifier.clickable { setRevealed(false) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showAppHeader) {
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

                        if (item.appName.isNotBlank()) {
                            Text(
                                text = item.appName,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                modifier = Modifier
                                    .background(
                                        color = Color.Gray.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (item.title.isNotBlank()) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        softWrap = false
                    )
                }

                if (item.text.isNotBlank()) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

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
}