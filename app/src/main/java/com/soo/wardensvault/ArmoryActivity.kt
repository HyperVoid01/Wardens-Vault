package com.soo.wardensvault

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout

/*
Armory: spend Gold on cosmetics for your Warden and Bedroom. Purely visual - never affects budgets.
The knight at the top shows what is equipped right now, and each item shows its picture.
 */
class ArmoryActivity : ScreenActivity() {

    override suspend fun render() {
        val stats = gamification.getStats(userId)
        val level = GamificationManager.levelForXp(stats.xp)
        val ownedList = db.ownedItemDao().getAll(userId)
        val owned = ownedList.associateBy { it.itemKey }
        val equipped = ownedList.filter { it.equipped }.mapNotNull { CosmeticCatalog.byKey(it.itemKey) }

        addTitle("Armory")
        addText("Level $level   -   Gold: ${stats.gold}", UiKit.GOLD, 18f, true)
        addText("Cosmetics are only for show and never change your finances.", UiKit.MUTED, 12f)

        //the Warden as they look now
        content.addView(showcase(equipped))
        addButton("View your Bedroom") { goTo(BedroomActivity::class.java) }

        //everything the user owns, in the inventory grid
        addHeader("Your Inventory")
        val ownedItems = ownedList.mapNotNull { o -> CosmeticCatalog.byKey(o.itemKey)?.let { Pair(it, o.equipped) } }
        if (ownedItems.isEmpty()) {
            addText("Nothing here yet. Buy something below and it appears in your inventory.", UiKit.MUTED, 13f)
        } else {
            addText("Tap an item to equip or unequip it. Faded items are not equipped.", UiKit.MUTED, 12f)
            content.addView(inventoryGrid(ownedItems))
        }

        addHeader("Shop")
        for (slot in Slot.values()) {
            addHeader(slot.label)
            if (slot == Slot.DECORATION) {
                addText("Decorations appear in your Bedroom. You can display all of them at once.", UiKit.MUTED, 12f)
            }
            for (item in CosmeticCatalog.items.filter { it.slot == slot }) {
                val ownedItem = owned[item.key]
                val card = addCard()
                card.orientation = LinearLayout.HORIZONTAL
                card.gravity = Gravity.CENTER_VERTICAL
                card.addView(thumbnail(item))

                val texts = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginStart = UiKit.dp(this@ArmoryActivity, 12)
                    }
                }
                texts.addLine(item.name, Color.WHITE, 17f, true)
                when {
                    ownedItem != null && ownedItem.equipped ->
                        texts.addLine("EQUIPPED - tap to unequip", UiKit.GREEN, 13f, true)
                    ownedItem != null ->
                        texts.addLine("Owned - tap to equip", UiKit.GOLD, 13f)
                    level < item.minLevel ->
                        texts.addLine("Requires level ${item.minLevel}", UiKit.MUTED, 13f)
                    else ->
                        texts.addLine("${item.price} Gold - tap to buy", UiKit.ORANGE, 13f)
                }
                card.addView(texts)

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

    //the knight standing in a pit of gold (CoinBack.png), wearing what is equipped now
    private fun showcase(equipped: List<Cosmetic>): SceneLayout {
        val scene = SceneLayout(this, SHOWCASE_HEIGHT / SHOWCASE_WIDTH)
        scene.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = UiKit.dp(this@ArmoryActivity, 12) }
        UiKit.roundCorners(scene, UiKit.dp(this, 12).toFloat())

        val backdrop = ImageView(this).apply {
            setImageDrawable(UiKit.crisp(this@ArmoryActivity, R.drawable.scene_forge))
            scaleType = ImageView.ScaleType.FIT_XY
        }
        scene.add(backdrop, 0f, 0f, 1f, 1f)

        val warden = WardenView(this)
        warden.show(equipped)
        val knightWidth = 0.55f
        val knightHeight = scene.heightFor(knightWidth, 320, 320)
        scene.add(warden, 0.5f - knightWidth / 2, 0.96f - knightHeight, knightWidth, knightHeight)
        return scene
    }

