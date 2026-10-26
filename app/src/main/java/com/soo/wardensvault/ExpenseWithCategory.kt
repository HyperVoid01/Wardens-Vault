package com.soo.wardensvault

//Expense joined with its category name - what the expense list, calendar and graph display.
//For a recurring expense, date is the date of this particular repeat.
data class ExpenseWithCategory(
    val id: String,
    val categoryName: String,
    val description: String,
    val amount: Double,
    val date: Long,
    val startTime: Long?,
    val endTime: Long?,
    val photoPath: String?,
    val recurring: Boolean = false,
    val frequency: String = "Monthly",
    val endDate: Long? = null
)
