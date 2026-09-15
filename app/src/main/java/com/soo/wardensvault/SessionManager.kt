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
    private const val KEY_FULL_NAME = "logged_in_full_name"

    fun saveLoggedInUser(context: Context, userId: Int, fullName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_USER_ID, userId)
            .putString(KEY_FULL_NAME, fullName)
            .apply()
    }

    //returns -1 if no user is logged in
    fun getLoggedInUserId(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_USER_ID, -1)
    }

    fun getLoggedInFullName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_FULL_NAME, null)
    }

    fun clearSession(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
