package com.soo.wardensvault

import androidx.room.Entity
import androidx.room.PrimaryKey

//User data entity
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val username: String,
    val password: String
)
