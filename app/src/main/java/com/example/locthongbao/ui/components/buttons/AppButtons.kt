package com.example.locthongbao.ui.components.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val FilterBadgeGray = Color(0xFFE0E0E0)
private val DestructiveRed = Color(0xFFC62828)

/**
 * Nút bám sát mép phải, bo 2 góc trái — phù hợp thao tác một tay bên phải.
 */
@Composable
fun EdgeDockedActionButton(
    textColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        color = containerColor,
        shape = RoundedCornerShape(
            topStart = 10.dp,
            bottomStart = 10.dp,
            topEnd = 0.dp,
            bottomEnd = 0.dp
        ),
        shadowElevation = 2.dp,
        modifier = modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(start = 12.dp, end = 10.dp)
        ) {
            CompositionLocalProvider(LocalContentColor provides textColor) {
                content()
            }
        }
    }
}

/**
 * Nút "Xóa" bám mép phải dùng chung cho danh sách/nhóm/màn chi tiết.
 */
@Composable
fun EdgeDeleteDockedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Xóa"
) {
    EdgeDockedActionButton(
        textColor = MaterialTheme.colorScheme.error,
        onClick = onClick,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Nút "Lọc [activeLabel]" bám mép phải với badge xám cho phần tên bộ lọc.
 */
@Composable
fun EdgeFilterDockedButton(
    activeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EdgeDockedActionButton(
        textColor = Color.Black,
        onClick = onClick,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Lọc",
                color = Color.Black,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(
                        color = FilterBadgeGray,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = activeLabel,
                    color = Color.Black,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Nút hành động nằm ở vùng kéo ngang của thẻ nhóm ("Ghim", "Xoá").
 */
@Composable
fun SwipeActionButton(
    text: String,
    textColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 36.dp,
    containerColor: Color = Color.White
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Nút "Xem tất cả (+N)" nằm góc dưới bên phải của thẻ nhóm.
 */
@Composable
fun AppViewAllButton(
    remainingCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
        modifier = modifier.height(32.dp)
    ) {
        Text(
            text = if (remainingCount > 0) {
                "Xem tất cả (+$remainingCount)"
            } else {
                "Xem tất cả"
            },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
    }
}