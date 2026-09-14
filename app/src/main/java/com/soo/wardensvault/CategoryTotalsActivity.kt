package com.soo.wardensvault

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CategoryTotalsActivity : AppCompatActivity() {
    private lateinit var viewModel: ExpenseViewModel
    private lateinit var lvCategoryTotals: ListView
    private lateinit var tvPeriod: TextView
    private lateinit var tvGrandTotal: TextView
    private var userId: Int = -1

    //defaults to the current calendar month, same convention as the expense list screen
    private var fromMillis: Long = DateUtils.startOfCurrentMonth()
    private var toMillis: Long = DateUtils.endOfToday()

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_category_totals)
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
        lvCategoryTotals = findViewById(R.id.lvCategoryTotals)
        tvPeriod = findViewById(R.id.tvPeriod)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)

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
            loadTotals()
        }

        //load the current month's totals immediately on open
        loadTotals()
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

    private fun loadTotals() {
        if (fromMillis > toMillis) {
            Toast.makeText(this, "'From' date must be before 'To' date", Toast.LENGTH_SHORT)
                .show()
            return
        }
        lifecycleScope.launch {
            val totals = viewModel.getCategoryTotalsForPeriod(userId, fromMillis, toMillis)
            val grandTotal = totals.sumOf { it.total }
            tvGrandTotal.text =
                "Grand Total: R${String.format(Locale.getDefault(), "%.2f", grandTotal)}"
            lvCategoryTotals.adapter = CategoryTotalAdapter(totals)
        }
    }

    //each row: category name + how much was spent in it over the selected period
    //(categories with no expenses in range still show, at $0.00 - COALESCE in the
    //DAO query guarantees that rather than silently omitting them)
    private inner class CategoryTotalAdapter(
        private val items: List<CategoryTotal>
    ) : BaseAdapter() {
        override fun getCount() = items.size
        override fun getItem(position: Int) = items[position]
        override fun getItemId(position: Int) = items[position].categoryId.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(this@CategoryTotalsActivity)
                .inflate(R.layout.item_category_total, parent, false)
            val item = items[position]

            view.findViewById<TextView>(R.id.tvCategoryName).text = item.categoryName
            view.findViewById<TextView>(R.id.tvCategoryTotal).text =
                "R${String.format(Locale.getDefault(), "%.2f", item.total)}"

            return view
        }
    }
}
