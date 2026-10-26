// Code Attribution
// Based on: Save simple data with SharedPreferences
// Author:   Android Developers
// URL:      https://developer.android.com/training/data-storage/shared-preferences
// Accessed: 27 October 2026
// Used for: Reading and writing SharedPreferences

package com.soo.wardensvault

import android.content.Context
import android.content.SharedPreferences

//App-wide settings and small per-user flags, stored in SharedPreferences
object AppSettings {
    private const val PREFS_NAME = "warden_settings"

    const val KEY_ALL = "notif_all"
    const val KEY_BUDGET = "notif_budget"
    const val KEY_BILLS = "notif_bills"
    const val KEY_GOALS = "notif_goals"
    const val KEY_ACHIEVEMENTS = "notif_achievements"
    const val KEY_CASHFLOW = "notif_cashflow"

    fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    //notification toggles default to on
    fun isEnabled(context: Context, key: String): Boolean = prefs(context).getBoolean(key, true)

    fun setEnabled(context: Context, key: String, value: Boolean) {
        prefs(context).edit().putBoolean(key, value).apply()
    }

    //one-off flags such as "setup finished for user 3"; default false
    fun getFlag(context: Context, key: String): Boolean = prefs(context).getBoolean(key, false)

    fun setFlag(context: Context, key: String) {
        prefs(context).edit().putBoolean(key, true).apply()
    }
}
