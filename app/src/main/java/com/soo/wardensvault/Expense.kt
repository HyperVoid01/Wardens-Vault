package com.soo.wardensvault

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/*
Expense data entity.
date/startTime/endTime are stored as epoch millis (Long) so date-range
queries (SELECT ... WHERE date BETWEEN :from AND :to) work directly in SQL.
photoPath stores the file path on device storage, NOT the image itself -
Room/SQLite should never store raw image blobs for this use case.
 */
@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val categoryId: Int,
    val amount: Double,
    val date: Long,       // epoch millis, the calendar day of the expense
    val startTime: Long,  // epoch millis
    val endTime: Long,    // epoch millis
    val description: String,
    val photoPath: String? = null
)
