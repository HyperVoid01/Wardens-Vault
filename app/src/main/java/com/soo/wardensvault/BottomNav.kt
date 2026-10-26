package com.soo.wardensvault

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView

/*
Bottom navigation shared by every screen after login: Home, Map and Profile.

  - The icons are full-colour pixel art, so the default tint is switched off in code
    (setting itemIconTint="@null" in XML doesn't reliably work - that's why they didn't show).
  - Tapping a tab brings that screen to the front instead of piling up copies of it.
  - On screens that aren't one of the three tabs, Map is shown as selected (they are all
    reached from the Map) and tapping Map goes back to it.
 */
object BottomNav {
    enum class Tab(val itemId: Int) {
        HOME(R.id.nav_bedroom),
        MAP(R.id.nav_map),
        PROFILE(R.id.nav_helmet)
    }

    val BAR_COLOR: Int = Color.parseColor("#D95763")

    //creates the bar in code (used by screens that are built in code)
    fun create(activity: AppCompatActivity): BottomNavigationView {
        return BottomNavigationView(activity).apply {
            inflateMenu(R.menu.bottom_nav_menu)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
    }

    //styles the bar and wires up the tabs; current = the tab this screen IS, or null
    fun attach(activity: AppCompatActivity, nav: BottomNavigationView, current: Tab?) {
        nav.setBackgroundColor(BAR_COLOR)
        nav.itemIconTintList = null
        nav.itemIconSize = UiKit.dp(activity, 34)
        nav.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        nav.itemTextColor = ColorStateList.valueOf(Color.WHITE)
        nav.selectedItemId = (current ?: Tab.MAP).itemId

        //returning false keeps this screen's tab highlighted when the user comes back
        nav.setOnItemSelectedListener { item ->
            open(activity, item.itemId, current)
            false
        }
        nav.setOnItemReselectedListener { item ->
            open(activity, item.itemId, current)
        }
    }

    private fun open(activity: AppCompatActivity, itemId: Int, current: Tab?) {
        if (current != null && current.itemId == itemId) return   //already on that tab
        val target = when (itemId) {
            R.id.nav_bedroom -> HomeActivity::class.java
            R.id.nav_map -> MapMenuActivity::class.java
            else -> ProfileActivity::class.java
        }
        val intent = Intent(activity, target)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        activity.startActivity(intent)
    }
}
