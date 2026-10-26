// Code Attribution
// Based on: ViewModel overview
// Author:   Android Developers
// URL:      https://developer.android.com/topic/libraries/architecture/viewmodel
// Accessed: 27 October 2026
// Used for: AndroidViewModel structure

package com.soo.wardensvault

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseDao = AppDatabase.getDatabase(application).expenseDao()
    private val budgetGoalDao = AppDatabase.getDatabase(application).budgetGoalDao()

    suspend fun createExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun getExpensesForPeriod(userId: String, from: Long, to: Long): List<Expense> {
        return expenseDao.getExpensesForPeriod(userId, from, to)
    }

    suspend fun getExpensesWithCategoryForPeriod(
        userId: String,
        from: Long,
        to: Long
    ): List<ExpenseWithCategory> {
        return expenseDao.getExpensesWithCategoryForPeriod(userId, from, to)
    }

    suspend fun getCategoryTotalsForPeriod(userId: String, from: Long, to: Long): List<CategoryTotal> {
        return expenseDao.getCategoryTotalsForPeriod(userId, from, to)
    }

    suspend fun getTotalSpentForPeriod(userId: String, from: Long, to: Long): Double {
        return expenseDao.getTotalSpentForPeriod(userId, from, to)
    }

    suspend fun setGoal(userId: String, min: Double, max: Double) {
        val existing = budgetGoalDao.getGoalForUser(userId)
        val goal = BudgetGoal(id = existing?.id ?: "", userId = userId, minGoal = min, maxGoal = max)
        budgetGoalDao.setGoal(goal)
    }

    suspend fun getGoal(userId: String): BudgetGoal? {
        return budgetGoalDao.getGoalForUser(userId)
    }
}
