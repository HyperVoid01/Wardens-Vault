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

//Monthly budget goal - one document per user at users/{uid}/budget_goals/goal
class BudgetGoalDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private fun goalDoc(userId: String) = col(userId, "budget_goals").document(GOAL_ID)

    //set() overwrites the single goal document, so this both creates and updates
    suspend fun setGoal(goal: BudgetGoal): String {
        goalDoc(goal.userId).set(goal.copy(id = GOAL_ID)).await()
        return GOAL_ID
    }

    suspend fun getGoalForUser(userId: String): BudgetGoal? {
        return goalDoc(userId).get().await().toObject(BudgetGoal::class.java)
    }

    companion object {
        const val GOAL_ID = "goal"
    }
}
