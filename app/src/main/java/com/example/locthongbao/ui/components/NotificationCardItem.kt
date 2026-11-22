package com.example.locthongbao.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.locthongbao.data.local.NotificationEntity
import com.example.locthongbao.util.formatNotificationDateTime
import com.example.locthongbao.util.formatNotificationTime
import com.example.locthongbao.util.getAppIcon

@Composable
fun NotificationCardItem(item: NotificationEntity) {
    val context = LocalContext.current
    val reason = item.filterReason ?: ""
    val isSpam = item.isFiltered

    val formattedTime = remember(item.postTime) {
        formatNotificationDateTime(item.postTime)
    }
    // Nhóm thông báo bị bỏ qua không qua lọc spam
    val isIgnored = reason == "Chạy ngầm" || reason == "Hệ thống" || reason == "Trống" || reason == "Thông báo tổng"

    // Màu nền thẻ: vàng = ignored, đỏ = spam, xanh = hợp lệ
    val backgroundColor = when {
        isIgnored -> Color(0xFFFFF9C4)
        isSpam -> Color(0xFFFFEBEE)
        else -> Color(0xFFF1F8E9)
    }

    // Màu chữ lý do lọc
    val badgeColor = when {
        isIgnored -> Color(0xFFFBC02D)
        isSpam -> Color(0xFFEF5350)
        else -> Color(0xFF66BB6A)
    }

    // Cache icon theo packageName để scroll LazyColumn đỡ gọi PackageManager lại
    val iconBitmap = remember(item.packageName) {
        getAppIcon(item.packageName, context.packageManager)
    }


    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Hàng trên: icon + tên app + title + giờ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (iconBitmap != null) {
                    Image(
                        bitmap = iconBitmap,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    // Fallback chữ cái đầu nếu không lấy được icon app
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

                if (item.title.isNotBlank()) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(15f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    softWrap = false
                )
            }

            // Nội dung thông báo
            if (item.text.isNotBlank()) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Lý do lọc
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