package com.soo.wardensvault

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    //List of expenses for a user within a user-selectable date range
    @Query(
        "SELECT * FROM expenses WHERE userId = :userId AND date BETWEEN :from AND :to " +
        "ORDER BY date DESC"
    )
    suspend fun getExpensesForPeriod(userId: Int, from: Long, to: Long): List<Expense>

    //Total spent per category within a period - drives the "totals" screen
    @Query(
        "SELECT categories.id AS categoryId, categories.name AS categoryName, " +
        "COALESCE(SUM(expenses.amount), 0) AS total " +
        "FROM categories " +
        "LEFT JOIN expenses ON expenses.categoryId = categories.id " +
        "AND expenses.date BETWEEN :from AND :to " +
        "WHERE categories.userId = :userId " +
        "GROUP BY categories.id " +
        "ORDER BY categories.name ASC"
    )
    suspend fun getCategoryTotalsForPeriod(userId: Int, from: Long, to: Long): List<CategoryTotal>

    //Overall total spent by the user in a period - compared against min/max goals
    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM expenses " +
        "WHERE userId = :userId AND date BETWEEN :from AND :to"
    )
    suspend fun getTotalSpentForPeriod(userId: Int, from: Long, to: Long): Double
}
