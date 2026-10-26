// Code Attribution
// Based on: Create a custom drawing
// Author:   Android Developers
// URL:      https://developer.android.com/develop/ui/views/layout/custom-views/custom-drawing
// Accessed: 27 October 2026
// Used for: onMeasure / onDraw with Canvas and Paint

package com.soo.wardensvault

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

//Spending for one day, split by category name
data class DaySpend(val label: String, val parts: Map<String, Double>) {
    val total: Double get() = parts.values.sum()
}

//Stacked bar chart: one bar per day, each coloured segment is a category
class SpendingChartView(context: Context) : View(context) {
    private var days: List<DaySpend> = emptyList()
    private var colors: Map<String, Int> = emptyMap()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density

    fun setData(days: List<DaySpend>, colors: Map<String, Int>) {
        this.days = days
        this.colors = colors
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), (260 * density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (days.isEmpty()) return

        val left = 52 * density
        val right = width - 8 * density
        val top = 12 * density
        val bottom = height - 28 * density
        val maxTotal = maxOf(days.maxOf { it.total }, 1.0)

        //horizontal grid lines with amounts
        paint.textSize = 10 * density
        paint.textAlign = Paint.Align.LEFT
        for (i in 0..4) {
            val y = bottom - (bottom - top) * i / 4f
            paint.color = 0x55FFFFFF
            paint.strokeWidth = density
            canvas.drawLine(left, y, right, y, paint)
            paint.color = Color.WHITE
            canvas.drawText("R" + String.format("%.0f", maxTotal * i / 4), 2 * density, y + 4 * density, paint)
        }

        //stacked bars
        val slot = (right - left) / days.size
        val barWidth = slot * 0.7f
        for ((index, day) in days.withIndex()) {
            var yBottom = bottom
            val x = left + slot * index + (slot - barWidth) / 2
            for ((category, amount) in day.parts) {
                val h = (amount / maxTotal * (bottom - top)).toFloat()
                paint.color = colors[category] ?: Color.GRAY
                canvas.drawRect(x, yBottom - h, x + barWidth, yBottom, paint)
                yBottom -= h
            }
        }

        //date labels, thinned out so they don't overlap
        val step = maxOf(1, days.size / 6)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        var i = 0
        while (i < days.size) {
            canvas.drawText(days[i].label, left + slot * i + slot / 2, height - 8 * density, paint)
            i += step
        }
    }
}
