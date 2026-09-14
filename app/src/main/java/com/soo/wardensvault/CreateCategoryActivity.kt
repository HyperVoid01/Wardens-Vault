package com.soo.wardensvault

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class CreateCategoryActivity : AppCompatActivity() {
    private lateinit var viewModel: CategoryViewModel
    private lateinit var lvCategories: ListView
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_create_category)
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

        viewModel = ViewModelProvider(this)[CategoryViewModel::class.java]
        lvCategories = findViewById(R.id.lvCategories)

        val etCategoryName = findViewById<EditText>(R.id.etCategoryName)

        findViewById<Button>(R.id.btnAddCategory).setOnClickListener {
            val name = etCategoryName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(this, "Enter a category name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            etCategoryName.text.clear()
            lifecycleScope.launch {
                //awaited in order: insert completes, then the list is refreshed
                viewModel.createCategory(userId, name)
                refreshCategories()
            }
        }

        lifecycleScope.launch {
            refreshCategories()
        }
    }

    private suspend fun refreshCategories() {
        val categories = viewModel.getCategories(userId)
        val names = categories.map { it.name }
        lvCategories.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            names
        )
    }
}
