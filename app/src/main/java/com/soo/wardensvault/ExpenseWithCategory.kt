package com.soo.wardensvault

//Expense joined with its category name - what the expense list screen displays
data class ExpenseWithCategory(
    val id: Int,
    val categoryName: String,
    val description: String,
    val amount: Double,
    val date: Long,
    val startTime: Long,
    val endTime: Long,
    val photoPath: String?
)
