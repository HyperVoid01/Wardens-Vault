package com.soo.wardensvault

import android.content.Context
import androidx.room.withTransaction
import java.time.LocalDate
import kotlin.math.sqrt

/*
GamificationManager
All XP / Gold / level / daily-login-reward rules live here so the rest of the
app only has to call it (e.g. after an expense is saved).

Level curve: level n needs 50 * (n-1)^2 total XP
  (L2 = 50, L3 = 200, L4 = 450, L5 = 800 ...)
Gold is a cosmetic currency only - it never touches the budget calculations.
 */
class GamificationManager(context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val statsDao = db.playerStatsDao()

    //what happened when XP/Gold was awarded
    data class AwardResult(
        val xpGained: Int,
        val goldGained: Int,
        val oldLevel: Int,
        val newLevel: Int,
        val stats: PlayerStats
    ) {
        val leveledUp: Boolean get() = newLevel > oldLevel
    }

    //what happened when the user tried to claim today's login reward
    data class DailyRewardResult(
        val streak: Int,
        val goldGained: Int,
        val xpGained: Int,
        val isBonusDay: Boolean,
        val oldLevel: Int,
        val newLevel: Int,
        val stats: PlayerStats
    ) {
        val leveledUp: Boolean get() = newLevel > oldLevel
    }

    suspend fun getStats(userId: Int): PlayerStats {
        return statsDao.getStats(userId) ?: PlayerStats(userId = userId)
    }

    //generic reward - used by every feature that gives XP/Gold (expenses now, bills/goals later)
    suspend fun award(userId: Int, xp: Int, gold: Int, expensesLogged: Int = 0): AwardResult {
        return db.withTransaction {
            val current = statsDao.getStats(userId) ?: PlayerStats(userId = userId)
            val updated = current.copy(
                xp = current.xp + xp,
                gold = current.gold + gold,
                expensesLogged = current.expensesLogged + expensesLogged
            )
            statsDao.upsert(updated)
            AwardResult(xp, gold, levelForXp(current.xp), levelForXp(updated.xp), updated)
        }
    }

    //spends Gold on a cosmetic; returns false (and changes nothing) if the user can't afford it
    suspend fun spendGold(userId: Int, amount: Int): Boolean {
        return db.withTransaction {
            val current = statsDao.getStats(userId) ?: PlayerStats(userId = userId)
            if (current.gold < amount) return@withTransaction false
            statsDao.upsert(current.copy(gold = current.gold - amount))
            true
        }
    }

    //called after an expense is saved
    suspend fun onExpenseLogged(userId: Int): AwardResult {
        return award(userId, XP_PER_EXPENSE, GOLD_PER_EXPENSE, expensesLogged = 1)
    }

    /*
    "Staying within budget" reward: once per month, when last month's spending finished
    at or under the monthly budget. Returns null if nothing was awarded.
     */
    suspend fun checkMonthlyBudgetReward(context: Context, userId: Int): AwardResult? {
        val prevMonth = DateUtils.previousMonth(System.currentTimeMillis())
        val flagKey = "budget_reward_${userId}_${DateUtils.monthKey(prevMonth)}"
        if (AppSettings.getFlag(context, flagKey)) return null
        AppSettings.setFlag(context, flagKey)

        val goal = db.budgetGoalDao().getGoalForUser(userId) ?: return null
        if (goal.maxGoal <= 0) return null
        val spent = db.expenseDao().getTotalSpentForPeriod(
            userId, DateUtils.startOfMonth(prevMonth), DateUtils.endOfMonth(prevMonth)
        )
        if (spent <= 0.0 || spent > goal.maxGoal) return null
        return award(userId, BUDGET_MONTH_XP, BUDGET_MONTH_GOLD)
    }

    /*
    Claims today's login reward. Returns null if it was already claimed today,
    so the same day can never be claimed twice.
    Streak: claiming the day after the last claim extends it, any gap resets it to 1.
    Gold: 25, 30, 35 ... up to 55 from day 6; every 7th day adds a bonus.
     */
    suspend fun claimDailyReward(userId: Int): DailyRewardResult? {
        return db.withTransaction {
            val today = LocalDate.now().toEpochDay()
            val current = statsDao.getStats(userId) ?: PlayerStats(userId = userId)
            if (current.lastClaimDay == today) return@withTransaction null

            val streak = if (current.lastClaimDay == today - 1) current.loginStreak + 1 else 1
            val bonusDay = streak % 7 == 0

            var gold = DAILY_BASE_GOLD + DAILY_GOLD_STEP * minOf(streak, 6)
            var xp = DAILY_XP
            if (bonusDay) {
                gold += BONUS_GOLD
                xp += BONUS_XP
            }

            val updated = current.copy(
                xp = current.xp + xp,
                gold = current.gold + gold,
                loginStreak = streak,
                longestStreak = maxOf(current.longestStreak, streak),
                lastClaimDay = today
            )
            statsDao.upsert(updated)
            DailyRewardResult(
                streak, gold, xp, bonusDay,
                levelForXp(current.xp), levelForXp(updated.xp), updated
            )
        }
    }

    companion object {
        const val BILL_XP = 25
        const val BILL_GOLD = 15
        const val BILL_EARLY_BONUS_GOLD = 10
        const val GOAL_XP = 100
        const val GOAL_GOLD = 50
        const val BUDGET_MONTH_XP = 50
        const val BUDGET_MONTH_GOLD = 30
        const val XP_PER_EXPENSE = 10
        const val GOLD_PER_EXPENSE = 5
        const val DAILY_BASE_GOLD = 20
        const val DAILY_GOLD_STEP = 5
        const val DAILY_XP = 10
        const val BONUS_GOLD = 100
        const val BONUS_XP = 50

        fun levelForXp(xp: Int): Int = sqrt(xp / 50.0).toInt() + 1

        //total XP needed to reach the given level
        fun xpForLevel(level: Int): Int = 50 * (level - 1) * (level - 1)

        //XP earned inside the current level and the size of that level, for progress bars
        fun levelProgress(xp: Int): Pair<Int, Int> {
            val level = levelForXp(xp)
            val start = xpForLevel(level)
            val next = xpForLevel(level + 1)
            return Pair(xp - start, next - start)
        }

        fun titleForLevel(level: Int): String = when {
            level >= 20 -> "Vault Overlord"
            level >= 15 -> "Grand Warden"
            level >= 10 -> "Dungeon Master"
            level >= 7 -> "Keeper of the Vault"
            level >= 5 -> "Gold Guardian"
            level >= 3 -> "Dungeon Guard"
            else -> "Squire"
        }
    }
}
