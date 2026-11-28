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


    @Query("UPDATE notifications SET filterReason = :reason WHERE notificationKey = :key AND postTime = :postTime")
    suspend fun setReason(key: String, postTime: Long, reason: String)

    @Query("DELETE FROM notifications WHERE isFiltered = 0")
    suspend fun clearAllowedNotifications()

    @Query("DELETE FROM notifications WHERE isFiltered = 1")
    suspend fun clearBlockedNotifications()

    @Query("DELETE FROM notifications WHERE packageName = :packageName AND isFiltered = 0")
    suspend fun deleteAllowedByPackage(packageName: String)

    @Query("DELETE FROM notifications WHERE packageName = :packageName AND isFiltered = 1")
    suspend fun deleteBlockedByPackage(packageName: String)

    // Dùng cho màn hình "Xem tất cả" sau này khi xoá thông báo đơn
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
    UPDATE notifications
    SET isFiltered = :isFiltered,
        filterReason = :reason,
        isManualOverride = 1
    WHERE id = :id
    """
    )
    suspend fun updateManualClassification(
        id: Long,
        isFiltered: Boolean,
        reason: String
    )

}
