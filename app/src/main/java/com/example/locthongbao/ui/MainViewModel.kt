package com.example.locthongbao.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.locthongbao.data.local.AppDatabase
import com.example.locthongbao.data.local.NotificationEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.locthongbao.data.local.NotificationDao

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).notificationDao()

    val notifications: StateFlow<List<NotificationEntity>?> = dao.getAllNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Danh sách thông báo cho qua (chưa bị chặn)
    val allowedNotifications: StateFlow<List<NotificationEntity>?> = notifications
        .map { list -> list?.filter { !it.isFiltered } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Danh sách thông báo bị chặn
    val blockedNotifications: StateFlow<List<NotificationEntity>?> = notifications
        .map { list -> list?.filter { it.isFiltered } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            dao.cleanSystemGarbage()
        }
    }


    fun clearAllowedNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearAllowedNotifications()
        }
    }

    fun clearBlockedNotifications() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearBlockedNotifications()
        }
    }

    fun deleteAllowedGroup(packageName: String) {
        viewModelScope.launch {
            dao.deleteAllowedByPackage(packageName)
        }
    }

    fun deleteBlockedGroup(packageName: String) {
        viewModelScope.launch {
            dao.deleteBlockedByPackage(packageName)
        }
    }

    fun deleteNotificationById(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }


    fun onToggleSpamItem(item: NotificationEntity) {
        viewModelScope.launch {
            val nextFiltered = !item.isFiltered
            val nextReason = if (nextFiltered) {
                "Người dùng đánh dấu: Rác"
            } else {
                "Người dùng đánh dấu: Bình thường"
            }
            // Gọi qua repository hoặc dao tùy cấu trúc project của bạn:
            dao.updateManualClassification(
                id = item.id,
                isFiltered = nextFiltered,
                reason = nextReason
            )
        }
    }


}