package com.soo.wardensvault

import androidx.room.Entity
import androidx.room.PrimaryKey

//User data entity. password holds a salted PBKDF2 hash (see PasswordUtils), never plain text.
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val username: String,
    val password: String,
    val email: String = ""
)
