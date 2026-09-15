package com.soo.wardensvault

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

/*
Map menu - the themed hub screen. Each row maps to one of the app's real features:
  Castle   -> Home dashboard
  Dungeon  -> log a new expense (venturing out costs you)
  Treasury -> category totals (how much has gone where)
  Market   -> budget goal (set your spending limits)
  Reports  -> expense history list
  Armory   -> manage categories (your equipment loadout)
  Profile  -> account / logout
 */
class MapMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (SessionManager.getLoggedInUserId(this) == -1) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        findViewById<android.view.View>(R.id.rowCastle).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowDungeon).setOnClickListener {
            startActivity(Intent(this, CreateExpenseActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowTreasury).setOnClickListener {
            startActivity(Intent(this, CategoryTotalsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowMarket).setOnClickListener {
            startActivity(Intent(this, BudgetGoalActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowReports).setOnClickListener {
            startActivity(Intent(this, ExpenseListActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowArmory).setOnClickListener {
            startActivity(Intent(this, CreateCategoryActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_bedroom -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.nav_map -> true // already here
                R.id.nav_helmet -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
