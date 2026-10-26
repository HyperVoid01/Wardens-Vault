package com.soo.wardensvault

import android.graphics.Color
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
Daily spending graph for a week, a month or a custom range, with the categories
colour-coded and a short summary of the patterns (busiest day, weekends, top category).
 */
class SpendingGraphActivity : ScreenActivity() {
    private var from: Long = DateUtils.startOfDay(System.currentTimeMillis() - 6 * 86400000L)
    private var to: Long = DateUtils.endOfToday()
    private val dayLabel = SimpleDateFormat("d MMM", Locale.getDefault())
    private val palette = listOf(
        "#EF5350", "#42A5F5", "#66BB6A", "#FFCA28", "#AB47BC",
        "#26C6DA", "#FF7043", "#8D6E63", "#EC407A", "#9CCC65"
    ).map { Color.parseColor(it) }

    override suspend fun render() {
        addTitle("Daily Spending")

        val bar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        bar.addView(periodButton("Weekly") {
            from = DateUtils.startOfDay(System.currentTimeMillis() - 6 * 86400000L)
            to = DateUtils.endOfToday()
            refresh()
        })
        bar.addView(periodButton("Monthly") {
            from = DateUtils.startOfCurrentMonth()
            to = DateUtils.endOfMonth(System.currentTimeMillis())
            refresh()
        })
        bar.addView(periodButton("Custom") { showCustomForm() })
        content.addView(bar)

        addText("${dateFormat.format(from)} - ${dateFormat.format(to)}", UiKit.MUTED, 14f)

        //cap very long ranges so the chart stays readable
        val maxDays = 62L
        var start = from
        if ((to - from) / 86400000L > maxDays) {
            start = DateUtils.startOfDay(to - (maxDays - 1) * 86400000L)
            addText("Showing the last $maxDays days of the selected range.", UiKit.ORANGE, 12f)
        }

        val expenses = db.expenseDao().getExpensesWithCategoryForPeriod(userId, start, to)
        if (expenses.isEmpty()) {
            addText("No expenses in this period.", Color.WHITE)
            return
        }

        //spending per day, split by category
        val perDay = LinkedHashMap<Long, MutableMap<String, Double>>()
        var cursor = DateUtils.startOfDay(start)
        while (cursor <= to) {
            perDay[cursor] = LinkedHashMap()
            val cal = Calendar.getInstance().apply { timeInMillis = cursor }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cursor = cal.timeInMillis
        }
        val categoryTotals = LinkedHashMap<String, Double>()
        for (expense in expenses) {
            val day = DateUtils.startOfDay(expense.date)
            val parts = perDay[day] ?: continue
            parts[expense.categoryName] = (parts[expense.categoryName] ?: 0.0) + expense.amount
            categoryTotals[expense.categoryName] = (categoryTotals[expense.categoryName] ?: 0.0) + expense.amount
        }

        val colors = HashMap<String, Int>()
        for ((index, name) in categoryTotals.keys.withIndex()) colors[name] = palette[index % palette.size]

        val days = perDay.map { (day, parts) -> DaySpend(dayLabel.format(day), parts) }
        val chart = SpendingChartView(this)
        chart.setData(days, colors)
        content.addView(chart)

        //legend
        val legend = addCard()
        for ((name, total) in categoryTotals.entries.sortedByDescending { it.value }) {
            legend.addLine("■ $name: ${money(total)}", colors[name] ?: Color.WHITE, 14f, true)
        }

        //patterns
        val total = days.sumOf { it.total }
        val busiest = perDay.maxByOrNull { it.value.values.sum() }
        var weekendTotal = 0.0
        var weekendDays = 0
        var weekdayTotal = 0.0
        var weekdayDays = 0
        for ((day, parts) in perDay) {
            val dow = Calendar.getInstance().apply { timeInMillis = day }.get(Calendar.DAY_OF_WEEK)
            val spent = parts.values.sum()
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                weekendTotal += spent
                weekendDays++
            } else {
                weekdayTotal += spent
                weekdayDays++
            }
        }
        addHeader("Patterns")
        val insights = addCard()
        insights.addLine("Total spent: ${money(total)}", Color.WHITE, 15f, true)
        insights.addLine("Daily average: ${money(total / days.size)}", Color.WHITE, 14f)
        if (busiest != null) {
            insights.addLine(
                "Highest day: ${dateFormat.format(busiest.key)} (${money(busiest.value.values.sum())})",
                Color.WHITE, 14f
            )
        }
        val topCategory = categoryTotals.maxByOrNull { it.value }
        if (topCategory != null) {
            insights.addLine("Biggest category: ${topCategory.key} (${money(topCategory.value)})", Color.WHITE, 14f)
        }
        if (weekendDays > 0 && weekdayDays > 0) {
            val weekendAvg = weekendTotal / weekendDays
            val weekdayAvg = weekdayTotal / weekdayDays
            val note = if (weekendAvg > weekdayAvg * 1.2) {
                "You spend more on weekends (${money(weekendAvg)}/day vs ${money(weekdayAvg)}/day)."
            } else if (weekdayAvg > weekendAvg * 1.2) {
                "You spend more on weekdays (${money(weekdayAvg)}/day vs ${money(weekendAvg)}/day)."
            } else {
                "Weekend and weekday spending are similar."
            }
            insights.addLine(note, UiKit.GOLD, 14f)
        }
    }

    private fun periodButton(label: String, onClick: () -> Unit): android.widget.Button {
        return UiKit.button(this, label, onClick).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
    }

    private fun showCustomForm() {
        val fields = listOf(
            FormField("from", "Start date", FormField.Kind.DATE, from.toString()),
            FormField("to", "End date", FormField.Kind.DATE, to.toString())
        )
        showForm("Custom range", fields, "Show") { v ->
            val start = v["from"]?.toLongOrNull()
            val end = v["to"]?.toLongOrNull()
            if (start == null || end == null) return@showForm "Pick both dates"
            if (start > end) return@showForm "Start date must be before the end date"
            from = DateUtils.startOfDay(start)
            to = DateUtils.endOfDay(end)
            null
        }
    }
}
