package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.alarm.AlarmScheduler
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.CalendarRepository
import com.letsdoit.app.domain.repository.PreferencesRepository
import com.letsdoit.app.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDateTime

class UpdateTaskUseCaseTest {

    private val repository = mockk<TaskRepository>(relaxed = true)
    private val createTaskUseCaseMock = mockk<CreateTaskUseCase>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val calendarRepository = mockk<CalendarRepository>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val updateTaskUseCase = UpdateTaskUseCase(repository, createTaskUseCaseMock, alarmScheduler, calendarRepository, preferencesRepository)

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
            createdAt = java.time.LocalDateTime.now(),
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
            createdAt = java.time.LocalDateTime.now(),
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
            createdAt = java.time.LocalDateTime.now(),
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
            createdAt = java.time.LocalDateTime.now(),
            isSynced = false
        )

        updateTaskUseCase(task)

        coVerify { repository.updateTask(task) }
        coVerify { alarmScheduler.cancelAlarm(task) }
    }

    @Test
    fun `invoke creates next recurring task when completed`() = runTest {
        val now = LocalDateTime.now()
        val oldTask = Task(
             id = "1",
             title = "Task",
             description = "Desc",
             status = "Open",
             dueDate = now,
             priority = 1,
             listId = "list1",
             createdAt = java.time.LocalDateTime.now(),
             isSynced = true,
             recurrenceRule = "FREQ=DAILY"
        )
        val newTask = oldTask.copy(status = "Completed")

        coEvery { repository.getTask("1") } returns oldTask

        updateTaskUseCase(newTask)

        coVerify { repository.updateTask(newTask) }
        coVerify { createTaskUseCaseMock(any()) }
    }
}
