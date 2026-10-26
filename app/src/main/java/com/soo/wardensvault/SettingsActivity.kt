package com.soo.wardensvault

import android.graphics.Color
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat

//Settings: turn notifications on or off, in total or per type
class SettingsActivity : ScreenActivity() {

    override suspend fun render() {
        addTitle("Settings")
        addHeader("Notifications")
        addSwitch("Enable notifications", AppSettings.KEY_ALL)
        addSwitch("Budget and category limit alerts", AppSettings.KEY_BUDGET)
        addSwitch("Bill reminders", AppSettings.KEY_BILLS)
        addSwitch("Savings goal completed", AppSettings.KEY_GOALS)
        addSwitch("Achievement unlocked", AppSettings.KEY_ACHIEVEMENTS)
        addSwitch("Cash flow warnings", AppSettings.KEY_CASHFLOW)
        addText(
            "If you don't receive notifications, make sure Warden's Vault is allowed to send them in your phone's app settings.",
            UiKit.MUTED, 12f
        )
    }

    private fun addSwitch(label: String, key: String) {
        val toggle = SwitchCompat(this)
        toggle.text = label
        toggle.setTextColor(Color.WHITE)
        toggle.isChecked = AppSettings.isEnabled(this, key)
        toggle.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = UiKit.dp(this@SettingsActivity, 10) }
        toggle.setOnCheckedChangeListener { _, checked ->
            AppSettings.setEnabled(this, key, checked)
        }
        content.addView(toggle)
    }
}
