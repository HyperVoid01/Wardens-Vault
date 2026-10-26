package com.soo.wardensvault

/*
Password rules for new accounts.
Hashing and checking passwords is now done by Firebase Authentication, so the app never
stores or compares passwords itself - it only enforces a strength rule before sign-up.
 */
object PasswordUtils {

    //returns an error message, or null when the password meets the rules
    fun validateStrength(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters"
        if (!password.any { it.isLetter() }) return "Password must contain a letter"
        if (!password.any { it.isDigit() }) return "Password must contain a number"
        return null
    }
}
