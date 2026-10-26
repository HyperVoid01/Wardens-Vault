package com.soo.wardensvault

import android.content.Context

data class ForecastResult(
    val days: Int,
    val currentBalance: Double,
    val expectedIncome: Double,
    val billsDue: Double,
    val expectedSpending: Double,
    val projectedBalance: Double,
    val isNegative: Boolean,
    val isLow: Boolean
)

/*
Cash flow forecast.
  Current balance   = assets entered as Cash or Savings on the Net Worth screen
  Expected income   = average daily income over the last 90 days x forecast days
  Bills due         = unpaid recurring bills (and repeats) falling inside the forecast window
  Expected spending = average daily non-bill spending over the last 30 days x forecast days
                      (falls back to monthly budget / 30 when there is no spending history)
 */
object Forecaster {
    const val LOW_BALANCE = 500.0

    suspend fun forecast(context: Context, userId: String, days: Int): ForecastResult {
        val db = AppDatabase.getDatabase(context)
        val now = System.currentTimeMillis()
        val today = DateUtils.startOfDay(now)
        val end = DateUtils.endOfDay(today + days * 86400000L)

        val items = db.netWorthDao().getItems(userId)
        val balance = items
            .filter { it.kind == NetWorthManager.ASSET && (it.groupName == "Cash" || it.groupName == "Savings") }
            .sumOf { it.amount }

        val income90 = db.incomeDao().totalForPeriod(userId, today - 90 * 86400000L, DateUtils.endOfToday())
        val expectedIncome = income90 / 90.0 * days

        var billsDue = 0.0
        for (bill in db.billDao().getAll(userId)) {
            var due = bill.dueDate
            var skipFirst = bill.isPaid   // this cycle is already paid
            var first = true
            while (due <= end) {
                //the current cycle only counts what is still owed after partial payments
                val owed = if (first) BillManager.remaining(bill) else bill.amount
                if (due >= today && !skipFirst) billsDue += owed
                skipFirst = false
                first = false
                due = DateUtils.addFrequency(due, bill.frequency)
            }
        }

        var dailySpend = db.expenseDao().getRegularSpendForPeriod(userId, today - 30 * 86400000L, DateUtils.endOfToday()) / 30.0
        if (dailySpend <= 0.0) {
            val goal = db.budgetGoalDao().getGoalForUser(userId)
            if (goal != null) dailySpend = goal.maxGoal / 30.0
        }
        val expectedSpending = dailySpend * days

        val projected = balance + expectedIncome - billsDue - expectedSpending
        return ForecastResult(
            days, balance, expectedIncome, billsDue, expectedSpending, projected,
            projected < 0, projected in 0.0..LOW_BALANCE
        )
    }
}
