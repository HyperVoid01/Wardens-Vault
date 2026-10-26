package com.soo.wardensvault

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/*
Expenses - users/{uid}/expenses/{id}
Firestore has no SQL JOIN or SUM, so category names and totals are worked out here
after the matching expenses are loaded.
 */
class ExpenseDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {

    private fun expenses(userId: String) = col(userId, "expenses")

    //creates the expense with a new auto-generated id and returns that id
    suspend fun insertExpense(expense: Expense): String {
        val ref = expenses(expense.userId).document()
        ref.set(expense.copy(id = ref.id)).await()
        return ref.id
    }

    //List of expenses for a user within a user-selectable date range, newest first
    suspend fun getExpensesForPeriod(userId: String, from: Long, to: Long): List<Expense> {
        return expenses(userId)
            .whereGreaterThanOrEqualTo("date", from)
            .whereLessThanOrEqualTo("date", to)
            .orderBy("date", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Expense::class.java)
    }

    //Same period list, plus each expense's category name - drives the expense list screen.
    //Like the old INNER JOIN, expenses whose category no longer exists are left out.
    suspend fun getExpensesWithCategoryForPeriod(
        userId: String,
        from: Long,
        to: Long
    ): List<ExpenseWithCategory> {
        val names = categoryNames(userId)
        return getExpensesForPeriod(userId, from, to).mapNotNull { e ->
            val name = names[e.categoryId] ?: return@mapNotNull null
            ExpenseWithCategory(
                id = e.id,
                categoryName = name,
                description = e.description,
                amount = e.amount,
                date = e.date,
                startTime = e.startTime,
                endTime = e.endTime,
                photoPath = e.photoPath
            )
        }
    }

    //Total spent per category within a period - every category is listed, even with 0 spent
    suspend fun getCategoryTotalsForPeriod(userId: String, from: Long, to: Long): List<CategoryTotal> {
        val categories = col(userId, "categories")
            .orderBy("name", Query.Direction.ASCENDING)
            .get().await()
            .toObjects(Category::class.java)
        val sums = getExpensesForPeriod(userId, from, to)
            .groupBy { it.categoryId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        return categories.map { CategoryTotal(it.id, it.name, sums[it.id] ?: 0.0) }
    }

    //Overall total spent by the user in a period - compared against min/max goals
    suspend fun getTotalSpentForPeriod(userId: String, from: Long, to: Long): Double {
        return getExpensesForPeriod(userId, from, to).sumOf { it.amount }
    }

    //Spending that is NOT a bill payment (bill payments are saved with the description "Bill - <name>")
    suspend fun getRegularSpendForPeriod(userId: String, from: Long, to: Long): Double {
        return getExpensesForPeriod(userId, from, to)
            .filter { !it.description.startsWith("Bill - ") }
            .sumOf { it.amount }
    }

    suspend fun countForCategory(userId: String, categoryId: String): Int {
        return expenses(userId).whereEqualTo("categoryId", categoryId).get().await().size()
    }

    //moves every expense from one category to another (used before deleting a category)
    suspend fun reassignCategory(userId: String, oldCategoryId: String, newCategoryId: String) {
        val docs = expenses(userId).whereEqualTo("categoryId", oldCategoryId).get().await().documents
        //a Firestore batch holds at most 500 writes
        for (chunk in docs.chunked(450)) {
            val batch = firestore.batch()
            for (doc in chunk) batch.update(doc.reference, "categoryId", newCategoryId)
            batch.commit().await()
        }
    }

    suspend fun deleteExpense(userId: String, expenseId: String) {
        expenses(userId).document(expenseId).delete().await()
    }

    private suspend fun categoryNames(userId: String): Map<String, String> {
        return col(userId, "categories").get().await()
            .toObjects(Category::class.java)
            .associate { it.id to it.name }
    }
}
