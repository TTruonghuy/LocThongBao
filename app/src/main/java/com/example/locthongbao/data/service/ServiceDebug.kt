package com.example.locthongbao.data.service

object ServiceDebug {
    @Volatile var connected = false
    @Volatile var postedCount = 0
    @Volatile var lastError: String? = null
}