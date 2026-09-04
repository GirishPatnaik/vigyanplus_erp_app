package com.vigyan.juniorcollege.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts/decrypts sensitive fields (Aadhaar number, etc.) using AES/GCM with a key
 * held in the Android Keystore — the raw key material never leaves secure hardware
 * and is not extractable, even with root access to the device.
 *
 * Stored format: base64(iv) + ":" + base64(ciphertext)
 */
object AadhaarCrypto {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "vigyan_aadhaar_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH_BITS = 128

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    /** Returns null if [plainAadhaar] is blank — nothing to encrypt. */
    fun encrypt(plainAadhaar: String): String? {
        if (plainAadhaar.isBlank()) return null
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainAadhaar.trim().toByteArray(Charsets.UTF_8))
        return "${b64(iv)}:${b64(cipherText)}"
    }

    /** Returns null if [stored] is null/blank or cannot be decrypted. */
    fun decrypt(stored: String?): String? {
        if (stored.isNullOrBlank()) return null
        return try {
            val parts = stored.split(":")
            if (parts.size != 2) return null
            val iv = unb64(parts[0])
            val cipherText = unb64(parts[1])
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /** Masks a plaintext Aadhaar for display, e.g. "XXXX XXXX 1234". */
    fun mask(plainAadhaar: String): String {
        val digits = plainAadhaar.filter { it.isDigit() }
        if (digits.length < 4) return "XXXX XXXX XXXX"
        val last4 = digits.takeLast(4)
        return "XXXX XXXX $last4"
    }

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(str: String): ByteArray = Base64.decode(str, Base64.NO_WRAP)
}
