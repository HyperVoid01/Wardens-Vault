package com.soo.wardensvault

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/*
Add Expense.
  - Category and amount are required. Description, times and a receipt photo are optional.
  - The date starts on today.
  - A one-off expense can have a start and end time.
  - A recurring expense has a start date, an optional end date and how often it repeats,
    instead of times.
 */
class CreateExpenseActivity : AppCompatActivity() {
    private lateinit var expenseViewModel: ExpenseViewModel
    private lateinit var categoryViewModel: CategoryViewModel
    private var userId: String = ""

    private lateinit var spCategory: Spinner
    private lateinit var spFrequency: Spinner
    private lateinit var cbRecurring: CheckBox
    private lateinit var btnPickDate: Button
    private lateinit var btnPickStartTime: Button
    private lateinit var btnPickEndTime: Button
    private lateinit var btnClearTimes: Button
    private lateinit var btnPickEndDate: Button
    private var categories: List<Category> = emptyList()

    private val frequencies = listOf("Weekly", "Monthly", "Yearly")

    //the date starts on today; times and the end date are optional
    private var selectedDateMillis: Long = DateUtils.startOfToday()
    private var startMinutes: Int? = null   //minutes after midnight, e.g. 9:30 = 570
    private var endMinutes: Int? = null
    private var endDateMillis: Long? = null

