package com.soo.wardensvault

//One document per user (users/{uid}/budget_goals/goal) - their current min/max monthly spending goal
data class BudgetGoal(
    val id: String = "",
    val userId: String = "",
    val minGoal: Double = 0.0,
    val maxGoal: Double = 0.0
)
