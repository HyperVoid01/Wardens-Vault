package com.soo.wardensvault

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
Home = the financial dashboard: level / XP / Gold / streak, monthly budget progress,
alerts, category progress, upcoming bills, savings goals, net worth and a cash flow forecast.
Opening it also claims the daily login reward and refreshes bills, rewards and alerts.
 */
class HomeActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var gamification: GamificationManager
    private lateinit var db: AppDatabase
    private var userId: String = ""
    private var dashboardJob: Job? = null
    private val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

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
        if (userId.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        gamification = GamificationManager(this)
        db = AppDatabase.getDatabase(this)

        val fullName = intent.getStringExtra("fullName") ?: SessionManager.getLoggedInFullName(this)
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 12 -> "Good Morning"
            hour < 18 -> "Good Afternoon"
            else -> "Good Evening"
        }
        findViewById<TextView>(R.id.tvWelcome).text = "$greeting,\n$fullName"

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

        requestNotificationPermissionOnce()
    }

    override fun onResume() {
        super.onResume()
        if (userId.isNotEmpty()) refreshDashboard()
    }

    //Android 13+ needs the user's permission before notifications can be shown
    private fun requestNotificationPermissionOnce() {
        if (Build.VERSION.SDK_INT < 33) return
        if (AppSettings.getFlag(this, "notification_permission_asked")) return
        AppSettings.setFlag(this, "notification_permission_asked")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun refreshDashboard() {
        dashboardJob?.cancel()
        dashboardJob = launchSafe {
            CategorySeeder.ensureDefaults(this@HomeActivity, userId)
            BillManager.rollover(this@HomeActivity, userId)

            val dailyReward = gamification.claimDailyReward(userId)
            val budgetReward = gamification.checkMonthlyBudgetReward(this@HomeActivity, userId)
            val achievements = AchievementManager.checkAll(this@HomeActivity, userId)
            val alerts = AlertEngine.notifyNew(this@HomeActivity, userId)

            loadPlayerStats()
            loadBudgetStatus()
            buildDashboard(alerts)

            if (budgetReward != null) {
                Toast.makeText(
                    this@HomeActivity,
                    "You stayed within budget last month! +${budgetReward.xpGained} XP +${budgetReward.goldGained} Gold",
                    Toast.LENGTH_LONG
                ).show()
            }
            for (a in achievements) {
                Toast.makeText(this@HomeActivity, "Achievement unlocked: ${a.name}", Toast.LENGTH_LONG).show()
            }
            if (dailyReward != null) showDailyRewardDialog(dailyReward)
        }
    }

    private fun showDailyRewardDialog(reward: GamificationManager.DailyRewardResult) {
        val message = StringBuilder()
            .append("Day ${reward.streak} of your login streak!\n\n")
            .append("+${reward.goldGained} Gold\n")
            .append("+${reward.xpGained} XP")
        if (reward.isBonusDay) message.append("\n\n7-day streak bonus included!")
        if (reward.leveledUp) {
            message.append("\n\nLEVEL UP! You are now level ${reward.newLevel} - ")
                .append(GamificationManager.titleForLevel(reward.newLevel))
        }
        AlertDialog.Builder(this)
            .setTitle("Daily Reward")
            .setMessage(message.toString())
            .setPositiveButton("Claim", null)
            .show()
    }

    //shows level, XP bar, Gold and login streak
    private suspend fun loadPlayerStats() {
        val stats = gamification.getStats(userId)
        val level = GamificationManager.levelForXp(stats.xp)
        val (intoLevel, levelSize) = GamificationManager.levelProgress(stats.xp)

        findViewById<TextView>(R.id.tvLevel).text =
            "Level $level - ${GamificationManager.titleForLevel(level)}"
        findViewById<ProgressBar>(R.id.pbXp).progress =
            ((intoLevel * 100) / levelSize).coerceIn(0, 100)
        findViewById<TextView>(R.id.tvXp).text = "XP $intoLevel / $levelSize"
        findViewById<TextView>(R.id.tvGold).text = "Gold: ${stats.gold}"
        findViewById<TextView>(R.id.tvStreak).text =
            "Login streak: ${stats.loginStreak} ${if (stats.loginStreak == 1) "day" else "days"}"
    }

    //monthly budget: total, spent, remaining and percentage used
    private suspend fun loadBudgetStatus() {
        val from = DateUtils.startOfCurrentMonth()
        val to = DateUtils.endOfToday()
        val spent = viewModel.getTotalSpentForPeriod(userId, from, to)
        val goal = viewModel.getGoal(userId)

        val progressBar = findViewById<ProgressBar>(R.id.pbBudgetProgress)
        val tvBudgetLeft = findViewById<TextView>(R.id.tvBudgetLeft)

        if (goal != null && goal.maxGoal > 0) {
            val percent = (spent / goal.maxGoal) * 100
            progressBar.progress = percent.toInt().coerceIn(0, 100)
            val remaining = goal.maxGoal - spent
            tvBudgetLeft.text =
                "Budget ${UiKit.money(goal.maxGoal)}   Spent ${UiKit.money(spent)}\n" +
                    (if (remaining >= 0) "Remaining ${UiKit.money(remaining)}"
                    else "Over budget by ${UiKit.money(-remaining)}") +
                    "   (${String.format(Locale.getDefault(), "%.0f", percent)}% used)"
        } else {
            progressBar.progress = 0
            tvBudgetLeft.text = "Spent ${UiKit.money(spent)} - set a monthly budget under Castle"
        }
    }

    //category progress, alerts, upcoming bills, savings, net worth and forecast
    private suspend fun buildDashboard(alerts: List<Alert>) {
        val container = findViewById<LinearLayout>(R.id.llDashboard)
        container.removeAllViews()

        //alerts
        if (alerts.isNotEmpty()) {
            container.addView(UiKit.text(this, "Alerts", UiKit.GOLD, 18f, true))
            val card = UiKit.card(this)
            for (alert in alerts) {
                val color = if (alert.severity >= 2) UiKit.RED else UiKit.ORANGE
                card.addView(UiKit.text(this, alert.title, color, 15f, true))
                card.addView(UiKit.text(this, alert.message, Color.WHITE, 13f))
            }
            container.addView(card)
        }

        //category progress
        val now = System.currentTimeMillis()
        val totals = db.expenseDao().getCategoryTotalsForPeriod(
            userId, DateUtils.startOfMonth(now), DateUtils.endOfToday()
        )
        val limits = db.categoryLimitDao().getAll(userId).associateBy { it.categoryId }
        container.addView(UiKit.text(this, "Category progress", UiKit.GOLD, 18f, true))
        val categoryCard = UiKit.card(this)
        var anyLimit = false
        for (total in totals) {
            val limit = limits[total.categoryId] ?: continue
            anyLimit = true
            val status = spendStatus(total.total, limit.limitAmount)
            categoryCard.addView(UiKit.text(this, total.categoryName, Color.WHITE, 15f, true))
            categoryCard.addView(
                UiKit.text(
                    this,
                    "${UiKit.money(total.total)} / ${UiKit.money(limit.limitAmount)}   ${UiKit.statusLabel(status)}",
                    UiKit.statusColor(status), 13f, status == SpendStatus.EXCEEDED
                )
            )
            categoryCard.addView(
                UiKit.progress(this, (total.total / limit.limitAmount * 100).toInt(), UiKit.statusColor(status))
            )
        }
        if (!anyLimit) {
            categoryCard.addView(
                UiKit.text(this, "No category limits yet. Set them under Castle > Category Spending Limits.", UiKit.MUTED, 13f)
            )
        }
        container.addView(categoryCard)

        //upcoming bills
        container.addView(UiKit.text(this, "Upcoming bills", UiKit.GOLD, 18f, true))
        val billCard = UiKit.card(this)
        val today = DateUtils.startOfToday()
        val upcoming = db.billDao().getAll(userId).filter { !it.isPaid }.take(3)
        if (upcoming.isEmpty()) {
            billCard.addView(UiKit.text(this, "No unpaid bills - the dungeon is quiet.", UiKit.MUTED, 13f))
        }
        for (bill in upcoming) {
            val days = DateUtils.daysBetween(today, bill.dueDate)
            val whenText = when {
                days < 0 -> "OVERDUE"
                days == 0 -> "due today"
                else -> "due ${dateFormat.format(bill.dueDate)} (in $days days)"
            }
            billCard.addView(
                UiKit.text(
                    this,
                    "${EnemyCatalog.forBill(bill.name).displayName} - ${UiKit.money(bill.amount)} - $whenText",
                    if (days <= 0) UiKit.RED else Color.WHITE, 14f
                )
            )
        }
        container.addView(billCard)

        //savings progress
        container.addView(UiKit.text(this, "Savings progress", UiKit.GOLD, 18f, true))
        val goalCard = UiKit.card(this)
        val goals = db.savingsGoalDao().getAll(userId).filter { !it.completed }.take(3)
        if (goals.isEmpty()) {
            goalCard.addView(UiKit.text(this, "No active savings goals. Add one under Castle.", UiKit.MUTED, 13f))
        }
        for (goal in goals) {
            val percent = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount * 100).toInt() else 0
            goalCard.addView(UiKit.text(this, goal.name, Color.WHITE, 15f, true))
            goalCard.addView(
                UiKit.text(this, "${UiKit.money(goal.currentAmount)} / ${UiKit.money(goal.targetAmount)}  ($percent%)", Color.WHITE, 13f)
            )
            goalCard.addView(UiKit.progress(this, percent, UiKit.GOLD))
        }
        container.addView(goalCard)

        //net worth + forecast
        container.addView(UiKit.text(this, "Outlook", UiKit.GOLD, 18f, true))
        val outlook = UiKit.card(this)
        val netWorth = NetWorthManager.totals(db.netWorthDao().getItems(userId)).netWorth
        outlook.addView(
            UiKit.text(this, "Net worth: ${UiKit.money(netWorth)}", if (netWorth >= 0) UiKit.GREEN else UiKit.RED, 15f, true)
        )
        val forecast = Forecaster.forecast(this, userId, 14)
        val sign = if (forecast.projectedBalance < 0) "-" else ""
        outlook.addView(
            UiKit.text(
                this,
                "Estimated balance in 14 days: $sign${UiKit.money(Math.abs(forecast.projectedBalance))}",
                if (forecast.isNegative) UiKit.RED else if (forecast.isLow) UiKit.ORANGE else Color.WHITE, 14f
            )
        )
        container.addView(outlook)
    }
}
