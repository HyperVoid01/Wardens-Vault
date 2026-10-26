package com.soo.wardensvault

import android.content.Context

enum class SpendStatus { SAFE, APPROACHING, EXCEEDED }

//Safe below 75% of the limit, approaching from 75%, exceeded once spending passes the limit
fun spendStatus(spent: Double, limit: Double): SpendStatus = when {
    limit <= 0.0 -> SpendStatus.SAFE
    spent > limit -> SpendStatus.EXCEEDED
    spent >= limit * 0.75 -> SpendStatus.APPROACHING
    else -> SpendStatus.SAFE
}

//severity: 1 = heads-up, 2 = serious
data class Alert(
    val key: String,
    val settingKey: String,
    val title: String,
    val message: String,
    val severity: Int
)

/*
Works out the current budget / bill / cash flow alerts. The Home screen lists them and
notifyNew() also sends each one as a notification (once, thanks to the unique key).
 */
object AlertEngine {

    suspend fun compute(context: Context, userId: String): List<Alert> {
        val db = AppDatabase.getDatabase(context)
        val alerts = mutableListOf<Alert>()
        val now = System.currentTimeMillis()
        val month = DateUtils.monthKey(now)
        val from = DateUtils.startOfMonth(now)
        val to = DateUtils.endOfToday()

        //overall monthly budget
        val goal = db.budgetGoalDao().getGoalForUser(userId)
        val spent = db.expenseDao().getTotalSpentForPeriod(userId, from, to)
        if (goal != null && goal.maxGoal > 0) {
            val pct = spent / goal.maxGoal * 100
            val pctText = String.format("%.0f", pct)
            when {
                pct > 100 -> alerts.add(
                    Alert("budget_over_$month", AppSettings.KEY_BUDGET, "Monthly budget exceeded",
                        "You have spent $pctText% of your monthly budget.", 2)
                )
                pct >= 90 -> alerts.add(
                    Alert("budget_90_$month", AppSettings.KEY_BUDGET, "Monthly budget almost used",
                        "You have used $pctText% of your monthly budget.", 2)
                )
                pct >= 75 -> alerts.add(
                    Alert("budget_75_$month", AppSettings.KEY_BUDGET, "Approaching monthly budget",
                        "You have used $pctText% of your monthly budget.", 1)
                )
            }
        }

        //category limits
        val limits = db.categoryLimitDao().getAll(userId)
        if (limits.isNotEmpty()) {
            val totals = db.expenseDao().getCategoryTotalsForPeriod(userId, from, to)
                .associateBy { it.categoryId }
            for (limit in limits) {
                val total = totals[limit.categoryId] ?: continue
                val name = total.categoryName
                when (spendStatus(total.total, limit.limitAmount)) {
                    SpendStatus.EXCEEDED -> alerts.add(
                        Alert("cat_over_${limit.categoryId}_$month", AppSettings.KEY_BUDGET,
                            "$name limit exceeded",
                            "R${fmt(total.total)} spent of your R${fmt(limit.limitAmount)} $name limit.", 2)
                    )
                    SpendStatus.APPROACHING -> alerts.add(
                        Alert("cat_warn_${limit.categoryId}_$month", AppSettings.KEY_BUDGET,
                            "Approaching $name limit",
                            "R${fmt(total.total)} spent of your R${fmt(limit.limitAmount)} $name limit.", 1)
                    )
                    SpendStatus.SAFE -> {}
                }
            }
        }

        //upcoming and overdue bills
        val today = DateUtils.startOfToday()
        for (bill in db.billDao().getAll(userId)) {
            if (bill.isPaid) continue
            val days = DateUtils.daysBetween(today, bill.dueDate)
            val enemy = EnemyCatalog.forBill(bill.name).displayName
            when {
                days < 0 -> alerts.add(
                    Alert("bill_overdue_${bill.id}_${bill.dueDate}", AppSettings.KEY_BILLS,
                        "Bill overdue", "$enemy is overdue (R${fmt(BillManager.remaining(bill))}).", 2)
                )
                days == 0 -> alerts.add(
                    Alert("bill_today_${bill.id}_${bill.dueDate}", AppSettings.KEY_BILLS,
                        "Bill due today", "$enemy is due today (R${fmt(BillManager.remaining(bill))}).", 2)
                )
                days <= 3 -> alerts.add(
                    Alert("bill_soon_${bill.id}_${bill.dueDate}", AppSettings.KEY_BILLS,
                        "Upcoming bill", "$enemy is due in $days day(s) (R${fmt(BillManager.remaining(bill))}).", 1)
                )
            }
        }

        //cash flow
        val forecast = Forecaster.forecast(context, userId, 14)
        if (forecast.isNegative) {
            alerts.add(
                Alert("cashflow_neg_$month", AppSettings.KEY_CASHFLOW, "Cash flow warning",
                    "Your estimated balance in 14 days is -R${fmt(-forecast.projectedBalance)}.", 2)
            )
        } else if (forecast.isLow) {
            alerts.add(
                Alert("cashflow_low_$month", AppSettings.KEY_CASHFLOW, "Cash flow warning",
                    "Your estimated balance in 14 days is only R${fmt(forecast.projectedBalance)}.", 1)
            )
        }
        return alerts
    }

    //sends a notification for every alert that hasn't been sent before; returns all current alerts
    suspend fun notifyNew(context: Context, userId: String): List<Alert> {
        val alerts = compute(context, userId)
        val prefs = AppSettings.prefs(context)
        val prefKey = "notified_$userId"
        val sent = (prefs.getStringSet(prefKey, emptySet()) ?: emptySet()).toMutableSet()
        var changed = false
        for (alert in alerts) {
            if (alert.key in sent) continue
            val posted = NotificationHelper.notify(
                context, alert.settingKey, alert.key.hashCode(), alert.title, alert.message
            )
            if (posted) {
                sent.add(alert.key)
                changed = true
            }
        }
        if (changed) prefs.edit().putStringSet(prefKey, sent).apply()
        return alerts
    }

    private fun fmt(value: Double): String = String.format("%.2f", value)
}
