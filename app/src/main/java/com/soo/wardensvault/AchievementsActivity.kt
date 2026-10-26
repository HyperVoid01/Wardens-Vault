package com.soo.wardensvault

import android.graphics.Color

//Lists every achievement and which ones the Warden has unlocked
class AchievementsActivity : ScreenActivity() {

    override suspend fun render() {
        announce(AchievementManager.checkAll(this, userId))
        val unlocked = db.achievementDao().getAll(userId).associateBy { it.achievementKey }

        addTitle("Achievements")
        addText("${unlocked.size} of ${AchievementManager.all.size} unlocked", UiKit.MUTED, 14f)

        for (def in AchievementManager.all) {
            val entry = unlocked[def.key]
            val card = addCard()
            card.addLine(
                (if (entry != null) "★ " else "☆ ") + def.name,
                if (entry != null) UiKit.GOLD else Color.WHITE, 17f, true
            )
            card.addLine(def.description, UiKit.MUTED, 14f)
            if (entry != null) {
                card.addLine("Unlocked ${dateFormat.format(entry.unlockedAt)}", UiKit.GREEN, 12f)
            } else {
                card.addLine("Reward: ${def.xp} XP, ${def.gold} Gold", UiKit.MUTED, 12f)
            }
        }
    }
}
