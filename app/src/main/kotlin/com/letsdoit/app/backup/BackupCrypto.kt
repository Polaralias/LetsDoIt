package com.letsdoit.app.backup

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupCrypto @Inject constructor(
    private val keyStore: BackupKeyStore
) {
    private val random = SecureRandom()

    fun encrypt(plain: ByteArray): ByteArray {
        val key = ensureKey()
        val iv = ByteArray(12)
        random.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secret = SecretKeySpec(key, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secret, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plain)
        return iv + encrypted
    }

    fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > 12)
        val key = ensureKey()
        val iv = payload.copyOfRange(0, 12)
        val data = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secret = SecretKeySpec(key, "AES")
        cipher.init(Cipher.DECRYPT_MODE, secret, GCMParameterSpec(128, iv))
        return cipher.doFinal(data)
    }

    private fun ensureKey(): ByteArray {
        val existing = keyStore.readKey()
        if (existing != null) {
            return Base64.decode(existing, Base64.NO_WRAP)
        }
        val key = ByteArray(32)
        random.nextBytes(key)
        keyStore.writeKey(Base64.encodeToString(key, Base64.NO_WRAP))
        return key
    }
}
