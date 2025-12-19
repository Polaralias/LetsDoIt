package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.model.SearchFilter
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.util.UUID

class SearchTasksUseCaseTest {

    private val repository = mockk<TaskRepository>()
    private val useCase = SearchTasksUseCase(repository)

    private val task1 = Task(
        id = "1", listId = "L1", title = "Task 1", description = null,
        status = "Open", dueDate = null, priority = 1,
        createdAt = LocalDateTime.now(), isSynced = true
    )
    private val task2 = Task(
        id = "2", listId = "L1", title = "Task 2", description = null,
        status = "Completed", dueDate = null, priority = 3,
        createdAt = LocalDateTime.now(), isSynced = true
    )

    @Test
    fun `should return all tasks when filter is empty`() = runTest {
        every { repository.searchTasks("query") } returns flowOf(listOf(task1, task2))

        val result = useCase("query", SearchFilter()).first()

        assertEquals(2, result.size)
    }

    @Test
    fun `should filter by status`() = runTest {
        every { repository.searchTasks("query") } returns flowOf(listOf(task1, task2))

        val filter = SearchFilter(status = listOf("Open"))
        val result = useCase("query", filter).first()

        assertEquals(1, result.size)
        assertEquals("Task 1", result[0].title)
    }

    @Test
    fun `should filter by priority`() = runTest {
        every { repository.searchTasks("query") } returns flowOf(listOf(task1, task2))

        val filter = SearchFilter(priority = listOf(3))
        val result = useCase("query", filter).first()

        assertEquals(1, result.size)
        assertEquals("Task 2", result[0].title)
    }

    @Test
    fun `should filter by both status and priority`() = runTest {
        every { repository.searchTasks("query") } returns flowOf(listOf(task1, task2))

        val filter = SearchFilter(status = listOf("Open"), priority = listOf(3))
        val result = useCase("query", filter).first()

        assertEquals(0, result.size)
    }
}
