package com.soo.wardensvault

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateExpenseActivity : AppCompatActivity() {
    private lateinit var expenseViewModel: ExpenseViewModel
    private lateinit var categoryViewModel: CategoryViewModel
    private var userId: Int = -1

    private lateinit var spCategory: Spinner
    private var categories: List<Category> = emptyList()

    //selected date/start/end held as epoch millis once picked
    private var selectedDateMillis: Long? = null
    private var selectedStartMillis: Long? = null
    private var selectedEndMillis: Long? = null

    //path of the captured photo file, set once the camera returns successfully
    private var pendingPhotoFile: File? = null
    private var savedPhotoPath: String? = null

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    //modern camera-capture API - launches the camera app, writes the photo to pendingPhotoFile's Uri
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingPhotoFile != null) {
            savedPhotoPath = pendingPhotoFile!!.absolutePath
            val ivPreview = findViewById<ImageView>(R.id.ivPhotoPreview)
            ivPreview.visibility = android.view.View.VISIBLE
            ivPreview.setImageURI(Uri.fromFile(pendingPhotoFile))
        } else {
            Toast.makeText(this, "Photo capture cancelled", Toast.LENGTH_SHORT).show()
        }
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
        if (userId == -1) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        expenseViewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        categoryViewModel = ViewModelProvider(this)[CategoryViewModel::class.java]
        spCategory = findViewById(R.id.spCategory)

        loadCategories()
        setupDatePicker()
        setupTimePickers()
        setupPhotoCapture()
        setupSave()
    }

    private fun loadCategories() {
        lifecycleScope.launch {
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

    private fun setupDatePicker() {
        val tvDate = findViewById<TextView>(R.id.tvDate)
        findViewById<Button>(R.id.btnPickDate).setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    cal.set(year, month, dayOfMonth, 0, 0, 0)
                    selectedDateMillis = cal.timeInMillis
                    tvDate.text = dateFormat.format(cal.time)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTimePickers() {
        val tvStart = findViewById<TextView>(R.id.tvStartTime)
        val tvEnd = findViewById<TextView>(R.id.tvEndTime)

        findViewById<Button>(R.id.btnPickStartTime).setOnClickListener {
            pickTime { millis ->
                selectedStartMillis = millis
                tvStart.text = timeFormat.format(millis)
            }
        }

        findViewById<Button>(R.id.btnPickEndTime).setOnClickListener {
            pickTime { millis ->
                selectedEndMillis = millis
                tvEnd.text = timeFormat.format(millis)
            }
        }
    }

    private fun pickTime(onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                cal.set(Calendar.MINUTE, minute)
                cal.set(Calendar.SECOND, 0)
                onPicked(cal.timeInMillis)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        ).show()
    }

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
    }

    private fun setupSave() {
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etAmount = findViewById<EditText>(R.id.etAmount)

        findViewById<Button>(R.id.btnSaveExpense).setOnClickListener {
            val description = etDescription.text.toString().trim()
            val amountText = etAmount.text.toString().trim()

            if (categories.isEmpty()) {
                Toast.makeText(this, "Add a category first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (description.isEmpty()) {
                Toast.makeText(this, "Enter a description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amount = amountText.toDoubleOrNull()
            if (amount == null) {
                Toast.makeText(this, "Enter a valid amount", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val date = selectedDateMillis
            val start = selectedStartMillis
            val end = selectedEndMillis
            if (date == null || start == null || end == null) {
                Toast.makeText(this, "Pick a date, start time, and end time", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (end < start) {
                Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            val selectedCategory = categories[spCategory.selectedItemPosition]

            val expense = Expense(
                userId = userId,
                categoryId = selectedCategory.id,
                amount = amount,
                date = date,
                startTime = start,
                endTime = end,
                description = description,
                photoPath = savedPhotoPath
            )

            lifecycleScope.launch {
                expenseViewModel.createExpense(expense)
                Toast.makeText(
                    this@CreateExpenseActivity,
                    "Expense saved",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }
}
