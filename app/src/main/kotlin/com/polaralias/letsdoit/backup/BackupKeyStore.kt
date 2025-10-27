package com.polaralias.letsdoit.backup

interface BackupKeyStore {
    fun readKey(): String?
    fun writeKey(value: String)
}
