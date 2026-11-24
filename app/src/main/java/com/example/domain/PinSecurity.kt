package com.example.domain

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinSecurity {
    private const val ITERATIONS = 12_000
    private const val KEY_LENGTH_BITS = 256

    fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun hashPin(pin: String, saltHex: String): String {
        val saltBytes = hexToBytes(saltHex)
        val spec = PBEKeySpec(pin.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derived = factory.generateSecret(spec).encoded
        spec.clearPassword()
        return derived.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, saltHex: String, expectedHashHex: String): Boolean {
        val computed = hashPin(pin, saltHex)
        return MessageDigest.isEqual(computed.toByteArray(), expectedHashHex.toByteArray())
    }

    /**
     * Escalating cooldown after 5 wrong attempts: 30s, 1m, 5m.
     */
    fun lockoutDurationMsForFailures(failedAttempts: Int): Long = when {
        failedAttempts < 5 -> 0L
        failedAttempts == 5 -> 30_000L
        failedAttempts == 6 -> 60_000L
        else -> 300_000L
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val out = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            out[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return out
    }
}
