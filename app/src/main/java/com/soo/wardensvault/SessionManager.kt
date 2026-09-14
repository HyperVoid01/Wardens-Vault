package com.soo.wardensvault

import android.content.Context

/*
SessionManager
Persists which user is currently logged in using SharedPreferences,
so any Activity/ViewModel can find out "who is logged in" without
needing it passed through every Intent.
 */
object SessionManager {
    private const val PREFS_NAME = "warden_session"
    private const val KEY_USER_ID = "logged_in_user_id"

    fun saveLoggedInUser(context: Context, userId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_USER_ID, userId).apply()
    }

    //returns -1 if no user is logged in
    fun getLoggedInUserId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_USER_ID).apply()
    }
}
