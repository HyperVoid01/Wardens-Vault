package com.soo.wardensvault

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider


class RegistrationActivity : AppCompatActivity() {
    //declaring view model
    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registration)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //Creates instance of UserViewsModel
        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            //gets user input
            val fullName = findViewById<EditText>(R.id.etFullName).text.toString()
            val username = findViewById<EditText>(R.id.etUsername).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()

            //checks if user exists
            lifecycleScope.launch {
                if (viewModel.isUsernameTaken(username)) {
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Username already taken",
                        Toast.LENGTH_SHORT
                    ).show()
                    //creates new user and inserts into DB
                } else {
                    viewModel.registerUser(
                        User(
                            fullName = fullName,
                            username = username,
                            password = password
                        )
                    )
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Registered successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }
}