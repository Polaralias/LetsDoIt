package com.polaralias.letsdoit.share.sync

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Logpack(
    val events: List<LogpackEvent>,
    val acknowledgements: List<String>
)

@JsonClass(generateAdapter = true)
data class LogpackEvent(
    val id: String,
    val authorDeviceId: String,
    val lamport: Long,
    val timestamp: Long,
    val type: String,
    val payloadJson: String
)
