package com.example.indianbloom.data.nfc

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

object NfcCrypto {
    private const val KEY_ALIAS = "BloomNfcSignatureKey"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val HMAC_ALGORITHM = "HmacSHA256"

    init {
        generateKeyIfNeeded()
    }

    private fun generateKeyIfNeeded() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_HMAC_SHA256,
                ANDROID_KEYSTORE
            )
            keyGenerator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                .build()
            )
            keyGenerator.generateKey()
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    /**
     * Generates a secure HMAC-SHA256 signature for the given NFC card hardware UID.
     */
    fun generateSignature(uid: String): String {
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(getSecretKey())
        val bytes = mac.doFinal(uid.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies that the signature matches the calculated signature for this UID.
     */
    fun verifySignature(uid: String, signature: String): Boolean {
        val expected = generateSignature(uid)
        return expected.equals(signature, ignoreCase = true)
    }
}
