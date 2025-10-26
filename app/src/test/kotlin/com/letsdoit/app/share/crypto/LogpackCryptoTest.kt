package com.letsdoit.app.share.crypto

import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import kotlin.experimental.xor
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class LogpackCryptoTest {
    private val secureRandom = SecureRandom()

    @Test
    fun encryptsAndDecryptsLogpack() {
        val key = randomKey()
        val payload = ByteArray(512).also { secureRandom.nextBytes(it) }
        val encrypted = LogpackCrypto.encrypt(key, payload)
        val decrypted = LogpackCrypto.decrypt(key, encrypted)
        assertContentEquals(payload, decrypted)
    }

    @Test
    fun failsOnTamperedCiphertext() {
        val key = randomKey()
        val payload = ByteArray(64).also { secureRandom.nextBytes(it) }
        val encrypted = LogpackCrypto.encrypt(key, payload)
        encrypted[encrypted.lastIndex] = (encrypted.last() xor 0xFF.toByte())
        assertFailsWith<AEADBadTagException> {
            LogpackCrypto.decrypt(key, encrypted)
        }
    }

    private fun randomKey(): ByteArray = ByteArray(32).also { secureRandom.nextBytes(it) }
}
