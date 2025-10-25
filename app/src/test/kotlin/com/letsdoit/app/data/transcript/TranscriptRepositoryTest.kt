package com.letsdoit.app.data.transcript

import com.letsdoit.app.data.db.dao.TranscriptSessionDao
import com.letsdoit.app.data.db.entities.TranscriptSessionEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakeTranscriptSessionDao : TranscriptSessionDao {
    private var nextId = 1L
    private val state = MutableStateFlow<List<TranscriptSessionEntity>>(emptyList())

    override fun sessions(): Flow<List<TranscriptSessionEntity>> = state

    override suspend fun insert(entity: TranscriptSessionEntity) {
        val assigned = entity.copy(id = nextId++)
        state.value = listOf(assigned) + state.value
    }

    override suspend fun get(id: Long): TranscriptSessionEntity? {
        return state.value.firstOrNull { it.id == id }
    }
}

class TranscriptRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2024-07-01T12:00:00Z"), ZoneId.of("UTC"))

    @Test
    fun recordSessionAddsEntry() = runBlocking {
        val dao = FakeTranscriptSessionDao()
        val repository = TranscriptRepository(dao, clock)

        repository.recordSession(
            source = TranscriptSource.Record,
            engine = "local",
            langTag = "en-GB",
            audioPath = "/tmp/audio.m4a",
            textPath = "/tmp/audio.txt",
            durationMs = 1200L
        )

        val sessions = dao.sessions().first()
        assertEquals(1, sessions.size)
        val entity = sessions.first()
        assertEquals("record", entity.source)
        assertEquals("local", entity.engine)
        assertEquals("en-GB", entity.langTag)
        assertEquals("/tmp/audio.m4a", entity.audioPath)
        assertEquals("/tmp/audio.txt", entity.textPath)
        assertEquals(1200L, entity.durationMs)
        assertEquals(clock.instant(), entity.createdAt)
    }
}
