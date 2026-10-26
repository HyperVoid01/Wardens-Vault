package com.soo.wardensvault

import android.content.Context

/*
Bill / enemy rules.
 - A bill's enemy is alive until the full amount for the current cycle has been paid.
 - Paying LESS than what is left only wounds the enemy (its health bar drops); the payment is
   still saved as an expense and counts towards the bill.
 - Paying the rest on or before the due date defeats the enemy; it returns after the due date passes.
 - Paying the rest late settles the overdue cycle and the enemy immediately returns for the next one.
 - Every payment saves a normal expense (description "Bill - <name>") so budgets stay accurate.
 */
object BillManager {

    //what is still owed for the current cycle
    fun remaining(bill: Bill): Double =
        if (bill.isPaid) 0.0 else (bill.amount - bill.paidThisCycle).coerceAtLeast(0.0)

    //enemy health from 0 (defeated) to 1 (nothing paid yet)
    fun health(bill: Bill): Double =
        if (bill.amount <= 0.0) 0.0 else (remaining(bill) / bill.amount).coerceIn(0.0, 1.0)

    //brings enemies back to life for the next billing cycle once their due date has passed
    suspend fun rollover(context: Context, userId: String) {
        val dao = AppDatabase.getDatabase(context).billDao()
        val today = DateUtils.startOfToday()
        for (bill in dao.getAll(userId)) {
            if (bill.isPaid && bill.dueDate < today) {
                var next = bill.dueDate
                while (next < today) next = DateUtils.addFrequency(next, bill.frequency)
                dao.update(bill.copy(dueDate = next, isPaid = false, paidThisCycle = 0.0))
            }
        }
    }

    /*
    award is null when the enemy was only wounded (no reward until it is defeated).
    healthBefore / healthAfter (0..1) drive the health bar animation.
     */
    data class PayResult(
        val award: GamificationManager.AwardResult?,
        val early: Boolean,
        val defeated: Boolean,
        val stillOwed: Double,
        val healthBefore: Double,
        val healthAfter: Double
    )

    suspend fun payBill(context: Context, userId: String, bill: Bill, amount: Double): PayResult {
        val db = AppDatabase.getDatabase(context)
        val now = System.currentTimeMillis()
        val today = DateUtils.startOfDay(now)
        val early = today <= bill.dueDate
        val healthBefore = health(bill)

        db.expenseDao().insertExpense(
            Expense(
                userId = userId,
                categoryId = bill.categoryId,
                amount = amount,
                date = today,
                startTime = now,
                endTime = now,
                description = "Bill - ${bill.name}"
            )
        )

        val paidSoFar = bill.paidThisCycle + amount
        //not enough yet: the enemy is wounded but survives
        if (paidSoFar + 0.005 < bill.amount) {
            val wounded = bill.copy(paidThisCycle = paidSoFar)
            db.billDao().update(wounded)
            return PayResult(null, early, false, remaining(wounded), healthBefore, health(wounded))
        }

        //paid in full: the enemy is defeated
        db.billPaymentDao().insert(
            BillPayment(userId = userId, billName = bill.name, amount = paidSoFar, paidDate = now)
        )
        val updated = if (early) {
            bill.copy(isPaid = true, paidThisCycle = 0.0)
        } else {
            var next = bill.dueDate
            while (next < today) next = DateUtils.addFrequency(next, bill.frequency)
            bill.copy(dueDate = next, isPaid = false, paidThisCycle = 0.0)
        }
        db.billDao().update(updated)

        val gold = GamificationManager.BILL_GOLD +
            (if (early) GamificationManager.BILL_EARLY_BONUS_GOLD else 0)
        val award = GamificationManager(context).award(userId, GamificationManager.BILL_XP, gold)
        return PayResult(award, early, true, 0.0, healthBefore, 0.0)
    }
}
