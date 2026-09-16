package com.soo.wardensvault

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    private val categoryDao = AppDatabase.getDatabase(application).categoryDao()

    //suspend so callers await completion before refreshing a list
    suspend fun createCategory(userId: Int, name: String) {
        categoryDao.insertCategory(Category(userId = userId, name = name))
    }

    suspend fun getCategories(userId: Int): List<Category> {
        return categoryDao.getCategoriesForUser(userId)
    }
}
