package com.soo.wardensvault

import android.graphics.Color

//Cash flow forecast for 7, 14 or 30 days
class ForecastActivity : ScreenActivity() {
    private var days = 14

    override suspend fun render() {
        addTitle("Cash Flow Forecast")
        val f = Forecaster.forecast(this, userId, days)

        val bar = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.HORIZONTAL }
        for (option in listOf(7, 14, 30)) {
            val b = UiKit.button(this, "$option days") {
                days = option
                refresh()
            }
            b.layoutParams = android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
            if (option == days) b.alpha = 1f else b.alpha = 0.6f
            bar.addView(b)
        }
        content.addView(bar)

        val color = when {
            f.isNegative -> UiKit.RED
            f.isLow -> UiKit.ORANGE
            else -> UiKit.GREEN
        }
        val sign = if (f.projectedBalance < 0) "-" else ""
        addText(
            "Your estimated balance in ${f.days} days is $sign${money(Math.abs(f.projectedBalance))}.",
            color, 20f, true
        )
        if (f.isNegative) {
            addText("Warning: you may run short of money. Review your upcoming bills and spending.", UiKit.RED, 15f, true)
        } else if (f.isLow) {
            addText("Warning: your balance is predicted to get very low.", UiKit.ORANGE, 15f, true)
        }

        addHeader("How this is worked out")
        val card = addCard()
        card.addLine("Current balance (Cash + Savings): ${money(f.currentBalance)}", Color.WHITE)
        card.addLine("+ Expected income: ${money(f.expectedIncome)}", UiKit.GREEN)
        card.addLine("- Bills due: ${money(f.billsDue)}", UiKit.ORANGE)
        card.addLine("- Expected spending: ${money(f.expectedSpending)}", UiKit.ORANGE)
        card.addLine("= Estimated balance: $sign${money(Math.abs(f.projectedBalance))}", color, 16f, true)

        addText(
            "Balance comes from the Cash and Savings assets on the Net Worth screen. " +
                "Income is your 90-day average, spending is your 30-day average (or your monthly budget " +
                "if you have no history), and bills are your unpaid recurring bills.",
            UiKit.MUTED, 12f
        )
        addButton("Update my balance (Net Worth)") { goTo(NetWorthActivity::class.java) }
    }
}
