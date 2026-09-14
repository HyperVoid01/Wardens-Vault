package com.soo.wardensvault

//Result row for "total spent per category over a period" queries
data class CategoryTotal(
    val categoryId: Int,
    val categoryName: String,
    val total: Double
)
