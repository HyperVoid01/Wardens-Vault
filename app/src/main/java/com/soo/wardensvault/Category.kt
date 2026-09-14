package com.soo.wardensvault

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

//Category data entity - expenses and budgets belong to a category
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val name: String
)
