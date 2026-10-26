package com.soo.wardensvault

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

/*
Profile = the Warden and game progression: username, level, XP, Gold, title, equipped
cosmetics, bills defeated, expenses logged, login streak, goals completed and achievements.
Settings and Achievements are opened from here.
 */
class ProfileActivity : AppCompatActivity() {
    private var userId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)
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

        findViewById<TextView>(R.id.tvProfileName).text =
            SessionManager.getLoggedInFullName(this) ?: "Warden"

        findViewById<Button>(R.id.btnAchievements).setOnClickListener {
            startActivity(Intent(this, AchievementsActivity::class.java))
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            SessionManager.clearSession(this)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_bedroom -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, MapMenuActivity::class.java))
                    true
                }
                R.id.nav_helmet -> true // already here
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (userId.isNotEmpty()) loadProfile()
    }

    private fun loadProfile() {
        launchSafe {
            val db = AppDatabase.getDatabase(this@ProfileActivity)
            val stats = GamificationManager(this@ProfileActivity).getStats(userId)
            val level = GamificationManager.levelForXp(stats.xp)
            val (intoLevel, levelSize) = GamificationManager.levelProgress(stats.xp)

            val user = db.userDao().getUserById(userId)
            findViewById<TextView>(R.id.tvProfileUsername).text = "Username: ${user?.username ?: "-"}"

            //equipped cosmetics
            val equipped = db.ownedItemDao().getAll(userId)
                .filter { it.equipped }
                .mapNotNull { CosmeticCatalog.byKey(it.itemKey) }
            val equippedTitle = equipped.firstOrNull { it.slot == Slot.TITLE }
            val title = equippedTitle?.name ?: GamificationManager.titleForLevel(level)

            findViewById<TextView>(R.id.tvProfileTitle).text = "Level $level - $title"
            findViewById<ProgressBar>(R.id.pbProfileXp).progress =
                ((intoLevel * 100) / levelSize).coerceIn(0, 100)
            findViewById<TextView>(R.id.tvProfileXp).text =
                "XP: ${stats.xp} ($intoLevel / $levelSize to next level)"
            findViewById<TextView>(R.id.tvProfileGold).text = "Gold: ${stats.gold}"

            findViewById<TextView>(R.id.tvProfileEquipped).text =
                "Equipped: " + (if (equipped.isEmpty()) "nothing yet (visit the Armory)" else equipped.joinToString(", ") { it.name })

            findViewById<TextView>(R.id.tvProfileStreak).text =
                "Login streak: ${stats.loginStreak} (best ${stats.longestStreak})"
            findViewById<TextView>(R.id.tvProfileExpenses).text = "Expenses logged: ${stats.expensesLogged}"
            findViewById<TextView>(R.id.tvProfileBills).text =
                "Bills defeated: ${db.billPaymentDao().count(userId)}"
            findViewById<TextView>(R.id.tvProfileGoals).text =
                "Goals completed: ${db.savingsGoalDao().countCompleted(userId)}"
            findViewById<TextView>(R.id.tvProfileAchievements).text =
                "Achievements: ${db.achievementDao().getAll(userId).size} / ${AchievementManager.all.size}"

            //Warden colour tints the knight, dungeon background recolours the screen
            val warden = findViewById<ImageView>(R.id.ivWarden)
            val colour = equipped.firstOrNull { it.slot == Slot.COLOUR }
            if (colour != null) {
                warden.setColorFilter(Color.argb(110, Color.red(colour.color), Color.green(colour.color), Color.blue(colour.color)), PorterDuff.Mode.SRC_ATOP)
            } else {
                warden.clearColorFilter()
            }
            val background = equipped.firstOrNull { it.slot == Slot.BACKGROUND }
            val root = findViewById<View>(R.id.main)
            if (background != null) root.setBackgroundColor(background.color) else root.setBackgroundResource(R.drawable.ic_wall)
        }
    }
}
