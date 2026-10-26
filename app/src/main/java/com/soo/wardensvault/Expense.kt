package com.soo.wardensvault

/*
Expense document - users/{uid}/expenses/{id}.
date/startTime/endTime are stored as epoch millis so date-range queries work directly in Firestore.
photoPath stores the file path on device storage, Not the image itself
 */
data class Expense(
    val id: String = "",
    val userId: String = "",
    val categoryId: String = "",
    val amount: Double = 0.0,
    val date: Long = 0L,       // epoch millis, the calendar day of the expense
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val description: String = "",
    val photoPath: String? = null
)
