package com.soo.wardensvault

import android.content.Intent
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


class LoginActivity : AppCompatActivity() {
    //declaring view model
    private lateinit var viewModel: UserViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Creates instance of UserViewsModel
        viewModel = ViewModelProvider(this)[UserViewModel::class.java]

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            //gets user input
            val username = findViewById<EditText>(R.id.etUsername).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            //calls viewModel method to check details - runs in a coroutine
            lifecycleScope.launch {
                val user = viewModel.loginUser(username, password)
                //procedures to follow depending on result
                if (user != null) {
                    SessionManager.saveLoggedInUser(this@LoginActivity, user.id, user.fullName)
                    val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                    intent.putExtra("fullName", user.fullName)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

    }
}