package com.soo.wardensvault

/*
Menu screens opened from the Map. The "hub" extra decides which one:
  market   -> expenses
  treasury -> income & net worth
  castle   -> budgets & goals
  reports  -> analytics
 */
class HubActivity : ScreenActivity() {

    override suspend fun render() {
        when (intent.getStringExtra("hub")) {
            "treasury" -> {
                addTitle("Treasury")
                addBanner(R.drawable.scene_treasury)
                addText("Income & Net Worth", UiKit.MUTED)
                addButton("Income") { goTo(IncomeActivity::class.java) }
                addButton("Net Worth") { goTo(NetWorthActivity::class.java) }
                addButton("Cash Flow Forecast") { goTo(ForecastActivity::class.java) }
            }
            "castle" -> {
                addTitle("Castle")
                addBanner(R.drawable.scene_castle)
                addText("Budgets & Goals", UiKit.MUTED)
                addButton("Monthly Budget") { goTo(BudgetGoalActivity::class.java) }
                addButton("Category Spending Limits") { goTo(CategoryLimitsActivity::class.java) }
                addButton("Savings Goals") { goTo(GoalsActivity::class.java) }
            }
            "reports" -> {
                addTitle("Reports")
                addText("Analytics", UiKit.MUTED)
                addButton("Daily Spending Graph") { goTo(SpendingGraphActivity::class.java) }
                addButton("Category Spending Totals") { goTo(CategoryTotalsActivity::class.java) }
                addButton("Expense History") { goTo(ExpenseListActivity::class.java) }
                addButton("Calendar") { goTo(CalendarActivity::class.java) }
            }
            else -> {
                addTitle("Market")
                addBanner(R.drawable.scene_market)
                addText("Expenses", UiKit.MUTED)
                addButton("Add Expense") { goTo(CreateExpenseActivity::class.java) }
                addButton("Expense History") { goTo(ExpenseListActivity::class.java) }
                addButton("Calendar") { goTo(CalendarActivity::class.java) }
                addButton("Categories") { goTo(CreateCategoryActivity::class.java) }
            }
        }
    }
}
