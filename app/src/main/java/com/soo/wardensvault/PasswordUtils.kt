package com.soo.wardensvault

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/*
Password hashing - passwords are never stored as plain text.
Stored format: "pbkdf2$<base64 salt>$<base64 hash>"
Accounts created before hashing was added still hold plain text; verify() accepts
them once and the login screen then upgrades them to a hash.
 */
object PasswordUtils {
    private const val PREFIX = "pbkdf2$"
    private const val ITERATIONS = 10000
    private const val KEY_BITS = 256

    fun hash(password: String): String {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val hash = derive(password, salt)
        return PREFIX + Base64.encodeToString(salt, Base64.NO_WRAP) + "$" +
            Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun isHashed(stored: String): Boolean = stored.startsWith(PREFIX)

    fun verify(password: String, stored: String): Boolean {
        if (!isHashed(stored)) return password == stored   // legacy plain-text account
        val parts = stored.removePrefix(PREFIX).split("$")
        if (parts.size != 2) return false
        val salt = Base64.decode(parts[0], Base64.NO_WRAP)
        val expected = Base64.decode(parts[1], Base64.NO_WRAP)
        val actual = derive(password, salt)
        return java.security.MessageDigest.isEqual(expected, actual)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    //returns an error message, or null when the password meets the rules
    fun validateStrength(password: String): String? {
        if (password.length < 8) return "Password must be at least 8 characters"
        if (!password.any { it.isLetter() }) return "Password must contain a letter"
        if (!password.any { it.isDigit() }) return "Password must contain a number"
        return null
    }
}
