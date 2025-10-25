package com.letsdoit.app.backup

interface BackupKeyStore {
    fun readKey(): String?
    fun writeKey(value: String)
}
