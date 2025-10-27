package com.polaralias.letsdoit.bulk

import com.polaralias.letsdoit.bulk.ListToken
import com.polaralias.letsdoit.data.db.entities.ListEntity
import com.polaralias.letsdoit.data.db.entities.SpaceEntity
import com.polaralias.letsdoit.data.prefs.BulkPreferences
import com.polaralias.letsdoit.data.prefs.ViewPreferences
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BulkResolversTest {
    @Test
    fun resolveListToken_matchesListByName() {
        val spaces = listOf(SpaceEntity(id = 1, name = "Home"))
        val lists = listOf(
            ListEntity(id = 10, spaceId = 1, name = "Inbox"),
            ListEntity(id = 11, spaceId = 1, name = "Projects")
        )
        val token = ListToken(space = null, list = "Project")
        val result = resolveListToken(token, lists, spaces, defaultListId = 10)
        assertEquals(11L, result.list?.id)
        assertEquals("Home", result.spaceName)
        assertNull(result.error)
    }

    @Test
    fun resolveListToken_matchesSpaceAndList() {
        val spaces = listOf(SpaceEntity(id = 1, name = "Work"), SpaceEntity(id = 2, name = "Personal"))
        val lists = listOf(
            ListEntity(id = 20, spaceId = 1, name = "Planning"),
            ListEntity(id = 21, spaceId = 2, name = "Planning")
        )
        val token = ListToken(space = "Work", list = "Plan")
        val result = resolveListToken(token, lists, spaces, defaultListId = null)
        assertEquals(20L, result.list?.id)
        assertEquals("Work", result.spaceName)
    }

    @Test
    fun mergeParsedTask_prefersTokenPriority() {
        val dueAt = Instant.parse("2024-05-01T10:00:00Z")
        val result = mergeParsedTask(
            parsedTitle = "Long task name that should be trimmed",
            dueAt = dueAt,
            repeatExpression = null,
            tokenPriority = 3,
            preferences = BulkPreferences.Default,
            viewPreferences = ViewPreferences.Default,
            zoneId = ZoneId.of("UTC")
        )
        assertEquals(3, result.priority)
        assertEquals(dueAt, result.startAt)
        assertEquals(BulkPreferences.Default.defaultDurationMinutes, result.durationMinutes)
    }
}
