package com.soo.wardensvault

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.Locale

//Small helpers that build the themed views used by screens created in code
object UiKit {
    val GOLD: Int = Color.parseColor("#FFC107")
    val GREEN: Int = Color.parseColor("#66BB6A")
    val ORANGE: Int = Color.parseColor("#FFA726")
    val RED: Int = Color.parseColor("#EF5350")
    val MUTED: Int = Color.parseColor("#B0BEC5")
    private val CARD_BLUE: Int = Color.parseColor("#E61A237E")

    fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    fun money(value: Double): String = "R" + String.format(Locale.getDefault(), "%.2f", value)

    fun text(
        context: Context,
        text: String,
        color: Int = Color.WHITE,
        sizeSp: Float = 16f,
        bold: Boolean = false
    ): TextView = TextView(context).apply {
        this.text = text
        setTextColor(color)
        textSize = sizeSp
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        //soft dark shadow keeps text readable over any background
        setShadowLayer(3f, 1f, 1f, Color.argb(200, 0, 0, 0))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(context, 4) }
    }

    //wooden pill button (Button.png), same look as the ActionButton style in XML
    fun button(context: Context, label: String, onClick: () -> Unit): Button =
        Button(context).apply {
            text = label
            backgroundTintList = null
            background = crisp(context, R.drawable.btn_pill)
            setTextColor(Color.parseColor("#FFF3D6"))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setShadowLayer(2f, 1f, 1f, Color.argb(200, 0, 0, 0))
            isAllCaps = false
            minHeight = dp(context, 52)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(context, 8) }
            setOnClickListener { onClick() }
        }

    //rounded translucent panel to group related rows
    fun card(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val pad = dp(context, 12)
        setPadding(pad, pad, pad, pad)
        background = GradientDrawable().apply {
            setColor(CARD_BLUE)
            cornerRadius = dp(context, 10).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(context, 10) }
    }

    fun progress(context: Context, percent: Int, color: Int): ProgressBar =
        ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = percent.coerceIn(0, 100)
            progressTintList = ColorStateList.valueOf(color)
            progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#55FFFFFF"))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 14)
            ).apply { topMargin = dp(context, 6) }
        }

    fun statusColor(status: SpendStatus): Int = when (status) {
        SpendStatus.SAFE -> GREEN
        SpendStatus.APPROACHING -> ORANGE
        SpendStatus.EXCEEDED -> RED
    }

    fun statusLabel(status: SpendStatus): String = when (status) {
        SpendStatus.SAFE -> "Safe"
        SpendStatus.APPROACHING -> "Approaching limit"
        SpendStatus.EXCEEDED -> "OVER BUDGET"
    }

    /*
    Loads a pixel-art image without smoothing, so it stays sharp when scaled up
    (Android blurs scaled images by default).
     */
    fun crisp(context: Context, resId: Int): Drawable {
        val drawable = ContextCompat.getDrawable(context, resId)!!.mutate()
        drawable.isFilterBitmap = false   //works for normal images and 9-patches
        return drawable
    }

    //screen title on the wooden plaque, matching the ScreenTitle style in XML
    fun title(context: Context, text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(Color.parseColor("#FFF3D6"))
        textSize = 22f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setShadowLayer(3f, 2f, 2f, Color.argb(200, 0, 0, 0))
        gravity = android.view.Gravity.CENTER
        background = crisp(context, R.drawable.title_plaque)
        minHeight = dp(context, 60)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    //clips a view to rounded corners
    fun roundCorners(view: View, radius: Float) {
        view.outlineProvider = object : android.view.ViewOutlineProvider() {
            override fun getOutline(v: View, outline: android.graphics.Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, radius)
            }
        }
        view.clipToOutline = true
    }

    //label shown above an input in screens built in code
    fun label(context: Context, text: String): TextView =
        text(context, text, Color.WHITE, 15f, true).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = dp(context, 14)
        }

    fun spacer(context: Context, heightDp: Int): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(context, heightDp)
        )
    }
}
