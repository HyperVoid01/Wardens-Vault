package com.soo.wardensvault

//Category document - users/{uid}/categories/{id}. Expenses, bills and limits belong to a category.
data class Category(
    val id: String = "",
    val userId: String = "",
    val name: String = ""
)
