package com.vigyan.juniorcollege.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Local-only password/PIN hashing (PBKDF2WithHmacSHA256).
 * Stored format: base64(salt):base64(hash)
 */
object PasswordHasher {
    private const val ITERATIONS = 12000
    private const val KEY_LENGTH = 256

    fun hash(rawPassword: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(rawPassword.toCharArray(), salt)
        return "${b64(salt)}:${b64(hash)}"
    }

    fun verify(rawPassword: String, stored: String): Boolean {
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = unb64(parts[0])
        val expected = unb64(parts[1])
        val actual = pbkdf2(rawPassword.toCharArray(), salt)
        return MessageDigest.isEqual(expected, actual)
    }

    private fun pbkdf2(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun b64(bytes: ByteArray) = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
    private fun unb64(str: String): ByteArray = android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
}
