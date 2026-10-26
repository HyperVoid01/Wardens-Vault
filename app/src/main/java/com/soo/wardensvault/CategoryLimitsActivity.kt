package com.soo.wardensvault

import android.graphics.Color

//Category spending limits compared with this month's spending (safe / approaching / exceeded)
class CategoryLimitsActivity : ScreenActivity() {

    override suspend fun render() {
        addTitle("Spending Limits")
        addText("Set a monthly limit per category. Tap a category to change its limit.", UiKit.MUTED, 14f)

        val now = System.currentTimeMillis()
        val totals = db.expenseDao().getCategoryTotalsForPeriod(
            userId, DateUtils.startOfMonth(now), DateUtils.endOfToday()
        )
        val limits = db.categoryLimitDao().getAll(userId).associateBy { it.categoryId }

        //overall monthly budget first
        val goal = db.budgetGoalDao().getGoalForUser(userId)
        val spentTotal = db.expenseDao().getTotalSpentForPeriod(userId, DateUtils.startOfMonth(now), DateUtils.endOfToday())
        val overall = addCard()
        overall.addLine("Monthly budget", Color.WHITE, 18f, true)
        if (goal != null && goal.maxGoal > 0) {
            val status = spendStatus(spentTotal, goal.maxGoal)
            overall.addLine("${money(spentTotal)} / ${money(goal.maxGoal)}", Color.WHITE)
            overall.addBar((spentTotal / goal.maxGoal * 100).toInt(), UiKit.statusColor(status))
            overall.addLine(UiKit.statusLabel(status), UiKit.statusColor(status), 14f, true)
        } else {
            overall.addLine("No monthly budget set (Castle > Monthly Budget).", UiKit.MUTED, 13f)
        }

        if (totals.isEmpty()) {
            addText("You have no categories yet. Add some under Market > Categories.", Color.WHITE)
            return
        }

        addHeader("Categories")
        for (total in totals) {
            val limit = limits[total.categoryId]
            val card = addCard()
            card.addLine(total.categoryName, Color.WHITE, 17f, true)
            if (limit == null) {
                card.addLine("Spent ${money(total.total)} - no limit set", UiKit.MUTED, 14f)
            } else {
                val status = spendStatus(total.total, limit.limitAmount)
                card.addLine("${money(total.total)} / ${money(limit.limitAmount)}", Color.WHITE, 15f)
                card.addBar((total.total / limit.limitAmount * 100).toInt(), UiKit.statusColor(status))
                card.addLine(UiKit.statusLabel(status), UiKit.statusColor(status), 14f, true)
            }
            card.setOnClickListener { showLimitForm(total, limit) }
        }
    }

    private fun showLimitForm(total: CategoryTotal, existing: CategoryLimit?) {
        val fields = listOf(
            FormField(
                "limit", "Monthly limit for ${total.categoryName} (leave empty to remove)",
                FormField.Kind.NUMBER, existing?.limitAmount?.toString() ?: ""
            )
        )
        showForm("Spending limit", fields) { v ->
            val text = v["limit"].orEmpty()
            if (text.isEmpty()) {
                db.categoryLimitDao().remove(userId, total.categoryId)
                return@showForm null
            }
            val amount = parseAmount(text) ?: return@showForm "Enter a valid limit (not negative)"
            if (amount == 0.0) {
                db.categoryLimitDao().remove(userId, total.categoryId)
            } else {
                db.categoryLimitDao().upsert(
                    CategoryLimit(
                        id = existing?.id ?: "", userId = userId,
                        categoryId = total.categoryId, limitAmount = amount
                    )
                )
            }
            null
        }
    }
}
