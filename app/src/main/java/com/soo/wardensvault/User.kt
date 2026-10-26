package com.soo.wardensvault

/*
User profile document - stored at users/{uid} in Firestore.
id is the Firebase Authentication uid. The password is NOT stored here:
Firebase Authentication holds and hashes it.

Every field has a default value so Firestore can rebuild the object with toObject().
 */
data class User(
    val id: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = ""
)
