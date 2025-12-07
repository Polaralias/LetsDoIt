package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.TaskRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTasksUseCaseTest {

    private val repository = mockk<TaskRepository>()
    private val getTasksUseCase = GetTasksUseCase(repository)

    @Test
    fun `invoke returns flow from repository`() = runTest {
        val tasks = listOf(
            Task("1", "list1", "Title 1", null, "open", null, 1, true),
            Task("2", "list1", "Title 2", null, "open", null, 1, true)
        )
        every { repository.getTasksFlow() } returns flowOf(tasks)

        val result = getTasksUseCase().toList()

        assertEquals(1, result.size)
        assertEquals(tasks, result[0])
        verify { repository.getTasksFlow() }
    }
}
