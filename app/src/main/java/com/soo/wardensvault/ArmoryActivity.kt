package com.soo.wardensvault

import android.graphics.Color
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

//Armory: spend Gold on cosmetics for your Warden and dungeon. Purely visual - never affects budgets.
class ArmoryActivity : ScreenActivity() {

    override suspend fun render() {
        val stats = gamification.getStats(userId)
        val level = GamificationManager.levelForXp(stats.xp)
        val owned = db.ownedItemDao().getAll(userId).associateBy { it.itemKey }

        addTitle("Armory")
        addText("Level $level   -   Gold: ${stats.gold}", UiKit.GOLD, 18f, true)
        addText("Cosmetics are only for show and never change your finances.", UiKit.MUTED, 12f)

        for (slot in Slot.values()) {
            addHeader(slot.label)
            for (item in CosmeticCatalog.items.filter { it.slot == slot }) {
                val ownedItem = owned[item.key]
                val card = addCard()
                card.addLine(item.name, Color.WHITE, 17f, true)
                when {
                    ownedItem != null && ownedItem.equipped ->
                        card.addLine("EQUIPPED - tap to unequip", UiKit.GREEN, 13f, true)
                    ownedItem != null ->
                        card.addLine("Owned - tap to equip", UiKit.GOLD, 13f)
                    level < item.minLevel ->
                        card.addLine("Requires level ${item.minLevel}", UiKit.MUTED, 13f)
                    else ->
                        card.addLine("${item.price} Gold - tap to buy", UiKit.ORANGE, 13f)
                }
                card.setOnClickListener {
                    when {
                        ownedItem != null && ownedItem.equipped -> unequip(item)
                        ownedItem != null -> equip(item)
                        level < item.minLevel -> toast("You need level ${item.minLevel} for ${item.name}")
                        else -> confirm("Buy ${item.name}", "Spend ${item.price} Gold on ${item.name}?") { buy(item) }
                    }
                }
            }
        }
    }

    private fun buy(item: Cosmetic) {
        launchSafe {
            if (!gamification.spendGold(userId, item.price)) {
                toast("Not enough Gold")
                return@launchSafe
            }
            db.ownedItemDao().insert(OwnedItem(userId = userId, itemKey = item.key, equipped = false))
            setEquipped(item, true)
            toast("${item.name} bought and equipped!")
            announce(AchievementManager.checkAll(this@ArmoryActivity, userId))
            refresh()
        }
    }

    private fun equip(item: Cosmetic) {
        launchSafe {
            setEquipped(item, true)
            refresh()
        }
    }

    private fun unequip(item: Cosmetic) {
        launchSafe {
            setEquipped(item, false)
            refresh()
        }
    }

    //only one item per slot can be equipped at a time
    private suspend fun setEquipped(item: Cosmetic, equip: Boolean) {
        for (owned in db.ownedItemDao().getAll(userId)) {
            val ownedSlot = CosmeticCatalog.byKey(owned.itemKey)?.slot ?: continue
            if (ownedSlot != item.slot) continue
            val shouldBeEquipped = equip && owned.itemKey == item.key
            if (owned.equipped != shouldBeEquipped) {
                db.ownedItemDao().update(owned.copy(equipped = shouldBeEquipped))
            }
        }
    }
}
