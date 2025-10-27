package com.polaralias.letsdoit.data.transcript

import com.polaralias.letsdoit.data.db.dao.TranscriptSessionDao
import com.polaralias.letsdoit.data.db.entities.TranscriptSessionEntity
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class TranscriptSource { Record, Import }

data class TranscriptSession(
    val id: Long,
    val createdAt: Instant,
    val source: TranscriptSource,
    val engine: String,
    val langTag: String,
    val audioPath: String,
    val textPath: String?,
    val durationMs: Long?
)

@Singleton
class TranscriptRepository @Inject constructor(
    private val dao: TranscriptSessionDao,
    private val clock: Clock
) {
    fun sessions(): Flow<List<TranscriptSession>> {
        return dao.sessions().map { entities -> entities.map(::mapEntity) }
    }

    suspend fun recordSession(
        source: TranscriptSource,
        engine: String,
        langTag: String,
        audioPath: String,
        textPath: String?,
        durationMs: Long?
    ) {
        val entity = TranscriptSessionEntity(
            createdAt = clock.instant(),
            source = source.name.lowercase(),
            engine = engine,
            langTag = langTag,
            audioPath = audioPath,
            textPath = textPath,
            durationMs = durationMs
        )
        dao.insert(entity)
    }

    suspend fun get(id: Long): TranscriptSession? {
        return dao.get(id)?.let(::mapEntity)
    }

    private fun mapEntity(entity: TranscriptSessionEntity): TranscriptSession {
        val source = when (entity.source.lowercase()) {
            TranscriptSource.Record.name.lowercase() -> TranscriptSource.Record
            TranscriptSource.Import.name.lowercase() -> TranscriptSource.Import
            else -> TranscriptSource.Record
        }
        return TranscriptSession(
            id = entity.id,
            createdAt = entity.createdAt,
            source = source,
            engine = entity.engine,
            langTag = entity.langTag,
            audioPath = entity.audioPath,
            textPath = entity.textPath,
            durationMs = entity.durationMs
        )
    }
}
