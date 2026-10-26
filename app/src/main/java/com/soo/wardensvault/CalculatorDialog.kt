// Code Attribution
// Based on: Dialogs
// Author:   Android Developers
// URL:      https://developer.android.com/develop/ui/views/components/dialogs
// Accessed: 27 October 2026
// Used for: AlertDialog builder

package com.soo.wardensvault

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import java.math.BigDecimal
import java.math.RoundingMode

/*
A pixel-art calculator for working out an amount (e.g. adding up a few receipts),
drawn with the number tiles from All Numbers.png.

  - It works left to right like a simple pocket calculator: 12 + 30 x 2 = 84
  - % turns the current number into a percentage (50 % -> 0.5)
  - "Use amount" sends the result back to the amount box.
 */
object CalculatorDialog {

    //key label -> tile picture (DEL uses the blank tile with text on it)
    private val keys = listOf(
        listOf("7", "8", "9", "÷", "AC"),
        listOf("4", "5", "6", "×", "DEL"),
        listOf("1", "2", "3", "−", "%"),
        listOf(".", "0", "=", "+", "")
    )

    private fun tileFor(key: String): Int = when (key) {
        "0" -> R.drawable.key_0
        "1" -> R.drawable.key_1
        "2" -> R.drawable.key_2
        "3" -> R.drawable.key_3
        "4" -> R.drawable.key_4
        "5" -> R.drawable.key_5
        "6" -> R.drawable.key_6
        "7" -> R.drawable.key_7
        "8" -> R.drawable.key_8
        "9" -> R.drawable.key_9
        "." -> R.drawable.key_point
        "+" -> R.drawable.key_plus
        "−" -> R.drawable.key_minus
        "×" -> R.drawable.key_times
        "÷" -> R.drawable.key_divide
        "=" -> R.drawable.key_equals
        "AC" -> R.drawable.key_clear
        "%" -> R.drawable.key_percent
        else -> R.drawable.key_blank
    }

    fun show(context: Context, initial: String, onUse: (Double) -> Unit) {
        val calc = Calculator(initial.toDoubleOrNull()?.takeIf { it > 0 })

        val display = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 26f
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_input)
            val pad = UiKit.dp(context, 12)
            setPadding(pad, pad, pad, pad)
            text = calc.display()
        }

        val pad = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val p = UiKit.dp(context, 16)
            setPadding(p, p, p, 0)
            addView(display, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }

        for (row in keys) {
            val line = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            for (key in row) {
                val cell = FrameLayout(context)
                if (key.isNotEmpty()) {
                    cell.addView(ImageView(context).apply {
                        setImageDrawable(UiKit.crisp(context, tileFor(key)))
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        adjustViewBounds = true
                    })
                    if (key == "DEL") {
                        cell.addView(TextView(context).apply {
                            text = "DEL"
                            setTextColor(Color.parseColor("#8FA6D9"))
                            textSize = 14f
                            setTypeface(typeface, android.graphics.Typeface.BOLD)
                            gravity = Gravity.CENTER
                        }, FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                        ))
                    }
                    cell.setOnClickListener {
                        calc.press(key)
                        display.text = calc.display()
                    }
                }
                val m = UiKit.dp(context, 2)
                line.addView(cell, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    setMargins(m, m, m, m)
                })
            }
            pad.addView(line)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("Calculator")
            .setView(pad)
            .setPositiveButton("Use amount", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val result = calc.result()
                if (result == null || result <= 0.0) {
                    display.text = "Enter an amount"
                } else {
                    onUse(result)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    //simple left-to-right calculator state
    private class Calculator(start: Double?) {
        private var total: Double? = start   //result so far
        private var op: String? = null        //operator waiting for its second number
        private var entry = ""                //number being typed
        private var error = false

        fun press(key: String) {
            if (error && key != "AC") return
            when (key) {
                "AC" -> { total = null; op = null; entry = ""; error = false }
                "DEL" -> if (entry.isNotEmpty()) entry = entry.dropLast(1)
                "." -> if (!entry.contains(".")) entry = if (entry.isEmpty()) "0." else "$entry."
                "%" -> {
                    val value = entry.toDoubleOrNull() ?: return
                    entry = format(value / 100)
                }
                "+", "−", "×", "÷" -> {
                    commit()
                    if (total != null) op = key
                }
                "=" -> {
                    commit()
                    op = null
                }
                else -> if (entry.length < 12) {   //a digit
                    if (op == null && entry.isEmpty() && total != null) total = null   //start fresh after =
                    entry = if (entry == "0") key else entry + key
                }
            }
        }

        //folds the typed number into the total using the waiting operator
        private fun commit() {
            val value = entry.toDoubleOrNull() ?: return
            val current = total
            val pending = op
            total = if (current == null || pending == null) value else when (pending) {
                "+" -> current + value
                "−" -> current - value
                "×" -> current * value
                else -> if (value == 0.0) { error = true; current } else current / value
            }
            entry = ""
        }

        fun result(): Double? {
            commit()
            op = null
            return if (error) null else total?.let { round2(it) }
        }

        fun display(): String {
            if (error) return "Can't divide by 0"
            val parts = listOfNotNull(total?.let { format(it) }, op, entry.ifEmpty { null })
            return if (parts.isEmpty()) "0" else parts.joinToString(" ")
        }

        private fun round2(value: Double): Double =
            BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()

        private fun format(value: Double): String {
            val rounded = round2(value)
            return if (rounded == Math.floor(rounded)) rounded.toLong().toString() else rounded.toString()
        }
    }
}
