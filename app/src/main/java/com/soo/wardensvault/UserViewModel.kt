// Code Attribution
// Based on: ViewModel overview
// Author:   Android Developers
// URL:      https://developer.android.com/topic/libraries/architecture/viewmodel
// Accessed: 27 October 2026
// Used for: AndroidViewModel structure
//
// Based on: Authenticate with Firebase using Password-Based Accounts on Android
// Author:   Firebase
// URL:      https://firebase.google.com/docs/auth/android/password-auth
// Accessed: 27 October 2026
// Used for: Firebase sign-up, sign-in, current user and sign-out

package com.soo.wardensvault

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await

//thrown by registerUser when another account already uses the chosen username
class UsernameTakenException : Exception("Username already taken")

/*
Views Model Class
used to manage user-related operations
acts as bridge between the UI, Firebase Authentication and Firestore
 */
class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val userDao = AppDatabase.getDatabase(application).userDao()

    /*
    Creates the Firebase account (Firebase hashes and stores the password), claims the
    username and saves the profile. Returns the new user's id.
    The new user is left signed in so their default data can be created straight after.
     */
    suspend fun registerUser(fullName: String, username: String, email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw IllegalStateException("Registration failed")
        val uid = firebaseUser.uid

        try {
            userDao.reserveUsername(username, uid, email)
        } catch (e: Exception) {
            //undo the account so the email can be used again, then report why it failed
            firebaseUser.delete().await()
            if (e is FirebaseFirestoreException &&
                e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            ) {
                throw UsernameTakenException()   //the rules refused because the name was just taken
            }
            throw e
        }

        userDao.insertUser(User(id = uid, fullName = fullName, username = username, email = email))
        return uid
    }

    /*
    Signs in with a username OR an email address plus the password.
    Returns the user's profile, or null when the details are wrong.
    Network problems are thrown so the screen can say so.
     */
    suspend fun loginUser(usernameOrEmail: String, password: String): User? {
        val input = usernameOrEmail.trim()
        val email = if (input.contains("@")) input else userDao.getEmailForUsername(input) ?: return null

        try {
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            return null
        } catch (e: FirebaseAuthInvalidUserException) {
            return null
        }

        val uid = auth.currentUser?.uid ?: return null
        return userDao.getUserById(uid) ?: User(id = uid, email = email)
    }

    //checks if username is already used by another account
    suspend fun isUsernameTaken(username: String): Boolean {
        return userDao.isUsernameTaken(username)
    }
}
