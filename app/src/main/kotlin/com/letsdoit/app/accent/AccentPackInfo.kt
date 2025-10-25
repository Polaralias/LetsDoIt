package com.letsdoit.app.accent

import java.time.Instant

data class AccentPackInfo(
    val id: String,
    val name: String,
    val createdAt: Instant,
    val provider: String,
    val prompt: String,
    val count: Int
)
