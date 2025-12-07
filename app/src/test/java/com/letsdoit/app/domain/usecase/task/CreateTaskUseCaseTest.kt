package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class CreateTaskUseCaseTest {

    private val repository = mockk<TaskRepository>(relaxed = true)
    private val createTaskUseCase = CreateTaskUseCase(repository)

    @Test
    fun `invoke throws exception when title is blank`() = runTest {
        val task = Task(
            id = "1",
            listId = "list1",
            title = "",
            description = "desc",
            status = "open",
            dueDate = null,
            priority = 1,
            createdAt = 0,
            updatedAt = 0,
            isSynced = false
        )

        try {
            createTaskUseCase(task)
            throw AssertionError("Expected IllegalArgumentException was not thrown")
        } catch (e: IllegalArgumentException) {
            // Expected
        }
    }

    @Test
    fun `invoke calls repository when title is valid`() = runTest {
        val task = Task(
            id = "1",
            listId = "list1",
            title = "Valid Title",
            description = "desc",
            status = "open",
            dueDate = null,
            priority = 1,
            createdAt = 0,
            updatedAt = 0,
            isSynced = false
        )

        createTaskUseCase(task)

        coVerify { repository.createTask(task) }
    }
}
