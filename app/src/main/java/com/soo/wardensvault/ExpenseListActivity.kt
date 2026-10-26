package com.soo.wardensvault

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseListActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var lvExpenses: ListView
    private lateinit var tvPeriod: TextView
    private lateinit var tvTotal: TextView
    private lateinit var spFilterCategory: Spinner
    private var userId: String = ""

    //null = show every category
    private var filterCategory: String? = null

    //defaults to the current calendar month until the user picks their own range
    private var fromMillis: Long = DateUtils.startOfCurrentMonth()
    private var toMillis: Long = DateUtils.endOfToday()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expense_list)
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

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        lvExpenses = findViewById(R.id.lvExpenses)
        tvPeriod = findViewById(R.id.tvPeriod)
        tvTotal = findViewById(R.id.tvTotal)

        spFilterCategory = findViewById(R.id.spFilterCategory)
        setupCategoryFilter()

        updatePeriodLabel()

        findViewById<Button>(R.id.btnPickFrom).setOnClickListener {
            pickDate(fromMillis) { millis ->
                fromMillis = DateUtils.startOfDay(millis)
                updatePeriodLabel()
            }
        }

        findViewById<Button>(R.id.btnPickTo).setOnClickListener {
            pickDate(toMillis) { millis ->
                toMillis = DateUtils.endOfDay(millis)
                updatePeriodLabel()
            }
        }

        findViewById<Button>(R.id.btnLoad).setOnClickListener {
            loadExpenses()
        }

        //load the current month's expenses immediately on open
        loadExpenses()
    }

    private fun pickDate(currentMillis: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = currentMillis }
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                onPicked(cal.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    //the From / To buttons show the chosen dates
    private fun updatePeriodLabel() {
        findViewById<Button>(R.id.btnPickFrom).text = dateFormat.format(fromMillis)
        findViewById<Button>(R.id.btnPickTo).text = dateFormat.format(toMillis)
        val days = DateUtils.daysBetween(fromMillis, toMillis) + 1
        tvPeriod.text = "$days day(s) selected"
    }

    private fun loadExpenses() {
        if (fromMillis > toMillis) {
            Toast.makeText(this, "'From' date must be before 'To' date", Toast.LENGTH_SHORT)
                .show()
            return
        }
        launchSafe {
            val all = viewModel.getExpensesWithCategoryForPeriod(userId, fromMillis, toMillis)
            val category = filterCategory
            val expenses = if (category == null) all else all.filter { it.categoryName == category }
            val total = expenses.sumOf { it.amount }
            tvTotal.text = "Total: R${String.format(Locale.getDefault(), "%.2f", total)}"
            lvExpenses.adapter = ExpenseListAdapter(expenses)
        }
    }

    //spinner of "All categories" + the user's categories; changing it reloads the list
    private fun setupCategoryFilter() {
        launchSafe {
            val categories = AppDatabase.getDatabase(this@ExpenseListActivity)
                .categoryDao().getCategoriesForUser(userId)
            val options = listOf("All categories") + categories.map { it.name }
            spFilterCategory.adapter = ArrayAdapter(
                this@ExpenseListActivity,
                android.R.layout.simple_spinner_dropdown_item,
                options
            )
            spFilterCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    filterCategory = if (position == 0) null else options[position]
                    loadExpenses()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    //date, plus the times (if given) or how often it repeats (if recurring)
    private fun whenText(item: ExpenseWithCategory): String {
        val date = dateFormat.format(item.date)
        if (item.recurring) {
            val until = item.endDate?.let { " until ${dateFormat.format(it)}" } ?: ""
            return "$date  |  Repeats ${item.frequency.lowercase()}$until"
        }
        val start = item.startTime ?: return date
        val end = item.endTime
        return if (end != null) "$date, ${timeFormat.format(start)} - ${timeFormat.format(end)}"
        else "$date, ${timeFormat.format(start)}"
    }

    //deleting an expense always asks first
    private fun confirmDelete(item: ExpenseWithCategory) {
        val name = item.description.ifEmpty { item.categoryName }
        val amount = "R${String.format(Locale.getDefault(), "%.2f", item.amount)}"
        val message = if (item.recurring) {
            "Delete the recurring expense \"$name\" ($amount)? Every repeat of it will be removed."
        } else {
            "Delete \"$name\" ($amount)?"
        }
        AlertDialog.Builder(this)
            .setTitle("Delete expense")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                launchSafe {
                    AppDatabase.getDatabase(this@ExpenseListActivity).expenseDao().deleteExpense(userId, item.id)
                    loadExpenses()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPhotoDialog(photoPath: String) {
        val file = File(photoPath)
        if (!file.exists()) {
            Toast.makeText(this, "Photo file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val imageView = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeFile(photoPath))
            adjustViewBounds = true
        }
        AlertDialog.Builder(this)
            .setTitle("Expense Photo")
            .setView(imageView)
            .setPositiveButton("Close", null)
            .show()
    }

    //simple custom adapter - each row shows category, description, amount, date/time,
    //and a "Photo" button that only appears when photoPath is non-null
    private inner class ExpenseListAdapter(
        private val items: List<ExpenseWithCategory>
    ) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@ExpenseListActivity)
                .inflate(R.layout.item_expense, parent, false)
            val item = items[position]

            view.findViewById<TextView>(R.id.tvCategoryName).text = item.categoryName
            //description is optional, so hide the line when it's empty
            val tvDescription = view.findViewById<TextView>(R.id.tvDescription)
            tvDescription.text = item.description
            tvDescription.visibility = if (item.description.isEmpty()) View.GONE else View.VISIBLE

            view.findViewById<TextView>(R.id.tvDateTime).text =
                whenText(item) + (if (item.photoPath != null) "  |  Receipt attached" else "")
            view.findViewById<TextView>(R.id.tvAmount).text =
                "R${String.format(Locale.getDefault(), "%.2f", item.amount)}"

            val btnPhoto = view.findViewById<Button>(R.id.btnViewPhoto)
            if (item.photoPath != null) {
                btnPhoto.visibility = View.VISIBLE
                btnPhoto.setOnClickListener { showPhotoDialog(item.photoPath) }
            } else {
                btnPhoto.visibility = View.GONE
                btnPhoto.setOnClickListener(null)
            }

            //tap = view the receipt, long-press = delete (with confirmation)
            view.setOnClickListener {
                if (item.photoPath != null) {
                    showPhotoDialog(item.photoPath)
                } else {
                    Toast.makeText(this@ExpenseListActivity, "No receipt attached", Toast.LENGTH_SHORT).show()
                }
            }
            view.setOnLongClickListener {
                confirmDelete(item)
                true
            }

            return view
        }
    }
}
