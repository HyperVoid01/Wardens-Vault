package com.soo.wardensvault

import com.google.firebase.firestore.PropertyName

/*
Documents for the remaining design-document features.
Each one lives under users/{uid}/<collection>/{id}, so accounts stay isolated.
Every field has a default value so Firestore can rebuild the object with toObject().
 */

//Per-category monthly spending limit - document id is the categoryId (one per category)
data class CategoryLimit(
    val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val limitAmount: Double = 0.0
)

//Income entry: Salary / Freelance / Bonus / Other
data class Income(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val source: String = "",
    val type: String = "",
    val date: Long = 0L
)

//Recurring bill - shown in the game as an enemy. dueDate is the current cycle's due date.
data class Bill(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val amount: Double = 0.0,
    val dueDate: Long = 0L,
    val frequency: String = "Monthly",   // Weekly / Monthly / Yearly
    val categoryId: String = "",
    //Firestore would otherwise save a Kotlin "isX" Boolean as "x" and fail to read it back
    @get:PropertyName("isPaid") @field:PropertyName("isPaid")
    val isPaid: Boolean = false,
    //how much has been paid towards the current cycle - smaller payments only wound the enemy
    val paidThisCycle: Double = 0.0
)

//History of every bill payment ("enemy defeated")
data class BillPayment(
    val id: String = "",
    val userId: String = "",
    val billName: String = "",
    val amount: Double = 0.0,
    val paidDate: Long = 0L
)

//Savings goal with optional deadline. createdAt keeps newest-first ordering (Firestore ids aren't ordered)
data class SavingsGoal(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val currentAmount: Double = 0.0,
    val deadline: Long? = null,
    val completed: Boolean = false,
    val createdAt: Long = 0L
)

//Asset or liability used for the net worth calculation
data class NetWorthItem(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val kind: String = "",        // ASSET or LIABILITY
    val groupName: String = "",   // Cash, Savings, Investments, Other assets, Loans, Credit card debt, Other debts
    val amount: Double = 0.0
)

//One net worth value per day (document id is the day), so the change over time can be shown
data class NetWorthSnapshot(
    val id: String = "",
    val userId: String = "",
    val date: Long = 0L,
    val netWorth: Double = 0.0
)

//Achievements the user has unlocked - document id is the achievementKey, so each unlocks once
data class UnlockedAchievement(
    val id: String = "",
    val userId: String = "",
    val achievementKey: String = "",
    val unlockedAt: Long = 0L
)

//Cosmetic items bought with Gold - document id is the itemKey (definitions live in CosmeticCatalog)
data class OwnedItem(
    val id: String = "",
    val userId: String = "",
    val itemKey: String = "",
    val equipped: Boolean = false
)
