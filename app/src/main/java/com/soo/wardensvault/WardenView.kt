package com.soo.wardensvault

import android.content.Context
import android.graphics.PorterDuff
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView

/*
The user's knight, built from layers so it shows exactly what they have equipped:
  base knight -> armour -> helmet -> shield -> weapon
All layers are 320x320 pixel-art images drawn to line up, so they're simply stacked.

  - No colour equipped: the blue knight. A Warden colour tints the grey knight instead.
  - When a weapon or shield is equipped, a base knight without its own sword/shield is
    used so the old one doesn't poke out from behind the new one.
Can be used in XML (<com.soo.wardensvault.WardenView .../>) or created in code.
 */
class WardenView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val base = addLayer()
    private val armour = addLayer()
    private val helmet = addLayer()
    private val shield = addLayer()
    private val weapon = addLayer()

    init {
        show(emptyList())
    }

    private fun addLayer(): ImageView {
        val layer = ImageView(context)
        layer.scaleType = ImageView.ScaleType.FIT_CENTER
        addView(layer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        return layer
    }

    //redraws the knight wearing the given equipped cosmetics
    fun show(equipped: List<Cosmetic>) {
        fun inSlot(slot: Slot) = equipped.firstOrNull { it.slot == slot }

        val colour = inSlot(Slot.COLOUR)
        val hasWeapon = inSlot(Slot.WEAPON) != null
        val hasShield = inSlot(Slot.SHIELD) != null

        val baseRes = if (colour == null) {
            when {
                hasWeapon && hasShield -> R.drawable.knight_blue_bare
                hasWeapon -> R.drawable.knight_blue_nosword
                hasShield -> R.drawable.knight_blue_noshield
                else -> R.drawable.knight_blue
            }
        } else {
            when {
                hasWeapon && hasShield -> R.drawable.knight_grey_bare
                hasWeapon -> R.drawable.knight_grey_nosword
                hasShield -> R.drawable.knight_grey_noshield
                else -> R.drawable.knight_grey
            }
        }
        base.setImageDrawable(UiKit.crisp(context, baseRes))
        //MULTIPLY keeps the knight's shading while changing its colour
        if (colour != null) base.setColorFilter(colour.color, PorterDuff.Mode.MULTIPLY) else base.clearColorFilter()

        setLayer(armour, inSlot(Slot.ARMOUR))
        setLayer(helmet, inSlot(Slot.HELMET))
        setLayer(shield, inSlot(Slot.SHIELD))
        setLayer(weapon, inSlot(Slot.WEAPON))
    }

    private fun setLayer(layer: ImageView, item: Cosmetic?) {
        if (item == null || item.drawableRes == 0) {
            layer.setImageDrawable(null)
        } else {
            layer.setImageDrawable(UiKit.crisp(context, item.drawableRes))
        }
    }
}
