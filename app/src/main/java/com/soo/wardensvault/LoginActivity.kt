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
import com.google.firebase.FirebaseNetworkException


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

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            //gets user input - either a username or an email address
            val username = findViewById<EditText>(R.id.etUsername).text.toString().trim()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter your username or email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            btnLogin.isEnabled = false //stops double-taps while Firebase is working
            //calls viewModel method to check details - runs in a coroutine
            lifecycleScope.launch {
                try {
                    val user = viewModel.loginUser(username, password)
                    //procedures to follow depending on result
                    if (user != null) {
                        SessionManager.saveLoggedInUser(this@LoginActivity, user.id, user.fullName)
                        CategorySeeder.ensureDefaults(this@LoginActivity, user.id)
                        //first login: run the initial setup once, then the dashboard
                        val setupDone = AppSettings.getFlag(this@LoginActivity, "setup_done_${user.id}")
                        val target = if (setupDone) HomeActivity::class.java else SetupActivity::class.java
                        val intent = Intent(this@LoginActivity, target)
                        intent.putExtra("fullName", user.fullName)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "Incorrect username or password", Toast.LENGTH_SHORT)
                            .show()
                        btnLogin.isEnabled = true
                    }
                } catch (e: FirebaseNetworkException) {
                    Toast.makeText(this@LoginActivity, "No internet connection", Toast.LENGTH_SHORT).show()
                    btnLogin.isEnabled = true
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                    btnLogin.isEnabled = true
                }
            }
        }

    }
}
