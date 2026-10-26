// Code Attribution
// Based on: Display content edge-to-edge in views
// Author:   Android Developers
// URL:      https://developer.android.com/develop/ui/views/layout/edge-to-edge
// Accessed: 27 October 2026
// Used for: enableEdgeToEdge and window insets setup
//
// Based on: Add pickers to your app
// Author:   Android Developers
// URL:      https://developer.android.com/develop/ui/views/components/pickers
// Accessed: 27 October 2026
// Used for: DatePickerDialog / TimePickerDialog
//
// Based on: Dialogs
// Author:   Android Developers
// URL:      https://developer.android.com/develop/ui/views/components/dialogs
// Accessed: 27 October 2026
// Used for: AlertDialog builder

package com.soo.wardensvault

import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

//One field in a popup form built by showForm()
data class FormField(
    val key: String,
    val label: String,
    val kind: Kind = Kind.TEXT,
    val initial: String = "",
    val options: List<String> = emptyList(),
    //optional DATE fields start empty; required ones start on today's date
    val optional: Boolean = false
) {
    //DATE values are epoch millis as text ("" = no date); CHOICE values are the chosen option
    enum class Kind { TEXT, NUMBER, DATE, CHOICE }
}

/*
Base class for the screens that are built in code. Subclasses implement render(),
which is re-run (after clearing the screen) every time the screen resumes or refresh() is called.
 */
abstract class ScreenActivity : AppCompatActivity() {
    //@JvmField: without it Kotlin generates getUserId(), which clashes with Android's hidden
    //Context.getUserId() and makes the system think the app runs as another phone user
    @JvmField
    protected var userId: String = ""
    protected lateinit var content: LinearLayout
    protected lateinit var db: AppDatabase
    protected lateinit var gamification: GamificationManager
    protected val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var renderJob: Job? = null

    protected abstract suspend fun render()

    //screens shown before the user reaches the app (e.g. first-time setup) hide the bar
    protected open val showBottomNav: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userId = SessionManager.getLoggedInUserId(this)
        if (userId.isEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        db = AppDatabase.getDatabase(this)
        gamification = GamificationManager(this)

        //no background here: the brick wall is drawn on the window (see WallDrawable)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            //gap between the content and the edges of the screen
            val side = UiKit.dp(this@ScreenActivity, 20)
            setPadding(side, UiKit.dp(this@ScreenActivity, 24), side, UiKit.dp(this@ScreenActivity, 24))
        }
        scroll.addView(
            content,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        root.addView(
            scroll,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        )
        if (showBottomNav) {
            val nav = BottomNav.create(this)
            root.addView(nav)
            BottomNav.attach(this, nav, null)
        }
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        if (userId.isNotEmpty()) refresh()
    }

    protected fun refresh() {
        renderJob?.cancel()
        renderJob = launchSafe {
            content.removeAllViews()
            render()
        }
    }

    // ---- view helpers ----

    //screen title on the wooden plaque (TextBox.png)
    protected fun addTitle(title: String) {
        content.addView(UiKit.title(this, title))
    }

