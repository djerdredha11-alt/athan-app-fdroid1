package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PrayerDao {

    @Query("SELECT * FROM cached_prayer_times WHERE cacheKey = :key LIMIT 1")
    suspend fun getCached(key: String): PrayerCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PrayerCacheEntity)

    @Query("DELETE FROM cached_prayer_times WHERE cachedTimestamp < :expirationTime")
    suspend fun clearOldCache(expirationTime: Long)
}
