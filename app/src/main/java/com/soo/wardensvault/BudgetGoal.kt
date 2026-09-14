package com.soo.wardensvault

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

//One row per user - their current min/max monthly spending goal
@Entity(
    tableName = "budget_goals",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BudgetGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val minGoal: Double,
    val maxGoal: Double
)
