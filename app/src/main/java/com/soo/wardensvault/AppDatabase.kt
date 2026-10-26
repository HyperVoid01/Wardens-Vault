package com.soo.wardensvault

import android.content.Context
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

/*
AppDatabase - the single access point to the online database (Cloud Firestore).

It used to be a Room database. It keeps the same shape so the rest of the app is unchanged:
  AppDatabase.getDatabase(context).expenseDao().getTotalSpentForPeriod(...)
but every DAO now reads and writes Firestore instead of a local SQLite file.

Firestore layout - everything a user owns lives under their own document:
  users/{uid}                          -> User profile
  users/{uid}/categories/{id}          -> Category
  users/{uid}/expenses/{id}            -> Expense
  users/{uid}/budget_goals/goal        -> BudgetGoal
  users/{uid}/player_stats/stats       -> PlayerStats
  users/{uid}/category_limits/{categoryId}, incomes, bills, bill_payments, savings_goals,
  net_worth_items, net_worth_snapshots/{day}, unlocked_achievements/{key}, owned_items/{itemKey}
  usernames/{lowercase username}       -> { uid, email } lookup so users can log in with a username
 */
class AppDatabase private constructor(firestore: FirebaseFirestore) {
    private val userDao = UserDao(firestore)
    private val categoryDao = CategoryDao(firestore)
    private val expenseDao = ExpenseDao(firestore)
    private val budgetGoalDao = BudgetGoalDao(firestore)
    private val playerStatsDao = PlayerStatsDao(firestore)
    private val categoryLimitDao = CategoryLimitDao(firestore)
    private val incomeDao = IncomeDao(firestore)
    private val billDao = BillDao(firestore)
    private val billPaymentDao = BillPaymentDao(firestore)
    private val savingsGoalDao = SavingsGoalDao(firestore)
    private val netWorthDao = NetWorthDao(firestore)
    private val achievementDao = AchievementDao(firestore)
    private val ownedItemDao = OwnedItemDao(firestore)

    fun userDao(): UserDao = userDao
    fun categoryDao(): CategoryDao = categoryDao
    fun expenseDao(): ExpenseDao = expenseDao
    fun budgetGoalDao(): BudgetGoalDao = budgetGoalDao
    fun playerStatsDao(): PlayerStatsDao = playerStatsDao
    fun categoryLimitDao(): CategoryLimitDao = categoryLimitDao
    fun incomeDao(): IncomeDao = incomeDao
    fun billDao(): BillDao = billDao
    fun billPaymentDao(): BillPaymentDao = billPaymentDao
    fun savingsGoalDao(): SavingsGoalDao = savingsGoalDao
    fun netWorthDao(): NetWorthDao = netWorthDao
    fun achievementDao(): AchievementDao = achievementDao
    fun ownedItemDao(): OwnedItemDao = ownedItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        //returns the shared instance (context is kept in the signature so callers don't change)
        @Suppress("UNUSED_PARAMETER")
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(FirebaseFirestore.getInstance()).also { INSTANCE = it }
            }
        }
    }
}

//Shared helper for every DAO: finds a collection that belongs to one user
abstract class FirestoreDao(protected val firestore: FirebaseFirestore) {
    protected fun userDoc(userId: String) = firestore.collection("users").document(userId)

    protected fun col(userId: String, name: String): CollectionReference =
        userDoc(userId).collection(name)
}
