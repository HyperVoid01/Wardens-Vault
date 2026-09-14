package com.soo.wardensvault

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val fullName = intent.getStringExtra("fullName")
        findViewById<TextView>(R.id.tvWelcome).text = "Welcome, $fullName!"

        findViewById<Button>(R.id.btnViewExpenses).setOnClickListener {
            startActivity(Intent(this, ExpenseListActivity::class.java))
        }

        findViewById<Button>(R.id.btnViewCategoryTotals).setOnClickListener {
            startActivity(Intent(this, CategoryTotalsActivity::class.java))
        }

        //temporary mapping - reassign these to whichever nav items fit the theme once
        //the expense-list and totals screens exist too
        findViewById<BottomNavigationView>(R.id.bottomNavigationView).setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_bedroom -> {
                    startActivity(Intent(this, CreateCategoryActivity::class.java))
                    true
                }
                R.id.nav_map -> {
                    startActivity(Intent(this, CreateExpenseActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}