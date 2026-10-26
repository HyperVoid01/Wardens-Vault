package com.soo.wardensvault

import android.content.Context

/*
Bill / enemy rules.
 - A bill's enemy is alive until the bill is paid for the current cycle.
 - Paying on or before the due date defeats the enemy; it returns after the due date passes.
 - Paying late settles the overdue cycle and the enemy immediately returns for the next one.
 - Paying also saves a normal expense (description "Bill - <name>") so budgets stay accurate.
 */
object BillManager {

    //brings enemies back to life for the next billing cycle once their due date has passed
    suspend fun rollover(context: Context, userId: String) {
        val dao = AppDatabase.getDatabase(context).billDao()
        val today = DateUtils.startOfToday()
        for (bill in dao.getAll(userId)) {
            if (bill.isPaid && bill.dueDate < today) {
                var next = bill.dueDate
                while (next < today) next = DateUtils.addFrequency(next, bill.frequency)
                dao.update(bill.copy(dueDate = next, isPaid = false))
            }
        }
    }

    data class PayResult(val award: GamificationManager.AwardResult, val early: Boolean)

    suspend fun payBill(context: Context, userId: String, bill: Bill, amount: Double): PayResult {
        val db = AppDatabase.getDatabase(context)
        val now = System.currentTimeMillis()
        val today = DateUtils.startOfDay(now)
        val early = today <= bill.dueDate

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
        db.billPaymentDao().insert(
            BillPayment(userId = userId, billName = bill.name, amount = amount, paidDate = now)
        )

        val updated = if (early) {
            bill.copy(isPaid = true)
        } else {
            var next = bill.dueDate
            while (next < today) next = DateUtils.addFrequency(next, bill.frequency)
            bill.copy(dueDate = next, isPaid = false)
        }
        db.billDao().update(updated)

        val gold = GamificationManager.BILL_GOLD +
            (if (early) GamificationManager.BILL_EARLY_BONUS_GOLD else 0)
        val award = GamificationManager(context).award(userId, GamificationManager.BILL_XP, gold)
        return PayResult(award, early)
    }
}
