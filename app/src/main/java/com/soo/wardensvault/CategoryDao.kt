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
//
// Based on: Order and limit data with Cloud Firestore
// Author:   Firebase
// URL:      https://firebase.google.com/docs/firestore/query-data/order-limit-data
// Accessed: 27 October 2026
// Used for: Filtered and ordered queries

package com.soo.wardensvault

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

//Categories - users/{uid}/categories/{id}
class CategoryDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {

    //creates the category with a new auto-generated id and returns that id
    suspend fun insertCategory(category: Category): String {
        val ref = col(category.userId, "categories").document()
        ref.set(category.copy(id = ref.id)).await()
        return ref.id
    }

    suspend fun updateCategory(category: Category) {
        col(category.userId, "categories").document(category.id).set(category).await()
    }

    suspend fun deleteCategory(category: Category) {
        col(category.userId, "categories").document(category.id).delete().await()
    }

    suspend fun getCategoriesForUser(userId: String): List<Category> {
        return col(userId, "categories")
            .orderBy("name", Query.Direction.ASCENDING)
            .get().await()
            .toObjects(Category::class.java)
    }

    suspend fun getCategoryById(userId: String, categoryId: String): Category? {
        return col(userId, "categories").document(categoryId).get().await()
            .toObject(Category::class.java)
    }
}
