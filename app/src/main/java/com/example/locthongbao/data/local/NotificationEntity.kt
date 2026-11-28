package com.example.locthongbao.data.local
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["postTime"]),
        Index(value = ["packageName", "postTime"]),
        Index(value = ["notificationKey", "postTime"], unique = true)
    ]
)


data class NotificationEntity (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val subText: String?,
    val postTime: Long,
    val category: String?,

    val channelId: String?,
    val channelName: String?,
    val flags: Int,
    val isOngoing: Boolean,
    val isClearable: Boolean,
    val isGroupSummary: Boolean,
    val groupKey: String?,
    val userId: Int,
    val color: Int,
    val extrasJson: String?,

    val isFiltered: Boolean,
    val filterReason: String,
    val isManualOverride: Boolean = false
)