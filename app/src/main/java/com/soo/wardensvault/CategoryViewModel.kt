// Code Attribution
// Based on: ViewModel overview
// Author:   Android Developers
// URL:      https://developer.android.com/topic/libraries/architecture/viewmodel
// Accessed: 27 October 2026
// Used for: AndroidViewModel structure

package com.soo.wardensvault

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()

    //suspend so callers await completion before refreshing a list
    suspend fun createCategory(userId: String, name: String) {
        categoryDao.insertCategory(Category(userId = userId, name = name))
    }

    suspend fun getCategories(userId: String): List<Category> {
        return categoryDao.getCategoriesForUser(userId)
    }
}
