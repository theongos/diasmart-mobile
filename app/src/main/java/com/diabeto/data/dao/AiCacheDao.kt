package com.diabeto.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.diabeto.data.entity.AiCacheEntity

@Dao
interface AiCacheDao {

    @Query("SELECT * FROM ai_cache WHERE queryHash = :hash AND expiresAt > :now LIMIT 1")
    suspend fun getCached(hash: String, now: Long = System.currentTimeMillis()): AiCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: AiCacheEntity)

    @Query("UPDATE ai_cache SET hitCount = hitCount + 1 WHERE queryHash = :hash")
    suspend fun incrementHitCount(hash: String)

    @Query("DELETE FROM ai_cache WHERE expiresAt < :now")
    suspend fun purgeExpired(now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM ai_cache")
    suspend fun count(): Int
}
