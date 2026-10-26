// Code Attribution
// Based on: Add data to Cloud Firestore
// Author:   Firebase
// URL:      https://firebase.google.com/docs/firestore/manage-data/add-data
// Accessed: 27 October 2026
// Used for: Writing, updating and deleting documents
//
// Based on: Get data with Cloud Firestore
// Author:   Firebase
// URL:      https://firebase.google.com/docs/firestore/query-data/get-data
// Accessed: 27 October 2026
// Used for: Reading documents and collections

package com.soo.wardensvault

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/*
User Data Object
Reads and writes user profiles (users/{uid}) and the username lookup (usernames/{name}).
Passwords are handled by Firebase Authentication, never stored here.
 */
class UserDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private val usernames = firestore.collection("usernames")

    //usernames are matched without caring about upper/lower case
    private fun key(username: String) = username.trim().lowercase()

    //saves the profile document; the id must be the Firebase Auth uid
    suspend fun insertUser(user: User): String {
        userDoc(user.id).set(user).await()
        return user.id
    }

    suspend fun getUserById(userId: String): User? {
        return userDoc(userId).get().await().toObject(User::class.java)
    }

    suspend fun isUsernameTaken(username: String): Boolean {
        return usernames.document(key(username)).get().await().exists()
    }

    //email linked to a username, or null if no account uses that username
    suspend fun getEmailForUsername(username: String): String? {
        return usernames.document(key(username)).get().await().getString("email")
    }

    //claims a username for a new account (the security rules stop it being taken twice)
    suspend fun reserveUsername(username: String, userId: String, email: String) {
        usernames.document(key(username))
            .set(mapOf("uid" to userId, "email" to email))
            .await()
    }
}
