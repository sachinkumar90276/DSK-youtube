package com.example.data.security

import android.util.Patterns
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object AuthCrypto {
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH_BITS = 256
    private val random = SecureRandom()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray {
        check(length % 2 == 0) { "Invalid hex string" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun generateSalt(): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.toHex()
    }

    fun hashPassword(password: String, saltHex: String): String {
        val salt = saltHex.fromHex()
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val hash = factory.generateSecret(spec).encoded
        return hash.toHex()
    }

    fun verifyPassword(password: String, saltHex: String, expectedHashHex: String): Boolean {
        val computed = hashPassword(password, saltHex)
        return MessageDigest.isEqual(
            computed.toByteArray(Charsets.UTF_8),
            expectedHashHex.toByteArray(Charsets.UTF_8)
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
