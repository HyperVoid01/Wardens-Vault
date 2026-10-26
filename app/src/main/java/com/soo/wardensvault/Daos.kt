package com.soo.wardensvault

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/*
Data access objects for the documents in GameEntities.kt.
Each one stores its documents under users/{uid}/<collection>.
Lists are sorted in code where Firestore would need an extra index to sort them.
 */

class CategoryLimitDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private fun limits(userId: String) = col(userId, "category_limits")

    suspend fun getAll(userId: String): List<CategoryLimit> {
        return limits(userId).get().await().toObjects(CategoryLimit::class.java)
    }

    suspend fun getForCategory(userId: String, categoryId: String): CategoryLimit? {
        return limits(userId).document(categoryId).get().await().toObject(CategoryLimit::class.java)
    }

    //one limit per category: the document id is the categoryId, so saving again replaces it
    suspend fun upsert(limit: CategoryLimit): String {
        limits(limit.userId).document(limit.categoryId).set(limit.copy(id = limit.categoryId)).await()
        return limit.categoryId
    }

    suspend fun remove(userId: String, categoryId: String) {
        limits(userId).document(categoryId).delete().await()
    }
}

class IncomeDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private fun incomes(userId: String) = col(userId, "incomes")

    suspend fun insert(income: Income): String {
        val ref = incomes(income.userId).document()
        ref.set(income.copy(id = ref.id)).await()
        return ref.id
    }

    suspend fun update(income: Income) {
        incomes(income.userId).document(income.id).set(income).await()
    }

    suspend fun delete(income: Income) {
        incomes(income.userId).document(income.id).delete().await()
    }

    suspend fun getAll(userId: String): List<Income> {
        return incomes(userId).orderBy("date", Query.Direction.DESCENDING)
            .get().await().toObjects(Income::class.java)
    }

    suspend fun totalForPeriod(userId: String, from: Long, to: Long): Double {
        return incomes(userId)
            .whereGreaterThanOrEqualTo("date", from)
            .whereLessThanOrEqualTo("date", to)
            .get().await()
            .toObjects(Income::class.java)
            .sumOf { it.amount }
    }

    suspend fun count(userId: String): Int {
        return incomes(userId).get().await().size()
    }
}

class BillDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private fun bills(userId: String) = col(userId, "bills")

    suspend fun insert(bill: Bill): String {
        val ref = bills(bill.userId).document()
        ref.set(bill.copy(id = ref.id)).await()
        return ref.id
    }

    suspend fun update(bill: Bill) {
        bills(bill.userId).document(bill.id).set(bill).await()
    }

    suspend fun delete(bill: Bill) {
        bills(bill.userId).document(bill.id).delete().await()
    }

    suspend fun getAll(userId: String): List<Bill> {
        return bills(userId).orderBy("dueDate", Query.Direction.ASCENDING)
            .get().await().toObjects(Bill::class.java)
    }

    suspend fun countForCategory(userId: String, categoryId: String): Int {
        return bills(userId).whereEqualTo("categoryId", categoryId).get().await().size()
    }

    suspend fun reassignCategory(userId: String, oldCategoryId: String, newCategoryId: String) {
        val docs = bills(userId).whereEqualTo("categoryId", oldCategoryId).get().await().documents
        for (chunk in docs.chunked(450)) {
            val batch = firestore.batch()
            for (doc in chunk) batch.update(doc.reference, "categoryId", newCategoryId)
            batch.commit().await()
        }
    }
}

class BillPaymentDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private fun payments(userId: String) = col(userId, "bill_payments")

    suspend fun insert(payment: BillPayment): String {
        val ref = payments(payment.userId).document()
        ref.set(payment.copy(id = ref.id)).await()
        return ref.id
    }

    suspend fun count(userId: String): Int {
        return payments(userId).get().await().size()
    }
}

class SavingsGoalDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private fun goals(userId: String) = col(userId, "savings_goals")

    suspend fun insert(goal: SavingsGoal): String {
        val ref = goals(goal.userId).document()
        val createdAt = if (goal.createdAt == 0L) System.currentTimeMillis() else goal.createdAt
        ref.set(goal.copy(id = ref.id, createdAt = createdAt)).await()
        return ref.id
    }

    suspend fun update(goal: SavingsGoal) {
        goals(goal.userId).document(goal.id).set(goal).await()
    }

    suspend fun delete(goal: SavingsGoal) {
        goals(goal.userId).document(goal.id).delete().await()
    }

    //active goals first, newest first within each group
    suspend fun getAll(userId: String): List<SavingsGoal> {
        return goals(userId).get().await().toObjects(SavingsGoal::class.java)
            .sortedWith(compareBy<SavingsGoal> { it.completed }.thenByDescending { it.createdAt })
    }

    suspend fun countCompleted(userId: String): Int {
        return goals(userId).whereEqualTo("completed", true).get().await().size()
    }
}

class NetWorthDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private fun items(userId: String) = col(userId, "net_worth_items")
    private fun snapshots(userId: String) = col(userId, "net_worth_snapshots")

    suspend fun insertItem(item: NetWorthItem): String {
        val ref = items(item.userId).document()
        ref.set(item.copy(id = ref.id)).await()
        return ref.id
    }

    suspend fun updateItem(item: NetWorthItem) {
        items(item.userId).document(item.id).set(item).await()
    }

    suspend fun deleteItem(item: NetWorthItem) {
        items(item.userId).document(item.id).delete().await()
    }

    suspend fun getItems(userId: String): List<NetWorthItem> {
        return items(userId).get().await().toObjects(NetWorthItem::class.java)
            .sortedWith(compareBy<NetWorthItem>({ it.kind }, { it.groupName }, { it.name }))
    }

    //one snapshot per day: the document id is the day's start time
    suspend fun insertSnapshot(snapshot: NetWorthSnapshot): String {
        val id = snapshot.date.toString()
        snapshots(snapshot.userId).document(id).set(snapshot.copy(id = id)).await()
        return id
    }

    suspend fun updateSnapshot(snapshot: NetWorthSnapshot) {
        insertSnapshot(snapshot)
    }

    suspend fun getSnapshotForDay(userId: String, date: Long): NetWorthSnapshot? {
        return snapshots(userId).document(date.toString()).get().await()
            .toObject(NetWorthSnapshot::class.java)
    }

    suspend fun getSnapshots(userId: String): List<NetWorthSnapshot> {
        return snapshots(userId).orderBy("date", Query.Direction.ASCENDING)
            .get().await().toObjects(NetWorthSnapshot::class.java)
    }
}

class AchievementDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private fun unlocked(userId: String) = col(userId, "unlocked_achievements")

    //the document id is the achievement key, so the same achievement can't be saved twice
    suspend fun insert(achievement: UnlockedAchievement): String {
        val id = achievement.achievementKey
        unlocked(achievement.userId).document(id).set(achievement.copy(id = id)).await()
        return id
    }

    suspend fun getAll(userId: String): List<UnlockedAchievement> {
        return unlocked(userId).orderBy("unlockedAt", Query.Direction.ASCENDING)
            .get().await().toObjects(UnlockedAchievement::class.java)
    }
}

class OwnedItemDao(firestore: FirebaseFirestore) : FirestoreDao(firestore) {
    private fun owned(userId: String) = col(userId, "owned_items")

    //the document id is the item key - each cosmetic can only be owned once
    suspend fun insert(item: OwnedItem): String {
        val id = item.itemKey
        owned(item.userId).document(id).set(item.copy(id = id)).await()
        return id
    }

    suspend fun update(item: OwnedItem) {
        owned(item.userId).document(item.itemKey).set(item.copy(id = item.itemKey)).await()
    }

    suspend fun getAll(userId: String): List<OwnedItem> {
        return owned(userId).get().await().toObjects(OwnedItem::class.java)
    }
}
