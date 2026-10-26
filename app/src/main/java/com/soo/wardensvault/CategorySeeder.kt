package com.soo.wardensvault

import android.content.Context

//Creates the default expense categories from the design document
object CategorySeeder {
    val defaults = listOf(
        "Groceries", "Transport", "Entertainment", "Shopping", "Healthcare", "Education", "Utilities"
    )

    //only seeds a user who has no categories at all, so deleted defaults are never re-added
    suspend fun ensureDefaults(context: Context, userId: String) {
        val dao = AppDatabase.getDatabase(context).categoryDao()
        if (dao.getCategoriesForUser(userId).isNotEmpty()) return
        for (name in defaults) dao.insertCategory(Category(userId = userId, name = name))
    }
}
