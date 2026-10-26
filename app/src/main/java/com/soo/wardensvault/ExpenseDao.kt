package com.soo.wardensvault

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/*
Expenses - users/{uid}/expenses/{id}
Firestore has no SQL JOIN or SUM, so category names and totals are worked out here
after the matching expenses are loaded.

Recurring expenses are stored once. getExpensesForPeriod() turns each one into its repeats
(one per week / month / year) that fall inside the period, so every total, chart, alert and
the calendar count a recurring expense every time it happens.
 */
class ExpenseDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {

    private fun expenses(userId: String) = col(userId, "expenses")

    //creates the expense with a new auto-generated id and returns that id
    suspend fun insertExpense(expense: Expense): String {
        val ref = expenses(expense.userId).document()
        ref.set(expense.copy(id = ref.id)).await()
        return ref.id
    }

    //Every expense (and every repeat of a recurring one) within a date range, newest first
    suspend fun getExpensesForPeriod(userId: String, from: Long, to: Long): List<Expense> {
        //one-off expenses dated inside the range
        val oneOff = expenses(userId)
            .whereGreaterThanOrEqualTo("date", from)
            .whereLessThanOrEqualTo("date", to)
            .orderBy("date", Query.Direction.DESCENDING)
            .get().await()
            .toObjects(Expense::class.java)
            .filter { !it.recurring }

        //recurring expenses, expanded into the repeats that land inside the range
        val repeats = expenses(userId)
            .whereEqualTo("recurring", true)
            .get().await()
            .toObjects(Expense::class.java)
            .flatMap { occurrences(it, from, to) }

        return (oneOff + repeats).sortedByDescending { it.date }
    }

    //the dates a recurring expense happens on between [from] and [to]
    private fun occurrences(expense: Expense, from: Long, to: Long): List<Expense> {
        val last = minOf(to, expense.endDate?.let { DateUtils.endOfDay(it) } ?: Long.MAX_VALUE)
        val result = mutableListOf<Expense>()
        var day = expense.date
        var steps = 0
        while (day <= last && steps < MAX_REPEATS) {
            if (day >= from) result.add(expense.copy(date = day))
            day = DateUtils.addFrequency(day, expense.frequency)
            steps++
        }
        return result
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
                photoPath = e.photoPath,
                recurring = e.recurring,
                frequency = e.frequency,
                endDate = e.endDate
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

    companion object {
        //safety limit when expanding a recurring expense (over 19 years of weekly repeats)
        private const val MAX_REPEATS = 1000
    }
}
