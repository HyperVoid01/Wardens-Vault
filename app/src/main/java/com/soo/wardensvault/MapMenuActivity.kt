// Code Attribution
// Based on: Display content edge-to-edge in views
// Author:   Android Developers
// URL:      https://developer.android.com/develop/ui/views/layout/edge-to-edge
// Accessed: 27 October 2026
// Used for: enableEdgeToEdge and window insets setup

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

        //draw the pixel-art icons sharp instead of blurred
        val icons = mapOf(
            R.id.rowBedroom to R.drawable.nav_home,
            R.id.rowCastle to R.drawable.menu_icon_castle,
            R.id.rowMarket to R.drawable.menu_icon_market,
            R.id.rowArmory to R.drawable.menu_icon_armory,
            R.id.rowDungeon to R.drawable.menu_icon_dungeon,
            R.id.rowTreasury to R.drawable.menu_icon_treasury,
            R.id.rowReports to R.drawable.menu_icon_reports,
            R.id.rowProfile to R.drawable.menu_icon_profile
        )
        for ((rowId, iconRes) in icons) {
            val row = findViewById<android.view.ViewGroup>(rowId)
            (row.getChildAt(0) as? android.widget.ImageView)?.setImageDrawable(UiKit.crisp(this, iconRes))
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
