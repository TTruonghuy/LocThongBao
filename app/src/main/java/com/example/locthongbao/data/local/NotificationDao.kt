package com.example.locthongbao.data.local
import androidx.annotation.InterpolatorRes
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY postTime DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Query ("DELETE FROM notifications")
    suspend fun clearAll()

    @Query("SELECT * FROM notifications WHERE notificationKey = :key ORDER BY postTime DESC LIMIT 1")
    suspend fun latestByKey(key: String): NotificationEntity?

    @Query( """
        DELETE FROM notifications
        WHERE packageName IN (
        'com.android.updater', 
        'com.xiaomi.discover', 
        'com.miui.systemAdSolution', 
        'com.android.providers.downloads'
        )
        OR (
        title = '' AND text = ''
        )
    """)
    suspend fun cleanSystemGarbage()

    @Update
    suspend fun update(n: NotificationEntity)




    @Query("DELETE FROM notifications WHERE isFiltered = 0")
    suspend fun clearAllowedNotifications()

    @Query("DELETE FROM notifications WHERE isFiltered = 1")
    suspend fun clearBlockedNotifications()



}