    //full-width pixel-art picture (e.g. the Castle window) with rounded corners
    protected fun addBanner(resId: Int): ImageView {
        val image = ImageView(this).apply {
            setImageDrawable(UiKit.crisp(this@ScreenActivity, resId))
            adjustViewBounds = true                       //height follows the picture's shape
            scaleType = ImageView.ScaleType.FIT_XY
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = UiKit.dp(this@ScreenActivity, 12) }
        }
        UiKit.roundCorners(image, UiKit.dp(this, 12).toFloat())
        content.addView(image)
        return image
    }

    protected fun addHeader(title: String) {
        content.addView(UiKit.text(this, title, UiKit.GOLD, 18f, true).apply {
            (layoutParams as LinearLayout.LayoutParams).topMargin = UiKit.dp(this@ScreenActivity, 18)
        })
    }

    protected fun addText(text: String, color: Int = Color.WHITE, sizeSp: Float = 16f, bold: Boolean = false): TextView {
        val tv = UiKit.text(this, text, color, sizeSp, bold)
        content.addView(tv)
        return tv
    }

    protected fun addButton(label: String, onClick: () -> Unit): Button {
        val b = UiKit.button(this, label, onClick)
        content.addView(b)
        return b
    }

    //label above an input box, for screens built in code
    protected fun addLabeledInput(label: String, inputType: Int): EditText {
        content.addView(UiKit.label(this, label))
        val et = EditText(this)
        et.inputType = inputType
        et.setTextColor(Color.WHITE)
        et.setBackgroundResource(R.drawable.bg_input)
        val pad = UiKit.dp(this, 12)
        et.setPadding(pad, pad, pad, pad)
        et.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = UiKit.dp(this@ScreenActivity, 6) }
        content.addView(et)
        return et
    }

    protected fun addCard(): LinearLayout {
        val card = UiKit.card(this)
        content.addView(card)
        return card
    }

    protected fun LinearLayout.addLine(text: String, color: Int = Color.WHITE, sizeSp: Float = 15f, bold: Boolean = false) {
        addView(UiKit.text(this@ScreenActivity, text, color, sizeSp, bold))
    }

    protected fun LinearLayout.addBar(percent: Int, color: Int) {
        addView(UiKit.progress(this@ScreenActivity, percent, color))
    }

    protected fun money(value: Double): String = UiKit.money(value)

    protected fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    //shows achievements / level-ups that just happened
    protected fun announce(achievements: List<AchievementDef>) {
        for (a in achievements) {
            Toast.makeText(this, "Achievement unlocked: ${a.name}", Toast.LENGTH_LONG).show()
        }
    }

    //valid, non-negative number or null
    protected fun parseAmount(text: String?): Double? {
        val value = text?.trim()?.toDoubleOrNull() ?: return null
        return if (value < 0) null else value
    }

    protected fun confirm(title: String, message: String, onYes: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Yes") { _, _ -> onYes() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    //simple list of actions in a dialog
    protected fun showOptions(title: String, labels: List<String>, onPick: (Int) -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(labels.toTypedArray()) { _, which -> onPick(which) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /*
    Popup form. onSubmit gets the entered values and returns an error message to show
    (the form stays open) or null when saved (the form closes and the screen refreshes).
     */
    protected fun showForm(
        title: String,
        fields: List<FormField>,
        okLabel: String = "Save",
        onSubmit: suspend (Map<String, String>) -> String?
    ) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = UiKit.dp(this@ScreenActivity, 20)
            setPadding(pad, UiKit.dp(this@ScreenActivity, 8), pad, 0)
        }
        val getters = LinkedHashMap<String, () -> String>()

        for (field in fields) {
            layout.addView(TextView(this).apply {
                text = field.label
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, UiKit.dp(this@ScreenActivity, 12), 0, 0)
            })
            when (field.kind) {
                FormField.Kind.TEXT, FormField.Kind.NUMBER -> {
                    val et = EditText(this)
                    et.setText(field.initial)
                    et.inputType = if (field.kind == FormField.Kind.NUMBER) {
                        InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                    } else {
                        InputType.TYPE_CLASS_TEXT
                    }
                    layout.addView(et)
                    getters[field.key] = { et.text.toString().trim() }
                }
                FormField.Kind.DATE -> {
                    //required dates start on today so the user rarely has to pick one
                    var picked: Long? = field.initial.toLongOrNull()
                        ?: if (field.optional) null else DateUtils.startOfToday()
                    val btn = Button(this)
                    val emptyText = if (field.optional) "No date - tap to pick one" else "Tap to pick a date"
                    btn.text = picked?.let { dateFormat.format(it) } ?: emptyText
                    btn.setOnClickListener {
                        val cal = Calendar.getInstance()
                        picked?.let { cal.timeInMillis = it }
                        DatePickerDialog(
                            this,
                            { _, year, month, day ->
                                val chosen = Calendar.getInstance()
                                chosen.set(year, month, day, 0, 0, 0)
                                chosen.set(Calendar.MILLISECOND, 0)
                                picked = chosen.timeInMillis
                                btn.text = dateFormat.format(chosen.time)
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                    layout.addView(btn)
                    getters[field.key] = { picked?.toString() ?: "" }
                }
                FormField.Kind.CHOICE -> {
                    val spinner = Spinner(this)
                    spinner.adapter = ArrayAdapter(
                        this, android.R.layout.simple_spinner_dropdown_item, field.options
                    )
                    val index = field.options.indexOf(field.initial)
                    if (index >= 0) spinner.setSelection(index)
                    layout.addView(spinner)
                    getters[field.key] = { spinner.selectedItem?.toString() ?: "" }
                }
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(ScrollView(this).apply { addView(layout) })
            .setPositiveButton(okLabel, null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val values = HashMap<String, String>()
                for ((key, getter) in getters) values[key] = getter()
                launchSafe {
                    val error = onSubmit(values)
                    if (error == null) {
                        dialog.dismiss()
                        refresh()
                    } else {
                        toast(error)
                    }
                }
            }
        }
        dialog.show()
    }

    protected fun goTo(target: Class<*>) {
        startActivity(Intent(this, target))
    }

    protected fun View.margin(topDp: Int) {
        (layoutParams as? LinearLayout.LayoutParams)?.topMargin = UiKit.dp(this@ScreenActivity, topDp)
    }
}