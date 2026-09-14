package com.soo.wardensvault

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
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
    private var userId: Int = -1

    //defaults to the current calendar month until the user picks their own range
    private var fromMillis: Long = startOfCurrentMonth()
    private var toMillis: Long = endOfToday()

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
        if (userId == -1) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        viewModel = ViewModelProvider(this)[ExpenseViewModel::class.java]
        lvExpenses = findViewById(R.id.lvExpenses)
        tvPeriod = findViewById(R.id.tvPeriod)
        tvTotal = findViewById(R.id.tvTotal)

        updatePeriodLabel()

        findViewById<Button>(R.id.btnPickFrom).setOnClickListener {
            pickDate(fromMillis) { millis ->
                fromMillis = startOfDay(millis)
                updatePeriodLabel()
            }
        }

        findViewById<Button>(R.id.btnPickTo).setOnClickListener {
            pickDate(toMillis) { millis ->
                toMillis = endOfDay(millis)
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

    private fun updatePeriodLabel() {
        tvPeriod.text = "${dateFormat.format(fromMillis)} - ${dateFormat.format(toMillis)}"
    }

    private fun loadExpenses() {
        if (fromMillis > toMillis) {
            Toast.makeText(this, "'From' date must be before 'To' date", Toast.LENGTH_SHORT)
                .show()
            return
        }
        lifecycleScope.launch {
            val expenses = viewModel.getExpensesWithCategoryForPeriod(userId, fromMillis, toMillis)
            val total = viewModel.getTotalSpentForPeriod(userId, fromMillis, toMillis)
            tvTotal.text = "Total: R${String.format(Locale.getDefault(), "%.2f", total)}"
            lvExpenses.adapter = ExpenseListAdapter(expenses)
        }
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

    private fun startOfCurrentMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return startOfDay(cal.timeInMillis)
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun endOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    private fun endOfToday(): Long = endOfDay(System.currentTimeMillis())

    //simple custom adapter - each row shows category, description, amount, date/time,
    //and a "Photo" button that only appears when photoPath is non-null
    private inner class ExpenseListAdapter(
        private val items: List<ExpenseWithCategory>
    ) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].id.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@ExpenseListActivity)
                .inflate(R.layout.item_expense, parent, false)
            val item = items[position]

            view.findViewById<TextView>(R.id.tvCategoryName).text = item.categoryName
            view.findViewById<TextView>(R.id.tvDescription).text = item.description
            view.findViewById<TextView>(R.id.tvDateTime).text =
                "${dateFormat.format(item.date)}, ${timeFormat.format(item.startTime)} - " +
                timeFormat.format(item.endTime)
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

            return view
        }
    }
}
