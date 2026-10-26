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
    val options: List<String> = emptyList()
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
    protected var userId: Int = -1
    protected lateinit var content: LinearLayout
    protected lateinit var db: AppDatabase
    protected lateinit var gamification: GamificationManager
    protected val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private var renderJob: Job? = null

    protected abstract suspend fun render()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userId = SessionManager.getLoggedInUserId(this)
        if (userId == -1) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        db = AppDatabase.getDatabase(this)
        gamification = GamificationManager(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.ic_wall)
        }
        val scroll = ScrollView(this)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = UiKit.dp(this@ScreenActivity, 20)
            setPadding(pad, pad, pad, pad)
        }
        scroll.addView(
            content,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        root.addView(
            scroll,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        setContentView(root)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        if (userId != -1) refresh()
    }

    protected fun refresh() {
        renderJob?.cancel()
        renderJob = lifecycleScope.launch {
            content.removeAllViews()
            render()
        }
    }

    // ---- view helpers ----

    protected fun addTitle(title: String) {
        content.addView(UiKit.text(this, title, UiKit.GOLD, 26f, true))
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
                setPadding(0, UiKit.dp(this@ScreenActivity, 10), 0, 0)
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
                    var picked: Long? = field.initial.toLongOrNull()
                    val btn = Button(this)
                    btn.text = picked?.let { dateFormat.format(it) } ?: "Pick a date (optional)"
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
                lifecycleScope.launch {
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