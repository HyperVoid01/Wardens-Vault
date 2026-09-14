package com.soo.wardensvault

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.Locale

class BudgetGoalActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText
    private lateinit var tvSpentThisMonth: TextView
    private lateinit var tvGoalStatus: TextView
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget_goal)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userId = SessionManager.getLoggedInUserId(this)
        if (userId == -1) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        etMinGoal = findViewById(R.id.etMinGoal)
        etMaxGoal = findViewById(R.id.etMaxGoal)
        tvSpentThisMonth = findViewById(R.id.tvSpentThisMonth)
        tvGoalStatus = findViewById(R.id.tvGoalStatus)

        findViewById<Button>(R.id.btnSaveGoal).setOnClickListener {
            saveGoal()
        }

        loadExistingGoalAndStatus()
    }

    private fun loadExistingGoalAndStatus() {
        lifecycleScope.launch {
            val goal = viewModel.getGoal(userId)
            if (goal != null) {
                etMinGoal.setText(formatAmount(goal.minGoal))
                etMaxGoal.setText(formatAmount(goal.maxGoal))
            }
            refreshMonthStatus(goal)
        }
    }

    private fun saveGoal() {
        val minText = etMinGoal.text.toString().trim()
        val maxText = etMaxGoal.text.toString().trim()

        val min = minText.toDoubleOrNull()
        val max = maxText.toDoubleOrNull()

        if (min == null || max == null) {
            Toast.makeText(this, "Enter both a minimum and maximum goal", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (min < 0 || max < 0) {
            Toast.makeText(this, "Goals can't be negative", Toast.LENGTH_SHORT).show()
            return
        }
        if (min > max) {
            Toast.makeText(this, "Minimum goal can't be greater than maximum", Toast.LENGTH_SHORT)
                .show()
            return
        }

        lifecycleScope.launch {
            viewModel.setGoal(userId, min, max)
            Toast.makeText(this@BudgetGoalActivity, "Goal saved", Toast.LENGTH_SHORT).show()
            refreshMonthStatus(BudgetGoal(userId = userId, minGoal = min, maxGoal = max))
        }
    }

    //shows how much has been spent this calendar month so far, and whether that
    //sits below, within, or above the saved min/max goal
    private suspend fun refreshMonthStatus(goal: BudgetGoal?) {
        val from = DateUtils.startOfCurrentMonth()
        val to = DateUtils.endOfToday()
        val spent = viewModel.getTotalSpentForPeriod(userId, from, to)
        tvSpentThisMonth.text = "Spent: R${formatAmount(spent)}"

        tvGoalStatus.text = when {
            goal == null -> "Set a goal above to track this month's progress"
            spent < goal.minGoal -> "Below your minimum goal (${formatAmount(goal.minGoal)})"
            spent > goal.maxGoal -> "Over your maximum goal (${formatAmount(goal.maxGoal)})"
            else -> "Within your goal range (${formatAmount(goal.minGoal)} - " +
                "${formatAmount(goal.maxGoal)})"
        }
    }

    private fun formatAmount(amount: Double): String {
        return String.format(Locale.getDefault(), "%.2f", amount)
    }
}