    /*
    Base.png's inventory grid (5 columns x 7 rows) with an owned item in each slot.
    The cell positions are measured from the picture.
     */
    private fun inventoryGrid(items: List<Pair<Cosmetic, Boolean>>): SceneLayout {
        val grid = SceneLayout(this, GRID_HEIGHT / GRID_WIDTH)
        grid.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = UiKit.dp(this@ArmoryActivity, 8) }
        val background = ImageView(this).apply {
            setImageDrawable(UiKit.crisp(this@ArmoryActivity, R.drawable.ui_inventory))
            scaleType = ImageView.ScaleType.FIT_XY
        }
        grid.add(background, 0f, 0f, 1f, 1f)

        for ((index, entry) in items.take(GRID_COLUMNS.size * GRID_ROWS.size).withIndex()) {
            val (item, isEquipped) = entry
            val (x0, x1) = GRID_COLUMNS[index % GRID_COLUMNS.size]
            val (y0, y1) = GRID_ROWS[index / GRID_COLUMNS.size]
            val inset = (x1 - x0) * 0.15f
            val cell = thumbnail(item).apply {
                alpha = if (isEquipped) 1f else 0.45f
                setOnClickListener { if (isEquipped) unequip(item) else equip(item) }
            }
            grid.add(
                cell,
                (x0 + inset) / GRID_WIDTH, (y0 + inset) / GRID_HEIGHT,
                (x1 - x0 - 2 * inset) / GRID_WIDTH, (y1 - y0 - 2 * inset) / GRID_HEIGHT
            )
        }
        return grid
    }

    //small picture of the item: its art, a colour swatch, or a scroll for titles
    private fun thumbnail(item: Cosmetic): View {
        val size = UiKit.dp(this, 56)
        val params = LinearLayout.LayoutParams(size, size)
        return when {
            item.drawableRes != 0 -> ImageView(this).apply {
                setImageDrawable(UiKit.crisp(this@ArmoryActivity, item.drawableRes))
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = params
            }
            item.color != 0 -> View(this).apply {
                background = GradientDrawable().apply {
                    setColor(item.color)
                    cornerRadius = UiKit.dp(this@ArmoryActivity, 8).toFloat()
                    setStroke(UiKit.dp(this@ArmoryActivity, 2), Color.WHITE)
                }
                layoutParams = params
            }
            else -> ImageView(this).apply {
                setImageDrawable(UiKit.crisp(this@ArmoryActivity, R.drawable.nav_map))
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = params
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

    /*
    Only one item per slot can be equipped at a time - except decorations,
    which each have their own spot in the Bedroom, so any number can be shown.
     */
    private suspend fun setEquipped(item: Cosmetic, equip: Boolean) {
        val onlyOne = item.slot != Slot.DECORATION
        for (owned in db.ownedItemDao().getAll(userId)) {
            val ownedSlot = CosmeticCatalog.byKey(owned.itemKey)?.slot ?: continue
            if (ownedSlot != item.slot) continue
            val shouldBeEquipped = when {
                owned.itemKey == item.key -> equip
                onlyOne && equip -> false        //equipping this one takes the others off
                else -> owned.equipped            //leave other decorations as they are
            }
            if (owned.equipped != shouldBeEquipped) {
                db.ownedItemDao().update(owned.copy(equipped = shouldBeEquipped))
            }
        }
    }

    companion object {
        private const val SHOWCASE_WIDTH = 1080f
        private const val SHOWCASE_HEIGHT = 840f

        //inventory picture size and the inside of each grid slot (pixels in Base.png)
        private const val GRID_WIDTH = 1080f
        private const val GRID_HEIGHT = 1660f
        private val GRID_COLUMNS = listOf(60f to 250f, 260f to 440f, 450f to 640f, 650f to 830f, 840f to 1020f)
        private val GRID_ROWS = listOf(
            250f to 440f, 450f to 630f, 640f to 820f, 830f to 1010f,
            1020f to 1200f, 1210f to 1390f, 1400f to 1580f
        )
    }
}
