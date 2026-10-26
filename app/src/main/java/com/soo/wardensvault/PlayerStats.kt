package com.soo.wardensvault

/*
Gamification progress for one user - users/{uid}/player_stats/stats.
lastClaimDay is the epoch-day (LocalDate.toEpochDay) of the last daily reward claim,
or -1 if the user has never claimed one.
Level is not stored - it is derived from xp (see GamificationManager.levelForXp).
 */
data class PlayerStats(
    val userId: String = "",
    val xp: Int = 0,
    val gold: Int = 0,
    val loginStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastClaimDay: Long = -1L,
    val expensesLogged: Int = 0
)
