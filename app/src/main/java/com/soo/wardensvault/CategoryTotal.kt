package com.soo.wardensvault

//Result row for "total spent per category over a period" (worked out in ExpenseDao)
data class CategoryTotal(
    val categoryId: String,
    val categoryName: String,
    val total: Double
)
