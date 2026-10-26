package com.soo.wardensvault

import android.graphics.Color
import android.graphics.Outline
import android.graphics.PorterDuff
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/*
Bedroom - the Warden's quarters, opened from the Map.
Everything in it follows what the user has equipped in the Armory:
  - the knight wears the equipped helmet, armour, shield and weapon, in the Warden colour
  - the Bedroom Theme tints the whole room
  - each owned Bedroom Decoration that is equipped appears in its spot
  - the plaque shows the equipped title (or the level title when none is equipped)
 */
class BedroomActivity : ScreenActivity() {

    override suspend fun render() {
        val stats = gamification.getStats(userId)
        val level = GamificationManager.levelForXp(stats.xp)
        val equipped = db.ownedItemDao().getAll(userId)
            .filter { it.equipped }
            .mapNotNull { CosmeticCatalog.byKey(it.itemKey) }
        fun inSlot(slot: Slot) = equipped.firstOrNull { it.slot == slot }
        val title = inSlot(Slot.TITLE)?.name ?: GamificationManager.titleForLevel(level)

        addTitle("Bedroom")
        addText("Your Warden's quarters. Everything here changes with what you equip in the Armory.", UiKit.MUTED, 14f)

        content.addView(buildScene(equipped, title))

        //summary under the room
        val card = addCard()
        card.addLine("Level $level - $title", UiKit.GOLD, 17f, true)
        card.addLine("Gold: ${stats.gold}", Color.WHITE, 15f)
        val theme = inSlot(Slot.BACKGROUND)?.name ?: "Plain"
        card.addLine("Room theme: $theme", Color.WHITE, 14f)
        val worn = equipped.filter { it.slot != Slot.BACKGROUND && it.slot != Slot.TITLE }
        card.addLine(
            if (worn.isEmpty()) "Nothing equipped yet - visit the Armory to dress your Warden and decorate."
            else "Equipped: " + worn.joinToString(", ") { it.name },
            UiKit.MUTED, 13f
        )
        addButton("Customise in the Armory") { goTo(ArmoryActivity::class.java) }
    }

    //the room picture with the knight, decorations and title plaque placed on top
    private fun buildScene(equipped: List<Cosmetic>, title: String): View {
        val aspect = ROOM_HEIGHT / ROOM_WIDTH
        val scene = SceneLayout(this, aspect)
        scene.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = UiKit.dp(this@BedroomActivity, 12) }
        //rounded corners
        val radius = UiKit.dp(this, 12).toFloat()
        scene.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }
        scene.clipToOutline = true

        //the room, tinted by the equipped Bedroom Theme
        val room = ImageView(this).apply {
            setImageDrawable(UiKit.crisp(this@BedroomActivity, R.drawable.room_bedroom))
            scaleType = ImageView.ScaleType.FIT_XY
        }
        val theme = equipped.firstOrNull { it.slot == Slot.BACKGROUND }
        if (theme != null) {
            room.setColorFilter(
                Color.argb(100, Color.red(theme.color), Color.green(theme.color), Color.blue(theme.color)),
                PorterDuff.Mode.SRC_ATOP
            )
        }
        scene.add(room, 0f, 0f, 1f, 1f)

        //decorations - each has its own spot in the room
        for (deco in equipped.filter { it.slot == Slot.DECORATION }) {
            val spot = DECORATION_SPOTS[deco.key] ?: continue
            val image = ImageView(this).apply {
                setImageDrawable(UiKit.crisp(this@BedroomActivity, deco.drawableRes))
                scaleType = ImageView.ScaleType.FIT_XY
            }
            scene.add(image, spot.left, spot.top, spot.width, scene.heightFor(spot.width, spot.imageW, spot.imageH))
        }

        //the knight standing on the floor in front of the bed
        val warden = WardenView(this)
        warden.show(equipped)
        val knightWidth = 0.40f
        val knightHeight = scene.heightFor(knightWidth, 320, 320)
        scene.add(warden, 0.47f - knightWidth / 2, 0.965f - knightHeight, knightWidth, knightHeight)

        //title plaque across the top
        val plaque = FrameLayout(this).apply {
            background = UiKit.crisp(this@BedroomActivity, R.drawable.ui_plaque)
        }
        val plaqueText = TextView(this).apply {
            text = title
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
            setAutoSizeTextTypeUniformWithConfiguration(8, 20, 1, TypedValue.COMPLEX_UNIT_SP)
            val pad = UiKit.dp(this@BedroomActivity, 4)
            setPadding(pad * 4, pad, pad * 4, pad)
        }
        plaque.addView(plaqueText, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ))
        val plaqueWidth = 0.84f
        scene.add(plaque, 0.08f, 0.02f, plaqueWidth, scene.heightFor(plaqueWidth, 1468, 245))

        return scene
    }

    //where a decoration sits (fractions of the room) and its image size in pixels
    private data class Spot(val left: Float, val top: Float, val width: Float, val imageW: Int, val imageH: Int)

    companion object {
        private const val ROOM_WIDTH = 1080f
        private const val ROOM_HEIGHT = 1920f

        private val DECORATION_SPOTS = mapOf(
            "deco_banner" to Spot(0.07f, 0.12f, 0.15f, 180, 230),   //on the left wall
            "deco_torch" to Spot(0.58f, 0.29f, 0.12f, 160, 200),    //on the right wall
            "deco_chest" to Spot(0.66f, 0.875f, 0.28f, 260, 160)    //on the floor
        )
    }
}
