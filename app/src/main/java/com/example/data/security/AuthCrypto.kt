package com.example.data.security

import android.util.Base64
import android.util.Patterns
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AuthCrypto {
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH_BITS = 256
    private val random = SecureRandom()

    fun generateSalt(): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    fun hashPassword(password: String, saltBase64: String): String {
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return Base64.encodeToString(hash, Base64.NO_WRAP)
    }

    fun verifyPassword(password: String, saltBase64: String, expectedHashBase64: String): Boolean {
        val computed = hashPassword(password, saltBase64)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            expectedHashBase64.toByteArray(Charsets.UTF_8)
        )
    }

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    }

    fun isValidPassword(password: String): Boolean = password.length >= 6

    fun isValidMediaUrl(url: String): Boolean {
        val trimmed = url.trim().lowercase()
        return trimmed.startsWith("https://") ||
            trimmed.startsWith("http://") ||
            trimmed.startsWith("content://") ||
            trimmed.startsWith("file://")
    }
}
