package com.soo.wardensvault

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userId = SessionManager.getLoggedInUserId(this)
        if (userId == -1) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]

        val fullName = intent.getStringExtra("fullName") ?: SessionManager.getLoggedInFullName(this)
        findViewById<TextView>(R.id.tvWelcome).text = "Good Morning,\n$fullName"

        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_bedroom -> true // already home
                R.id.nav_map -> {
                    startActivity(Intent(this, MapMenuActivity::class.java))
                    true
                }
                R.id.nav_helmet -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (userId != -1) loadBudgetStatus()
    }

    //shows this month's spending as both a progress bar (against the max goal) and
    //a "Gold" figure - remaining budget if a goal is set, otherwise total spent so far
    private fun loadBudgetStatus() {
        lifecycleScope.launch {
            val from = DateUtils.startOfCurrentMonth()
            val to = DateUtils.endOfToday()
            val spent = viewModel.getTotalSpentForPeriod(userId, from, to)
            val goal = viewModel.getGoal(userId)

            val progressBar = findViewById<ProgressBar>(R.id.pbBudgetProgress)
            val tvGold = findViewById<TextView>(R.id.tvGold)

            if (goal != null && goal.maxGoal > 0) {
                val percent = ((spent / goal.maxGoal) * 100).toInt().coerceIn(0, 100)
                progressBar.progress = percent
                val remaining = goal.maxGoal - spent
                tvGold.text = "Gold: ${String.format("%.0f", remaining.coerceAtLeast(0.0))}"
            } else {
                progressBar.progress = 0
                tvGold.text = "Gold: ${String.format("%.0f", spent)}"
            }
        }
    }
}
