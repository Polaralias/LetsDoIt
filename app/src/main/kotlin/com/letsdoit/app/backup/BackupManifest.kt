package com.letsdoit.app.backup

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BackupManifest(
    val schemaVersion: Int,
    val createdAt: Long
)
