package com.example.locthongbao.ui.components

import com.example.locthongbao.data.local.NotificationEntity

data class AppNotificationGroup(
    val packageName: String,
    val appName: String,
    val notifications: List<NotificationEntity>,
    val latestPostTime: Long
) {
    val count: Int get() = notifications.size
}

fun List<NotificationEntity>.groupByApp(): List<AppNotificationGroup> {
    if(this.isNullOrEmpty()) return emptyList()
    return this
        .sortedByDescending { it.postTime }
        .groupBy { it.packageName }
        .map { (pkg, items) ->
            val sortedItems = items.sortedByDescending { it.postTime }
            val resolvedAppName = sortedItems
                .firstOrNull { it.appName.isNotBlank() }
                ?.appName
                ?: pkg

            AppNotificationGroup(
                packageName = pkg,
                appName = resolvedAppName,
                notifications = sortedItems,
                latestPostTime = sortedItems.firstOrNull()?.postTime ?: 0L
            )
        }
        .sortedByDescending { it.latestPostTime }
}