    //path of the captured photo file, set once the camera returns successfully
    private var pendingPhotoFile: File? = null
    private var savedPhotoPath: String? = null

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    //modern camera-capture API - launches the camera app, writes the photo to pendingPhotoFile's Uri
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPhotoFile != null) {
            showReceipt(pendingPhotoFile!!)
        } else {
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    //picks an existing image and copies it into the app's receipt folder
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val photoDir = File(getExternalFilesDir(null), "expense_photos")
            if (!photoDir.exists()) photoDir.mkdirs()
            val photoFile = File(photoDir, "expense_${System.currentTimeMillis()}.jpg")
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    photoFile.outputStream().use { output -> input.copyTo(output) }
                }
                showReceipt(photoFile)
            } catch (e: Exception) {
                Toast.makeText(this, "Couldn't load that image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    //remembers the receipt file and shows it in the preview
    private fun showReceipt(file: File) {
        savedPhotoPath = file.absolutePath
        val ivPreview = findViewById<ImageView>(R.id.ivPhotoPreview)
        ivPreview.visibility = View.VISIBLE
        ivPreview.setImageURI(Uri.fromFile(file))
        findViewById<Button>(R.id.btnRemovePhoto).visibility = View.VISIBLE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_expense)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        userId = SessionManager.getLoggedInUserId(this)
        if (userId.isEmpty()) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        BottomNav.attach(this, findViewById<BottomNavigationView>(R.id.bottomNavigationView), null)

        expenseViewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        categoryViewModel = ViewModelProvider(this)[CategoryViewModel::class.java]
        spCategory = findViewById(R.id.spCategory)
        spFrequency = findViewById(R.id.spFrequency)
        cbRecurring = findViewById(R.id.cbRecurring)
        btnPickDate = findViewById(R.id.btnPickDate)
        btnPickStartTime = findViewById(R.id.btnPickStartTime)
        btnPickEndTime = findViewById(R.id.btnPickEndTime)
        btnClearTimes = findViewById(R.id.btnClearTimes)
        btnPickEndDate = findViewById(R.id.btnPickEndDate)

        spFrequency.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, frequencies)
        spFrequency.setSelection(frequencies.indexOf("Monthly"))

        //pixel-art calculator for working out the amount
        val etAmount = findViewById<EditText>(R.id.etAmount)
        findViewById<Button>(R.id.btnCalculator).setOnClickListener {
            CalculatorDialog.show(this, etAmount.text.toString()) { result ->
                etAmount.setText(
                    if (result == Math.floor(result)) result.toLong().toString()
                    else String.format(Locale.US, "%.2f", result)
                )
            }
        }

        loadCategories()
        setupRecurringToggle()
        setupDatePickers()
        setupTimePickers()
        setupPhotoCapture()
        setupSave()
    }

    private fun loadCategories() {
        launchSafe {
            categories = categoryViewModel.getCategories(userId)
            if (categories.isEmpty()) {
                Toast.makeText(
                    this@CreateExpenseActivity,
                    "Add a category first",
                    Toast.LENGTH_LONG
                ).show()
            }
            spCategory.adapter = ArrayAdapter(
                this@CreateExpenseActivity,
                android.R.layout.simple_spinner_dropdown_item,
                categories.map { it.name }
            )
        }
    }

    //recurring: show how often + end date and hide the times; one-off: the opposite
    private fun setupRecurringToggle() {
        val tvDateLabel = findViewById<TextView>(R.id.tvDateLabel)
        val llFrequency = findViewById<View>(R.id.llFrequency)
        val llTimes = findViewById<View>(R.id.llTimes)
        val llEndDate = findViewById<View>(R.id.llEndDate)

        cbRecurring.setOnCheckedChangeListener { _, recurring ->
            tvDateLabel.text = if (recurring) "Start date" else "Date"
            llFrequency.visibility = if (recurring) View.VISIBLE else View.GONE
            llEndDate.visibility = if (recurring) View.VISIBLE else View.GONE
            llTimes.visibility = if (recurring) View.GONE else View.VISIBLE
        }
    }

    private fun setupDatePickers() {
        btnPickDate.text = dateFormat.format(selectedDateMillis)
        btnPickDate.setOnClickListener {
            pickDate(selectedDateMillis) { millis ->
                selectedDateMillis = millis
                btnPickDate.text = dateFormat.format(millis)
            }
        }

        btnPickEndDate.setOnClickListener {
            pickDate(endDateMillis ?: selectedDateMillis) { millis ->
                endDateMillis = millis
                btnPickEndDate.text = dateFormat.format(millis)
            }
        }
    }

    private fun pickDate(initial: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initial }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val chosen = Calendar.getInstance()
                chosen.set(year, month, dayOfMonth, 0, 0, 0)
                chosen.set(Calendar.MILLISECOND, 0)
                onPicked(chosen.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun setupTimePickers() {
        btnPickStartTime.setOnClickListener {
            pickTime(startMinutes) { minutes ->
                startMinutes = minutes
                btnPickStartTime.text = formatMinutes(minutes)
                btnClearTimes.visibility = View.VISIBLE
            }
        }

        btnPickEndTime.setOnClickListener {
            pickTime(endMinutes ?: startMinutes) { minutes ->
                endMinutes = minutes
                btnPickEndTime.text = formatMinutes(minutes)
                btnClearTimes.visibility = View.VISIBLE
            }
        }

        btnClearTimes.setOnClickListener {
            startMinutes = null
            endMinutes = null
            btnPickStartTime.text = "No time - tap to pick one"
            btnPickEndTime.text = "No time - tap to pick one"
            btnClearTimes.visibility = View.GONE
        }
    }

    private fun pickTime(initialMinutes: Int?, onPicked: (Int) -> Unit) {
        val now = Calendar.getInstance()
        val hour = initialMinutes?.div(60) ?: now.get(Calendar.HOUR_OF_DAY)
        val minute = initialMinutes?.rem(60) ?: now.get(Calendar.MINUTE)
        TimePickerDialog(
            this,
            { _, hourOfDay, minuteOfHour -> onPicked(hourOfDay * 60 + minuteOfHour) },
            hour,
            minute,
            true
        ).show()
    }

    private fun formatMinutes(minutes: Int): String =
        String.format(Locale.getDefault(), "%02d:%02d", minutes / 60, minutes % 60)

    //the chosen time on the chosen date, as epoch millis
    private fun timeOnDate(date: Long, minutes: Int): Long = date + minutes * 60_000L

    private fun setupPhotoCapture() {
        findViewById<Button>(R.id.btnTakePhoto).setOnClickListener {
            val photoDir = File(getExternalFilesDir(null), "expense_photos")
            if (!photoDir.exists()) photoDir.mkdirs()
            val photoFile = File(photoDir, "expense_${System.currentTimeMillis()}.jpg")
            pendingPhotoFile = photoFile

            val photoUri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            takePictureLauncher.launch(photoUri)
        }

        findViewById<Button>(R.id.btnPickPhoto).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        //remove (or replace by picking/taking another) receipt before saving
        findViewById<Button>(R.id.btnRemovePhoto).setOnClickListener {
            savedPhotoPath = null
            pendingPhotoFile = null
            findViewById<ImageView>(R.id.ivPhotoPreview).visibility = View.GONE
            findViewById<Button>(R.id.btnRemovePhoto).visibility = View.GONE
        }
    }

    private fun setupSave() {
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val btnSave = findViewById<Button>(R.id.btnSaveExpense)

        btnSave.setOnClickListener {
            val description = etDescription.text.toString().trim()
            val amount = etAmount.text.toString().trim().toDoubleOrNull()
            val recurring = cbRecurring.isChecked

            if (categories.isEmpty()) {
                Toast.makeText(this, "Add a category first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (amount == null || amount <= 0.0) {
                Toast.makeText(this, "Enter a valid amount greater than 0", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var startTime: Long? = null
            var endTime: Long? = null
            var endDate: Long? = null
            if (recurring) {
                endDate = endDateMillis
                if (endDate != null && endDate < selectedDateMillis) {
                    Toast.makeText(this, "The end date must be on or after the start date", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            } else {
                val start = startMinutes
                val end = endMinutes
                if (end != null && start == null) {
                    Toast.makeText(this, "Pick a start time too, or clear the times", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (start != null && end != null && end < start) {
                    Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                startTime = start?.let { timeOnDate(selectedDateMillis, it) }
                endTime = end?.let { timeOnDate(selectedDateMillis, it) }
            }

            val selectedCategory = categories[spCategory.selectedItemPosition]

            val expense = Expense(
                userId = userId,
                categoryId = selectedCategory.id,
                amount = amount,
                date = selectedDateMillis,
                startTime = startTime,
                endTime = endTime,
                description = description,
                photoPath = savedPhotoPath,
                recurring = recurring,
                frequency = if (recurring) spFrequency.selectedItem.toString() else "Monthly",
                endDate = endDate
            )

            btnSave.isEnabled = false
            launchSafe {
                try {
                    expenseViewModel.createExpense(expense)
                    //reward the user for logging the expense, then check achievements and budget alerts
                    val reward = GamificationManager(this@CreateExpenseActivity)
                        .onExpenseLogged(userId)
                    val achievements = AchievementManager.checkAll(this@CreateExpenseActivity, userId)
                    val alerts = AlertEngine.notifyNew(this@CreateExpenseActivity, userId)
                    var message = "Expense saved  +${reward.xpGained} XP  +${reward.goldGained} Gold"
                    if (reward.leveledUp) message += "\nLEVEL UP! Level ${reward.newLevel}"
                    for (a in achievements) message += "\nAchievement: ${a.name}"
                    //surface the most serious budget alert right away
                    val urgent = alerts.filter { it.settingKey == AppSettings.KEY_BUDGET }.maxByOrNull { it.severity }
                    if (urgent != null) message += "\n${urgent.title}"
                    Toast.makeText(
                        this@CreateExpenseActivity,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                } finally {
                    btnSave.isEnabled = true
                }
            }
        }
    }
}
