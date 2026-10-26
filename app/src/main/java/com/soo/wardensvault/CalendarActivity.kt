package com.soo.wardensvault

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
Calendar view of expenses. Days that have expenses are highlighted; tapping a day lists
what was spent on it. "View a date range" opens the expense history for a start/end period.
 */
class CalendarActivity : ScreenActivity() {
    private val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private var shownMonth: Long = DateUtils.startOfCurrentMonth()
    private var selectedDay: Long? = null

    override suspend fun render() {
        addTitle("Calendar")

        //month navigation
        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        nav.addView(UiKit.button(this, "<") { changeMonth(-1) }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        nav.addView(UiKit.text(this, monthFormat.format(shownMonth), Color.WHITE, 18f, true).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        })
        nav.addView(UiKit.button(this, ">") { changeMonth(1) }.apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        content.addView(nav)

        val monthStart = DateUtils.startOfMonth(shownMonth)
        val monthEnd = DateUtils.endOfMonth(shownMonth)
        val expenses = db.expenseDao().getExpensesWithCategoryForPeriod(userId, monthStart, monthEnd)

        //days of this month that have at least one expense
        val daysWithExpenses = HashSet<Int>()
        for (expense in expenses) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = expense.date
            daysWithExpenses.add(cal.get(Calendar.DAY_OF_MONTH))
        }

        //weekday header
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        for (label in listOf("S", "M", "T", "W", "T", "F", "S")) {
            header.addView(cell(label, false, false, UiKit.GOLD, null))
        }
        content.addView(header)

        //day grid
        val first = Calendar.getInstance().apply { timeInMillis = monthStart }
        val offset = first.get(Calendar.DAY_OF_WEEK) - 1   // Sunday = 0
        val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        var day = 1 - offset
        while (day <= daysInMonth) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            for (column in 0 until 7) {
                if (day < 1 || day > daysInMonth) {
                    row.addView(cell("", false, false, Color.WHITE, null))
                } else {
                    val dayStart = Calendar.getInstance().apply {
                        timeInMillis = monthStart
                        set(Calendar.DAY_OF_MONTH, day)
                    }.timeInMillis
                    val isSelected = selectedDay == dayStart
                    row.addView(
                        cell(day.toString(), day in daysWithExpenses, isSelected, Color.WHITE) {
                            selectedDay = dayStart
                            refresh()
                        }
                    )
                }
                day++
            }
            content.addView(row)
        }

        addText("Highlighted days have expenses. Tap a day to see them.", UiKit.MUTED, 12f)

        //expenses of the selected day
        val selected = selectedDay
        if (selected != null) {
            addHeader("Expenses on ${dateFormat.format(selected)}")
            val dayExpenses = expenses.filter { DateUtils.startOfDay(it.date) == selected }
            if (dayExpenses.isEmpty()) {
                addText("Nothing spent on this day.", Color.WHITE)
            } else {
                val card = addCard()
                for (expense in dayExpenses) {
                    card.addLine(
                        "${expense.categoryName}: ${expense.description} - ${money(expense.amount)}",
                        Color.WHITE, 15f
                    )
                }
                card.addLine("Total: ${money(dayExpenses.sumOf { it.amount })}", UiKit.GOLD, 15f, true)
            }
        }

        addButton("View a date range") { goTo(ExpenseListActivity::class.java) }
    }

    private fun changeMonth(delta: Int) {
        val cal = Calendar.getInstance().apply { timeInMillis = shownMonth }
        cal.add(Calendar.MONTH, delta)
        shownMonth = DateUtils.startOfMonth(cal.timeInMillis)
        selectedDay = null
        refresh()
    }

    //one cell of the grid; highlighted = has expenses, selected = currently chosen day
    private fun cell(
        label: String,
        highlighted: Boolean,
        selected: Boolean,
        textColor: Int,
        onClick: (() -> Unit)?
    ): View {
        val tv = TextView(this)
        tv.text = label
        tv.gravity = Gravity.CENTER
        tv.setTextColor(if (highlighted) Color.BLACK else textColor)
        val size = UiKit.dp(this, 44)
        tv.layoutParams = LinearLayout.LayoutParams(0, size, 1f).apply {
            val m = UiKit.dp(this@CalendarActivity, 2)
            setMargins(m, m, m, m)
        }
        if (highlighted || selected) {
            tv.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (highlighted) UiKit.GOLD else Color.TRANSPARENT)
                if (selected) setStroke(UiKit.dp(this@CalendarActivity, 3), Color.WHITE)
            }
        }
        if (onClick != null) tv.setOnClickListener { onClick() }
        return tv
    }
}
