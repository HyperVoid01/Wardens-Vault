package com.soo.wardensvault

import android.content.Context
import android.view.View
import android.view.ViewGroup

/*
A picture "stage" that keeps the shape of the room art (height = width x aspect) and places
each child at a position given as fractions of the stage, e.g. left = 0.5 means halfway across.
Because everything is a fraction, the bedroom looks the same on every screen size.
 */
class SceneLayout(context: Context, private val aspect: Float) : ViewGroup(context) {

    //left/top/width/height as fractions (0..1) of the stage's width and height
    private data class Place(val left: Float, val top: Float, val width: Float, val height: Float)

    private val places = HashMap<View, Place>()

    fun add(view: View, left: Float, top: Float, width: Float, height: Float) {
        places[view] = Place(left, top, width, height)
        addView(view)
    }

    //height fraction that keeps an image's own shape when it is [widthFraction] of the stage wide
    fun heightFor(widthFraction: Float, imageWidth: Int, imageHeight: Int): Float =
        widthFraction * imageHeight / imageWidth / aspect

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (w * aspect).toInt()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val place = places[child] ?: continue
            child.measure(
                MeasureSpec.makeMeasureSpec((place.width * w).toInt(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec((place.height * h).toInt(), MeasureSpec.EXACTLY)
            )
        }
        setMeasuredDimension(w, h)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val w = r - l
        val h = b - t
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val place = places[child] ?: continue
            val left = (place.left * w).toInt()
            val top = (place.top * h).toInt()
            child.layout(left, top, left + child.measuredWidth, top + child.measuredHeight)
        }
    }
}
