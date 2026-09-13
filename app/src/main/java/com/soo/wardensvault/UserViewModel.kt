package com.soo.wardensvault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/*
Views Model Class
used to manage user-related operations
acts as bridge between UI and RoomDB
 */

//UserViewModel extends AndroidViewModel - gives it access to app context
//useful for initializing RoomDB
class UserViewModel(application: Application) : AndroidViewModel(application) {
    //Gets an instance of UserDao from RoomDB
    private val userDao = AppDatabase.getDatabase(application).userDao()

    //Registers new user by inserting data into db
    fun registerUser(user: User) = viewModelScope.launch {
        userDao.insertUser(user)
    }

    //checks if user exists
    suspend fun loginUser(username: String, password: String): User? {
        return userDao.login(username, password)
    }

    //checks if username exists in db
    suspend fun isUsernameTaken(username: String): Boolean {
        return userDao.getUserByUsername(username) != null
    }
}