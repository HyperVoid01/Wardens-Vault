// Code Attribution
// Based on: Drawables overview
// Author:   Android Developers
// URL:      https://developer.android.com/guide/topics/graphics/2d-graphics
// Accessed: 27 October 2026
// Used for: Custom Drawable subclass

package com.soo.wardensvault

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable

/*
The brick wall behind every screen.

Before, each screen stretched the 1080x1920 wall image to fit itself, so the bricks changed
size from screen to screen (and squashed when the keyboard opened). Now one strip of bricks
(exactly one repeat of the pattern) is scaled to the screen WIDTH and repeated downwards,
so the bricks are always the same size. A dark shade on top keeps white text readable.
 */
class WallDrawable(context: Context) : Drawable() {
    private val strip: Bitmap = loadStrip(context)
    private val pixelPaint = Paint().apply { isFilterBitmap = false }   //keeps pixel art crisp
    private val shadePaint = Paint().apply { color = Color.argb(115, 0, 0, 0) }
    private val dst = RectF()

    override fun draw(canvas: Canvas) {
        val b = bounds
        if (b.isEmpty) return
        val stripHeight = strip.height * (b.width().toFloat() / strip.width)
        var y = b.top.toFloat()
        while (y < b.bottom) {
            dst.set(b.left.toFloat(), y, b.right.toFloat(), y + stripHeight)
            canvas.drawBitmap(strip, null, dst, pixelPaint)
            y += stripHeight
        }
        canvas.drawRect(b, shadePaint)
    }

    override fun setAlpha(alpha: Int) {
        pixelPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        pixelPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    companion object {
        @Volatile private var cached: Bitmap? = null

        //decoded once and shared by every screen
        private fun loadStrip(context: Context): Bitmap {
            return cached ?: synchronized(this) {
                cached ?: BitmapFactory.decodeResource(context.resources, R.drawable.bg_wall_strip)
                    .also { cached = it }
            }
        }
    }
}
