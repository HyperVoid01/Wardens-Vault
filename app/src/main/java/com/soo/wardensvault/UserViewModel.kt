package com.soo.wardensvault

import android.app.Application
import androidx.lifecycle.AndroidViewModel

/*
Views Model Class
used to manage user-related operations
acts as bridge between UI and RoomDB
 */

//UserViewModel extends AndroidViewModel which gives it access to app context
class UserViewModel(application: Application) : AndroidViewModel(application) {
    //Gets an instance of UserDao from RoomDB
    private val userDao = AppDatabase.getDatabase(application).userDao()

    //Registers a new user (password is hashed here) and returns the new user's id
    suspend fun registerUser(fullName: String, username: String, email: String, password: String): Int {
        val id = userDao.insertUser(
            User(
                fullName = fullName,
                username = username,
                password = PasswordUtils.hash(password),
                email = email
            )
        )
        return id.toInt()
    }

    //returns the user when username + password match, otherwise null.
    //Old plain-text passwords are upgraded to a hash on first successful login.
    suspend fun loginUser(username: String, password: String): User? {
        val user = userDao.getUserByUsername(username) ?: return null
        if (!PasswordUtils.verify(password, user.password)) return null
        if (!PasswordUtils.isHashed(user.password)) {
            userDao.updatePassword(user.id, PasswordUtils.hash(password))
        }
        return user
    }

    //checks if username exists in db
    suspend fun isUsernameTaken(username: String): Boolean {
        return userDao.getUserByUsername(username) != null
    }
}
