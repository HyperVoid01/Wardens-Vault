package com.soo.wardensvault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseDao = AppDatabase.getDatabase(application).expenseDao()
    private val budgetGoalDao = AppDatabase.getDatabase(application).budgetGoalDao()

    suspend fun createExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    suspend fun getExpensesForPeriod(userId: Int, from: Long, to: Long): List<Expense> {
        return expenseDao.getExpensesForPeriod(userId, from, to)
    }

    suspend fun getCategoryTotalsForPeriod(userId: Int, from: Long, to: Long): List<CategoryTotal> {
        return expenseDao.getCategoryTotalsForPeriod(userId, from, to)
    }

    suspend fun getTotalSpentForPeriod(userId: Int, from: Long, to: Long): Double {
        return expenseDao.getTotalSpentForPeriod(userId, from, to)
    }

    fun setGoal(userId: Int, min: Double, max: Double) = viewModelScope.launch {
        val existing = budgetGoalDao.getGoalForUser(userId)
        val goal = BudgetGoal(id = existing?.id ?: 0, userId = userId, minGoal = min, maxGoal = max)
        budgetGoalDao.setGoal(goal)
    }

    suspend fun getGoal(userId: Int): BudgetGoal? {
        return budgetGoalDao.getGoalForUser(userId)
    }
}
