package com.soo.wardensvault

import android.content.Intent
import android.text.InputType
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/*
Initial user setup shown after the first login. Collects monthly income, current savings and
the monthly budget. Everything is optional and can be skipped; categories, limits, bills and
goals can be added later from the Map.
 */
class SetupActivity : ScreenActivity() {
    private lateinit var etIncome: EditText
    private lateinit var etSavings: EditText
    private lateinit var etBudget: EditText

    //first-time setup happens before the main app, so no bottom navigation yet
    override val showBottomNav: Boolean
        get() = false

    override suspend fun render() {
        addTitle("Welcome, Warden!")
        addText(
            "Let's set up your vault. All of this is optional and you can change it later.",
            UiKit.MUTED, 14f
        )

        val numbers = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        etIncome = addLabeledInput("Monthly income (R)", numbers)
        etSavings = addLabeledInput("Savings you have now (R)", numbers)
        etBudget = addLabeledInput("Monthly spending budget (R)", numbers)

        addText(
            "Default categories (Groceries, Transport, ...) are created for you. " +
                "Add category limits, recurring bills and savings goals from the Map whenever you like.",
            UiKit.MUTED, 12f
        )
        addButton("Save and continue") { save() }
        addButton("Skip for now") { finishSetup() }
    }

    private fun save() {
        val incomeText = etIncome.text.toString().trim()
        val savingsText = etSavings.text.toString().trim()
        val budgetText = etBudget.text.toString().trim()

        val income = if (incomeText.isEmpty()) 0.0 else parseAmount(incomeText)
        val savings = if (savingsText.isEmpty()) 0.0 else parseAmount(savingsText)
        val budget = if (budgetText.isEmpty()) 0.0 else parseAmount(budgetText)
        if (income == null || savings == null || budget == null) {
            toast("Enter valid amounts (numbers that are not negative)")
            return
        }

        launchSafe {
            if (income > 0) {
                db.incomeDao().insert(
                    Income(
                        userId = userId, amount = income, source = "Monthly income",
                        type = "Salary", date = DateUtils.startOfToday()
                    )
                )
            }
            if (savings > 0) {
                db.netWorthDao().insertItem(
                    NetWorthItem(
                        userId = userId, name = "Initial savings", kind = NetWorthManager.ASSET,
                        groupName = "Savings", amount = savings
                    )
                )
            }
            if (budget > 0) {
                val existing = db.budgetGoalDao().getGoalForUser(userId)
                db.budgetGoalDao().setGoal(
                    BudgetGoal(id = existing?.id ?: "", userId = userId, minGoal = 0.0, maxGoal = budget)
                )
            }
            announce(AchievementManager.checkAll(this@SetupActivity, userId))
            finishSetup()
        }
    }

    private fun finishSetup() {
        AppSettings.setFlag(this, "setup_done_$userId")
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
