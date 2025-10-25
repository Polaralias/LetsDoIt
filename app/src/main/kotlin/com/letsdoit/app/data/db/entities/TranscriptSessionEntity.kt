package com.letsdoit.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "transcript_sessions")
data class TranscriptSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Instant,
    val source: String,
    val engine: String,
    val langTag: String,
    val audioPath: String,
    val textPath: String?,
    val durationMs: Long?
)
