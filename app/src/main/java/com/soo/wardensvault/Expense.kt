package com.soo.wardensvault

/*
Expense document - users/{uid}/expenses/{id}.
All dates and times are epoch millis so date-range queries work directly in Firestore.
photoPath stores the file path on device storage, Not the image itself.

One-off expense:  date = the day it happened; startTime/endTime are optional (null = not given).
Recurring expense: date = the start date, endDate = the last day it repeats (null = keeps
repeating) and frequency = Weekly / Monthly / Yearly. It is saved ONCE, and ExpenseDao works
out each repeat that falls inside the period being looked at.
 */
data class Expense(
    val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val amount: Double = 0.0,
    val date: Long = 0L,
    val startTime: Long? = null,
    val endTime: Long? = null,
    val description: String = "",
    val photoPath: String? = null,
    val recurring: Boolean = false,
    val frequency: String = "Monthly",
    val endDate: Long? = null
)
