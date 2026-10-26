package com.soo.wardensvault

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/*
Gamification progress for one user (one row per user).
lastClaimDay is the epoch-day (LocalDate.toEpochDay) of the last daily reward claim,
or -1 if the user has never claimed one.
Level is not stored - it is derived from xp (see GamificationManager.levelForXp).
 */
@Entity(
    tableName = "player_stats",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PlayerStats(
    @PrimaryKey val userId: Int,
    val xp: Int = 0,
    val gold: Int = 0,
    val loginStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastClaimDay: Long = -1L,
    val expensesLogged: Int = 0
)
