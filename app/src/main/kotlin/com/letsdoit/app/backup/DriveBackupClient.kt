package com.letsdoit.app.backup

interface DriveBackupClient {
    suspend fun listBackups(): List<BackupInfo>
    suspend fun download(id: String): ByteArray
    suspend fun upload(name: String, payload: ByteArray): BackupInfo
    suspend fun delete(id: String)
}
