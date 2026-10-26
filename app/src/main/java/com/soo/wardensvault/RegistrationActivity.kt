// Code Attribution
// Based on: Display content edge-to-edge in views
// Author:   Android Developers
// URL:      https://developer.android.com/develop/ui/views/layout/edge-to-edge
// Accessed: 27 October 2026
// Used for: enableEdgeToEdge and window insets setup
//
// Based on: Authenticate with Firebase using Password-Based Accounts on Android
// Author:   Firebase
// URL:      https://firebase.google.com/docs/auth/android/password-auth
// Accessed: 27 October 2026
// Used for: Firebase sign-up, sign-in, current user and sign-out

package com.soo.wardensvault

import android.os.Bundle
import android.util.Patterns
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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException


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

        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        btnSubmit.setOnClickListener {
            //gets user input
            val fullName = findViewById<EditText>(R.id.etFullName).text.toString().trim()
            val username = findViewById<EditText>(R.id.etUsername).text.toString().trim()
            val email = findViewById<EditText>(R.id.etEmail).text.toString().trim()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            val confirm = findViewById<EditText>(R.id.etConfirmPassword).text.toString()

            //validate everything before touching the database
            val error = when {
                fullName.isEmpty() || username.isEmpty() || email.isEmpty() ||
                    password.isEmpty() || confirm.isEmpty() -> "Please fill in every field"
                username.contains("@") -> "Usernames can't contain @"
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email address"
                PasswordUtils.validateStrength(password) != null -> PasswordUtils.validateStrength(password)
                password != confirm -> "Passwords do not match"
                else -> null
            }
            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSubmit.isEnabled = false
            lifecycleScope.launch {
                try {
                    //checks if username is taken before creating the account
                    if (viewModel.isUsernameTaken(username)) {
                        Toast.makeText(
                            this@RegistrationActivity,
                            "Username already taken",
                            Toast.LENGTH_SHORT
                        ).show()
                        btnSubmit.isEnabled = true
                        return@launch
                    }
                    //creates the account, then its default categories while still signed in
                    val newId = viewModel.registerUser(fullName, username, email, password)
                    CategorySeeder.ensureDefaults(this@RegistrationActivity, newId)
                    //Firebase signs new users in automatically - sign out so they log in as before
                    SessionManager.clearSession(this@RegistrationActivity)
                    Toast.makeText(
                        this@RegistrationActivity,
                        "Registered successfully - you can log in now",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } catch (e: Exception) {
                    val message = when (e) {
                        is UsernameTakenException -> "Username already taken"
                        is FirebaseAuthUserCollisionException -> "An account with that email already exists"
                        is FirebaseAuthWeakPasswordException -> "Password is too weak"
                        is FirebaseNetworkException -> "No internet connection"
                        else -> "Registration failed: ${e.message}"
                    }
                    Toast.makeText(this@RegistrationActivity, message, Toast.LENGTH_LONG).show()
                    btnSubmit.isEnabled = true
                }
            }
        }
    }
}
