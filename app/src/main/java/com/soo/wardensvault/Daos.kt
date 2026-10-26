package com.soo.wardensvault

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/*
Data access objects for the tables in GameEntities.kt
 */

@Dao
interface CategoryLimitDao {
    @Query("SELECT * FROM category_limits WHERE userId = :userId")
    suspend fun getAll(userId: Int): List<CategoryLimit>

    @Query("SELECT * FROM category_limits WHERE userId = :userId AND categoryId = :categoryId LIMIT 1")
    suspend fun getForCategory(userId: Int, categoryId: Int): CategoryLimit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(limit: CategoryLimit): Long

    @Query("DELETE FROM category_limits WHERE userId = :userId AND categoryId = :categoryId")
    suspend fun remove(userId: Int, categoryId: Int)
}

@Dao
interface IncomeDao {
    @Insert
    suspend fun insert(income: Income): Long

    @Update
    suspend fun update(income: Income)

    @Delete
    suspend fun delete(income: Income)

    @Query("SELECT * FROM incomes WHERE userId = :userId ORDER BY date DESC")
    suspend fun getAll(userId: Int): List<Income>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM incomes WHERE userId = :userId AND date BETWEEN :from AND :to")
    suspend fun totalForPeriod(userId: Int, from: Long, to: Long): Double

    @Query("SELECT COUNT(*) FROM incomes WHERE userId = :userId")
    suspend fun count(userId: Int): Int
}

@Dao
interface BillDao {
    @Insert
    suspend fun insert(bill: Bill): Long

    @Update
    suspend fun update(bill: Bill)

    @Delete
    suspend fun delete(bill: Bill)

    @Query("SELECT * FROM bills WHERE userId = :userId ORDER BY dueDate ASC")
    suspend fun getAll(userId: Int): List<Bill>

    @Query("SELECT COUNT(*) FROM bills WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: Int): Int

    @Query("UPDATE bills SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId AND userId = :userId")
    suspend fun reassignCategory(userId: Int, oldCategoryId: Int, newCategoryId: Int)
}

@Dao
interface BillPaymentDao {
    @Insert
    suspend fun insert(payment: BillPayment): Long

    @Query("SELECT COUNT(*) FROM bill_payments WHERE userId = :userId")
    suspend fun count(userId: Int): Int
}

@Dao
interface SavingsGoalDao {
    @Insert
    suspend fun insert(goal: SavingsGoal): Long

    @Update
    suspend fun update(goal: SavingsGoal)

    @Delete
    suspend fun delete(goal: SavingsGoal)

    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY completed ASC, id DESC")
    suspend fun getAll(userId: Int): List<SavingsGoal>

    @Query("SELECT COUNT(*) FROM savings_goals WHERE userId = :userId AND completed = 1")
    suspend fun countCompleted(userId: Int): Int
}

@Dao
interface NetWorthDao {
    @Insert
    suspend fun insertItem(item: NetWorthItem): Long

    @Update
    suspend fun updateItem(item: NetWorthItem)

    @Delete
    suspend fun deleteItem(item: NetWorthItem)

    @Query("SELECT * FROM net_worth_items WHERE userId = :userId ORDER BY kind ASC, groupName ASC, name ASC")
    suspend fun getItems(userId: Int): List<NetWorthItem>

    @Insert
    suspend fun insertSnapshot(snapshot: NetWorthSnapshot): Long

    @Update
    suspend fun updateSnapshot(snapshot: NetWorthSnapshot)

    @Query("SELECT * FROM net_worth_snapshots WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getSnapshotForDay(userId: Int, date: Long): NetWorthSnapshot?

    @Query("SELECT * FROM net_worth_snapshots WHERE userId = :userId ORDER BY date ASC")
    suspend fun getSnapshots(userId: Int): List<NetWorthSnapshot>
}

@Dao
interface AchievementDao {
    @Insert
    suspend fun insert(unlocked: UnlockedAchievement): Long

    @Query("SELECT * FROM unlocked_achievements WHERE userId = :userId ORDER BY unlockedAt ASC")
    suspend fun getAll(userId: Int): List<UnlockedAchievement>
}

@Dao
interface OwnedItemDao {
    @Insert
    suspend fun insert(item: OwnedItem): Long

    @Update
    suspend fun update(item: OwnedItem)

    @Query("SELECT * FROM owned_items WHERE userId = :userId")
    suspend fun getAll(userId: Int): List<OwnedItem>
}
