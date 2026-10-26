package com.soo.wardensvault

import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

//Income tracking: add, edit, delete and view income history
class IncomeActivity : ScreenActivity() {
    private val types = listOf("Salary", "Freelance", "Bonus", "Other")

    override suspend fun render() {
        addTitle("Income")
        val now = System.currentTimeMillis()
        val thisMonth = db.incomeDao().totalForPeriod(userId, DateUtils.startOfMonth(now), DateUtils.endOfMonth(now))
        addText("Received this month: ${money(thisMonth)}", UiKit.GREEN, 18f, true)
        addButton("+ Add Income") { showIncomeForm(null) }

        val incomes = db.incomeDao().getAll(userId)
        if (incomes.isEmpty()) {
            addText("No income recorded yet.", Color.WHITE)
            return
        }
        addHeader("Income history")
        for (income in incomes) {
            val card = addCard()
            card.addLine("${money(income.amount)}  -  ${income.type}", Color.WHITE, 17f, true)
            card.addLine("${income.source}  -  ${dateFormat.format(income.date)}", UiKit.MUTED, 13f)
            card.addLine("Tap to edit or delete", UiKit.MUTED, 11f)
            card.setOnClickListener {
                showOptions(income.source, listOf("Edit", "Delete")) { which ->
                    if (which == 0) {
                        showIncomeForm(income)
                    } else {
                        confirm("Delete income", "Delete this income entry?") {
                            lifecycleScope.launch {
                                db.incomeDao().delete(income)
                                refresh()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showIncomeForm(existing: Income?) {
        val fields = listOf(
            FormField("amount", "Amount", FormField.Kind.NUMBER, existing?.amount?.toString() ?: ""),
            FormField("source", "Source (e.g. Employer name)", FormField.Kind.TEXT, existing?.source ?: ""),
            FormField("type", "Income type", FormField.Kind.CHOICE, existing?.type ?: "Salary", types),
            FormField(
                "date", "Date", FormField.Kind.DATE,
                (existing?.date ?: DateUtils.startOfToday()).toString()
            )
        )
        showForm(if (existing == null) "Add income" else "Edit income", fields) { v ->
            val amount = parseAmount(v["amount"])
            val source = v["source"].orEmpty()
            val date = v["date"]?.toLongOrNull()
            if (amount == null || amount <= 0.0) return@showForm "Enter a valid amount greater than 0"
            if (source.isEmpty()) return@showForm "Enter the income source"
            if (date == null) return@showForm "Pick a date"
            val type = v["type"] ?: "Other"
            if (existing == null) {
                db.incomeDao().insert(Income(userId = userId, amount = amount, source = source, type = type, date = date))
            } else {
                db.incomeDao().update(existing.copy(amount = amount, source = source, type = type, date = date))
            }
            announce(AchievementManager.checkAll(this@IncomeActivity, userId))
            null
        }
    }
}
