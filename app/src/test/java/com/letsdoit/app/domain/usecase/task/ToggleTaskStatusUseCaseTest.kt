package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.TaskRepository
import com.letsdoit.app.domain.util.TaskStatusUtil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

class ToggleTaskStatusUseCaseTest {

    private val repository = mockk<TaskRepository>(relaxed = true)
    private val updateTaskUseCase = mockk<UpdateTaskUseCase>(relaxed = true)
    private val toggleTaskStatusUseCase = ToggleTaskStatusUseCase(repository, updateTaskUseCase)

    @Test
    fun `invoke changes status from Open to Completed`() = runTest {
        val task = Task(
            id = "1",
            title = "Task",
            description = "Desc",
            status = "Open",
            dueDate = null,
            priority = 1,
            listId = "list1",
            createdAt = LocalDateTime.now(),
            isSynced = false
        )
        coEvery { repository.getTask("1") } returns task

        toggleTaskStatusUseCase("1")

        coVerify { updateTaskUseCase(task.copy(status = TaskStatusUtil.COMPLETED)) }
    }

    @Test
    fun `invoke changes status from Completed to Open`() = runTest {
        val task = Task(
            id = "1",
            title = "Task",
            description = "Desc",
            status = "Completed",
            dueDate = null,
            priority = 1,
            listId = "list1",
            createdAt = LocalDateTime.now(),
            isSynced = false
        )
        coEvery { repository.getTask("1") } returns task

        toggleTaskStatusUseCase("1")

        coVerify { updateTaskUseCase(task.copy(status = TaskStatusUtil.OPEN)) }
    }

    @Test
    fun `invoke changes status from Done to Open`() = runTest {
        val task = Task(
            id = "1",
            title = "Task",
            description = "Desc",
            status = "Done",
            dueDate = null,
            priority = 1,
            listId = "list1",
            createdAt = LocalDateTime.now(),
            isSynced = false
        )
        coEvery { repository.getTask("1") } returns task

        toggleTaskStatusUseCase("1")

        coVerify { updateTaskUseCase(task.copy(status = TaskStatusUtil.OPEN)) }
    }

    @Test
    fun `invoke changes status from In Progress to Completed`() = runTest {
        val task = Task(
            id = "1",
            title = "Task",
            description = "Desc",
            status = "In Progress",
            dueDate = null,
            priority = 1,
            listId = "list1",
            createdAt = LocalDateTime.now(),
            isSynced = false
        )
        coEvery { repository.getTask("1") } returns task

        toggleTaskStatusUseCase("1")

        coVerify { updateTaskUseCase(task.copy(status = TaskStatusUtil.COMPLETED)) }
    }
}
