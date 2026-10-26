// Code Attribution
// Based on: Authenticate with Firebase using Password-Based Accounts on Android
// Author:   Firebase
// URL:      https://firebase.google.com/docs/auth/android/password-auth
// Accessed: 27 October 2026
// Used for: Firebase sign-up, sign-in, current user and sign-out
//
// Based on: Save simple data with SharedPreferences
// Author:   Android Developers
// URL:      https://developer.android.com/training/data-storage/shared-preferences
// Accessed: 27 October 2026
// Used for: Reading and writing SharedPreferences

package com.soo.wardensvault

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/*
SessionManager
Firebase Authentication remembers who is logged in (it survives app restarts on its own),
so the user id comes straight from Firebase. SharedPreferences only caches the full name
so screens can show it without a database read.
 */
object SessionManager {
    private const val PREFS_NAME = "warden_session"
    private const val KEY_FULL_NAME = "logged_in_full_name"

    //userId is kept in the signature so existing callers don't change; Firebase already knows it
    @Suppress("UNUSED_PARAMETER")
    fun saveLoggedInUser(context: Context, userId: String, fullName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FULL_NAME, fullName)
            .apply()
    }

    //the Firebase uid of the logged-in user, or "" if nobody is logged in
    @Suppress("UNUSED_PARAMETER")
    fun getLoggedInUserId(context: Context): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
    }

    fun getLoggedInFullName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FULL_NAME, null)
    }

    //signs out of Firebase and forgets the cached name
    fun clearSession(context: Context) {
        FirebaseAuth.getInstance().signOut()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
