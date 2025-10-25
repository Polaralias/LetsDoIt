package com.letsdoit.app.backup

import com.letsdoit.app.security.SecurePrefs
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureBackupKeyStore @Inject constructor(
    private val securePrefs: SecurePrefs
) : BackupKeyStore {
    private val keyPref = "backup_key"

    override fun readKey(): String? = securePrefs.read(keyPref)

    override fun writeKey(value: String) {
        securePrefs.write(keyPref, value)
    }
}
