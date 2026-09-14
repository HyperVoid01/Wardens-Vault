package com.soo.wardensvault

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BudgetGoalDao {
    //REPLACE lets "set a new goal" simply upsert since id is fixed once known
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setGoal(goal: BudgetGoal): Long

    @Query("SELECT * FROM budget_goals WHERE userId = :userId LIMIT 1")
    suspend fun getGoalForUser(userId: Int): BudgetGoal?
}
