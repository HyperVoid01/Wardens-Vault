package com.soo.wardensvault

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

//database class that holds the database and serves as main access point

/*
class must contain @Database annotation and an entities array that
contains all data entities associated with the database
 */
@Database(
    entities = [
        User::class, Category::class, Expense::class, BudgetGoal::class, PlayerStats::class,
        CategoryLimit::class, Income::class, Bill::class, BillPayment::class,
        SavingsGoal::class, NetWorthItem::class, NetWorthSnapshot::class,
        UnlockedAchievement::class, OwnedItem::class
    ],
    version = 4
)
//abstract class because it extends RoomDatabase
abstract class AppDatabase : RoomDatabase() {
    /*
    must define an abstract method with no arguments that returns
    DAO class. Must be created for each DAO class
     */

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun budgetGoalDao(): BudgetGoalDao
    abstract fun playerStatsDao(): PlayerStatsDao
    abstract fun categoryLimitDao(): CategoryLimitDao
    abstract fun incomeDao(): IncomeDao
    abstract fun billDao(): BillDao
    abstract fun billPaymentDao(): BillPaymentDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun netWorthDao(): NetWorthDao
    abstract fun achievementDao(): AchievementDao
    abstract fun ownedItemDao(): OwnedItemDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        //2 -> 3: adds the player_stats table without wiping existing user data
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `player_stats` (" +
                        "`userId` INTEGER NOT NULL, `xp` INTEGER NOT NULL, " +
                        "`gold` INTEGER NOT NULL, `loginStreak` INTEGER NOT NULL, " +
                        "`longestStreak` INTEGER NOT NULL, `lastClaimDay` INTEGER NOT NULL, " +
                        "`expensesLogged` INTEGER NOT NULL, PRIMARY KEY(`userId`), " +
                        "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) " +
                        "ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
            }
        }

        private const val FK_USER =
            "FOREIGN KEY(`userId`) REFERENCES `users`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
        private const val FK_CATEGORY =
            "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"

        //3 -> 4: email on users plus all the new feature tables, existing data is kept
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `email` TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `category_limits` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, " +
                        "`categoryId` INTEGER NOT NULL, `limitAmount` REAL NOT NULL, " +
                        "$FK_USER, $FK_CATEGORY)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `incomes` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, " +
                        "`amount` REAL NOT NULL, `source` TEXT NOT NULL, `type` TEXT NOT NULL, " +
                        "`date` INTEGER NOT NULL, $FK_USER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bills` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `amount` REAL NOT NULL, `dueDate` INTEGER NOT NULL, " +
                        "`frequency` TEXT NOT NULL, `categoryId` INTEGER NOT NULL, " +
                        "`isPaid` INTEGER NOT NULL, $FK_USER, $FK_CATEGORY)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `bill_payments` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, " +
                        "`billName` TEXT NOT NULL, `amount` REAL NOT NULL, " +
                        "`paidDate` INTEGER NOT NULL, $FK_USER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `savings_goals` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `targetAmount` REAL NOT NULL, " +
                        "`currentAmount` REAL NOT NULL, `deadline` INTEGER, " +
                        "`completed` INTEGER NOT NULL, $FK_USER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `net_worth_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, " +
                        "`name` TEXT NOT NULL, `kind` TEXT NOT NULL, `groupName` TEXT NOT NULL, " +
                        "`amount` REAL NOT NULL, $FK_USER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `net_worth_snapshots` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, " +
                        "`date` INTEGER NOT NULL, `netWorth` REAL NOT NULL, $FK_USER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `unlocked_achievements` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, " +
                        "`achievementKey` TEXT NOT NULL, `unlockedAt` INTEGER NOT NULL, $FK_USER)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `owned_items` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, " +
                        "`itemKey` TEXT NOT NULL, `equipped` INTEGER NOT NULL, $FK_USER)"
                )
            }
        }

        //creates instance of database
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "user_db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .fallbackToDestructiveMigration(true)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
