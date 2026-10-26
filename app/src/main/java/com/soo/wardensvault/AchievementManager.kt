package com.soo.wardensvault

import android.content.Context

data class AchievementDef(
    val key: String,
    val name: String,
    val description: String,
    val xp: Int,
    val gold: Int
)

/*
Achievements are checked from the database after meaningful events
(expense logged, bill paid, goal completed, income added, item bought ...).
Anything newly earned is saved, rewarded and announced with a notification.
 */
object AchievementManager {
    val all: List<AchievementDef> = listOf(
        AchievementDef("first_expense", "First Coin Spent", "Log your first expense", 20, 10),
        AchievementDef("bookkeeper", "Bookkeeper", "Log 25 expenses", 75, 40),
        AchievementDef("first_blood", "First Blood", "Defeat your first bill enemy", 30, 20),
        AchievementDef("monster_slayer", "Monster Slayer", "Defeat 10 bills", 100, 60),
        AchievementDef("goal_getter", "Goal Getter", "Complete a savings goal", 100, 50),
        AchievementDef("week_warrior", "Week Warrior", "Reach a 7-day login streak", 75, 50),
        AchievementDef("treasurer", "Treasurer", "Record your first income", 20, 10),
        AchievementDef("planner", "Vault Planner", "Set a monthly budget", 20, 10),
        AchievementDef("fashionista", "Fashionista", "Buy your first cosmetic", 20, 0),
        AchievementDef("level_5", "Seasoned Warden", "Reach level 5", 100, 75)
    )

    fun byKey(key: String): AchievementDef? = all.firstOrNull { it.key == key }

    //checks every achievement; returns the ones unlocked just now
    suspend fun checkAll(context: Context, userId: Int): List<AchievementDef> {
        val db = AppDatabase.getDatabase(context)
        val gamification = GamificationManager(context)

        val unlocked = db.achievementDao().getAll(userId).map { it.achievementKey }.toSet()
        val stats = gamification.getStats(userId)

        val conditions = mapOf(
            "first_expense" to (stats.expensesLogged >= 1),
            "bookkeeper" to (stats.expensesLogged >= 25),
            "first_blood" to (db.billPaymentDao().count(userId) >= 1),
            "monster_slayer" to (db.billPaymentDao().count(userId) >= 10),
            "goal_getter" to (db.savingsGoalDao().countCompleted(userId) >= 1),
            "week_warrior" to (stats.longestStreak >= 7),
            "treasurer" to (db.incomeDao().count(userId) >= 1),
            "planner" to (db.budgetGoalDao().getGoalForUser(userId) != null),
            "fashionista" to (db.ownedItemDao().getAll(userId).isNotEmpty()),
            "level_5" to (GamificationManager.levelForXp(stats.xp) >= 5)
        )

        val newlyUnlocked = all.filter { it.key !in unlocked && conditions[it.key] == true }
        for (def in newlyUnlocked) {
            db.achievementDao().insert(
                UnlockedAchievement(
                    userId = userId,
                    achievementKey = def.key,
                    unlockedAt = System.currentTimeMillis()
                )
            )
            gamification.award(userId, def.xp, def.gold)
            NotificationHelper.notify(
                context, AppSettings.KEY_ACHIEVEMENTS, 5000 + def.key.hashCode() % 1000,
                "Achievement unlocked!", "${def.name}: ${def.description}"
            )
        }
        return newlyUnlocked
    }
}
