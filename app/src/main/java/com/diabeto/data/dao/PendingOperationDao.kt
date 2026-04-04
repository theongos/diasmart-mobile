package com.diabeto.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.diabeto.data.entity.PendingOperationEntity

@Dao
interface PendingOperationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperationEntity): Long

    @Query("SELECT * FROM pending_operations WHERE status = 'PENDING' ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPending(limit: Int = 50): List<PendingOperationEntity>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE status = 'PENDING'")
    suspend fun pendingCount(): Int

    @Query("UPDATE pending_operations SET status = :status, lastAttemptAt = :now, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_operations WHERE status = 'FAILED' AND retryCount >= maxRetries")
    suspend fun purgeExhausted()

    @Query("DELETE FROM pending_operations")
    suspend fun deleteAll()
}
