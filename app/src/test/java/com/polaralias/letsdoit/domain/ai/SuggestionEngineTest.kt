package com.polaralias.letsdoit.domain.ai

import com.polaralias.letsdoit.domain.model.Task
import com.polaralias.letsdoit.domain.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SuggestionEngineTest {

    private lateinit var taskRepository: TaskRepository
    private lateinit var suggestionEngine: SuggestionEngine

    @Before
    fun setup() {
        taskRepository = mockk()
        suggestionEngine = SuggestionEngineImpl(taskRepository)
    }

    @Test
    fun `getSuggestions should return suggestion for frequent task on same day of week`() = runTest {
        // Given
        val today = LocalDate.now()
        // Ensure today is the same day of week as the tasks we create
        // We will mock tasks created on the same day of week in previous weeks.

        val taskTitle = "Gym"
        val tasks = listOf(
            createTask(taskTitle, today.minusWeeks(1).atStartOfDay()),
            createTask(taskTitle, today.minusWeeks(2).atStartOfDay()),
            createTask(taskTitle, today.minusWeeks(3).atStartOfDay())
        )

        every { taskRepository.getTasksFlow(null) } returns flowOf(tasks)

        // When
        val suggestions = suggestionEngine.getSuggestions().first()

        // Then
        assertEquals(1, suggestions.size)
        assertEquals(taskTitle, suggestions[0].title)
        assertTrue(suggestions[0].confidence > 0.3)
    }

    @Test
    fun `getSuggestions should NOT return suggestion if task already active today`() = runTest {
        // Given
        val today = LocalDate.now()
        val taskTitle = "Gym"

        val tasks = listOf(
            createTask(taskTitle, today.minusWeeks(1).atStartOfDay()),
            createTask(taskTitle, today.minusWeeks(2).atStartOfDay()),
            // Active task for today
            createTask(taskTitle, LocalDateTime.now(), status = "Open")
        )

        every { taskRepository.getTasksFlow(null) } returns flowOf(tasks)

        // When
        val suggestions = suggestionEngine.getSuggestions().first()

        // Then
        assertEquals(0, suggestions.size)
    }

    @Test
    fun `getSuggestions should NOT return suggestion if frequency is low`() = runTest {
        // Given
        val today = LocalDate.now()
        val taskTitle = "Rare Event"

        val tasks = listOf(
            createTask(taskTitle, today.minusWeeks(1).atStartOfDay())
            // Only once
        )

        every { taskRepository.getTasksFlow(null) } returns flowOf(tasks)

        // When
        val suggestions = suggestionEngine.getSuggestions().first()

        // Then
        assertEquals(0, suggestions.size)
    }

    private fun createTask(
        title: String,
        createdAt: LocalDateTime,
        status: String = "Done"
    ): Task {
        return Task(
            id = "id_${System.nanoTime()}",
            listId = "list_1",
            title = title,
            description = null,
            status = status,
            dueDate = null,
            priority = 0,
            createdAt = createdAt
        )
    }
}
