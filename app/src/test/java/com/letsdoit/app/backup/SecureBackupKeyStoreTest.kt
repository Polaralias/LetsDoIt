package com.letsdoit.app.backup

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.letsdoit.app.security.SecurePrefs
import java.io.File
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SecureBackupKeyStoreTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val baseDir = context.filesDir.parentFile ?: context.filesDir
        val sharedPrefsDir = File(baseDir, "shared_prefs")
        File(sharedPrefsDir, "secure_store.xml").delete()
        File(sharedPrefsDir, "secure_store.xml.bak").delete()
    }

    @Test
    fun storesKeyInEncryptedPreferences() {
        val securePrefs = SecurePrefs(context)
        val store = SecureBackupKeyStore(securePrefs)
        val keyBytes = ByteArray(32) { index -> index.toByte() }
        val key = Base64.getEncoder().encodeToString(keyBytes)
        store.writeKey(key)
        assertEquals(key, store.readKey())
        val baseDir = context.filesDir.parentFile ?: context.filesDir
        val sharedPrefsDir = File(baseDir, "shared_prefs")
        val prefsFile = File(sharedPrefsDir, "secure_store.xml")
        assertTrue(prefsFile.exists())
        val contents = prefsFile.readText()
        assertFalse(contents.contains("backup_key"))
        assertFalse(contents.contains(key))
    }
}
