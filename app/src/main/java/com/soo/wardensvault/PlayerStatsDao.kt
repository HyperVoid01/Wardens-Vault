package com.soo.wardensvault

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/*
Data access for the player_stats table (XP, Gold, login streak, counters)
 */
@Dao
interface PlayerStatsDao {
    @Query("SELECT * FROM player_stats WHERE userId = :userId")
    suspend fun getStats(userId: Int): PlayerStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: PlayerStats)
}
