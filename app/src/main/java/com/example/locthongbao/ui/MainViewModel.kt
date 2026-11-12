package com.example.locthongbao.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.locthongbao.data.local.AppDatabase
import com.example.locthongbao.data.local.NotificationEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Lấy thủ thư DAO từ tủ sắt AppDatabase
    private val dao = AppDatabase.getDatabase(application).notificationDao()

    // Chuyển chuông báo động (Flow) từ database thành StateFlow để giao diện Compose quan sát trực tiếp
    val notifications: StateFlow<List<NotificationEntity>?> = dao.getAllNotifications()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}