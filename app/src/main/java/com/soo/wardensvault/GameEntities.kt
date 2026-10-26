package com.soo.wardensvault

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/*
Entities added for the remaining design-document features.
Every row is scoped to a userId so accounts stay isolated.
 */

//Per-category monthly spending limit (one row per user + category)
@Entity(
    tableName = "category_limits",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class CategoryLimit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val categoryId: Int,
    val limitAmount: Double
)

//Income entry: Salary / Freelance / Bonus / Other
@Entity(
    tableName = "incomes",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val amount: Double,
    val source: String,
    val type: String,
    val date: Long
)

//Recurring bill - shown in the game as an enemy. dueDate is the current cycle's due date.
@Entity(
    tableName = "bills",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val name: String,
    val amount: Double,
    val dueDate: Long,
    val frequency: String,   // Weekly / Monthly / Yearly
    val categoryId: Int,
    val isPaid: Boolean
)

//History of every bill payment ("enemy defeated")
@Entity(
    tableName = "bill_payments",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class BillPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val billName: String,
    val amount: Double,
    val paidDate: Long
)

//Savings goal with optional deadline
@Entity(
    tableName = "savings_goals",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: Long?,
    val completed: Boolean
)

//Asset or liability used for the net worth calculation
@Entity(
    tableName = "net_worth_items",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class NetWorthItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val name: String,
    val kind: String,        // ASSET or LIABILITY
    val groupName: String,   // Cash, Savings, Investments, Other assets, Loans, Credit card debt, Other debts
    val amount: Double
)

//One net worth value per day, so the change over time can be shown
@Entity(
    tableName = "net_worth_snapshots",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class NetWorthSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val date: Long,
    val netWorth: Double
)

//Achievements the user has unlocked (definitions live in AchievementManager)
@Entity(
    tableName = "unlocked_achievements",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class UnlockedAchievement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val achievementKey: String,
    val unlockedAt: Long
)

//Cosmetic items bought with Gold (definitions live in CosmeticCatalog)
@Entity(
    tableName = "owned_items",
    foreignKeys = [
        ForeignKey(entity = User::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class OwnedItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val itemKey: String,
    val equipped: Boolean
)
