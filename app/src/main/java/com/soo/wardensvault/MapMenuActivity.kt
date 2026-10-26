package com.soo.wardensvault

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

/*
Map menu - the themed hub screen:
  Bedroom  -> the Warden's room, changes with Armory customisations
  Castle   -> Budgets & Goals
  Dungeon  -> Bills (enemies)
  Treasury -> Income & Net Worth
  Market   -> Expenses
  Reports  -> Analytics
  Armory   -> Customisation
  Profile  -> account / settings / logout
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

        if (SessionManager.getLoggedInUserId(this).isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        findViewById<android.view.View>(R.id.rowBedroom).setOnClickListener {
            startActivity(Intent(this, BedroomActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowCastle).setOnClickListener { openHub("castle") }
        findViewById<android.view.View>(R.id.rowDungeon).setOnClickListener {
            startActivity(Intent(this, BillsActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowTreasury).setOnClickListener { openHub("treasury") }
        findViewById<android.view.View>(R.id.rowMarket).setOnClickListener { openHub("market") }
        findViewById<android.view.View>(R.id.rowReports).setOnClickListener { openHub("reports") }
        findViewById<android.view.View>(R.id.rowArmory).setOnClickListener {
            startActivity(Intent(this, ArmoryActivity::class.java))
        }
        findViewById<android.view.View>(R.id.rowProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        BottomNav.attach(this, findViewById(R.id.bottomNavigationView), BottomNav.Tab.MAP)
    }

    private fun openHub(hub: String) {
        val intent = Intent(this, HubActivity::class.java)
        intent.putExtra("hub", hub)
        startActivity(intent)
    }
}
