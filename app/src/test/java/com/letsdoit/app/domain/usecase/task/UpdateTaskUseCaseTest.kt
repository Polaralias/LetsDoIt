package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.alarm.AlarmScheduler
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.TaskRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

class UpdateTaskUseCaseTest {

    private val repository = mockk<TaskRepository>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val updateTaskUseCase = UpdateTaskUseCase(repository, alarmScheduler)

    @Test
    fun `invoke calls repository and schedules alarm when due date is present and status is Open`() = runTest {
        val task = Task(
            id = "1",
            title = "Task",
            description = "Desc",
            status = "Open",
            dueDate = LocalDateTime.now().plusDays(1),
            priority = 1,
            listId = "list1",
            isSynced = false
        )

        updateTaskUseCase(task)

        coVerify { repository.updateTask(task) }
        coVerify { alarmScheduler.scheduleAlarm(task) }
    }

    @Test
    fun `invoke cancels alarm when due date is null`() = runTest {
        val task = Task(
            id = "1",
            title = "Task",
            description = "Desc",
            status = "Open",
            dueDate = null,
            priority = 1,
            listId = "list1",
            isSynced = false
        )

        updateTaskUseCase(task)

        coVerify { repository.updateTask(task) }
        coVerify { alarmScheduler.cancelAlarm(task) }
    }

    @Test
    fun `invoke cancels alarm when status is Completed`() = runTest {
        val task = Task(
            id = "1",
            title = "Task",
            description = "Desc",
            status = "Completed",
            dueDate = LocalDateTime.now().plusDays(1),
            priority = 1,
            listId = "list1",
            isSynced = false
        )

        updateTaskUseCase(task)

        coVerify { repository.updateTask(task) }
        coVerify { alarmScheduler.cancelAlarm(task) }
    }

    @Test
    fun `invoke cancels alarm when status is Done`() = runTest {
        val task = Task(
            id = "1",
            title = "Task",
            description = "Desc",
            status = "Done",
            dueDate = LocalDateTime.now().plusDays(1),
            priority = 1,
            listId = "list1",
            isSynced = false
        )

        updateTaskUseCase(task)

        coVerify { repository.updateTask(task) }
        coVerify { alarmScheduler.cancelAlarm(task) }
    }
}
