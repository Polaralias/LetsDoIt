package com.polaralias.letsdoit.domain.usecase.task

import com.polaralias.letsdoit.domain.alarm.AlarmScheduler
import com.polaralias.letsdoit.domain.model.Task
import com.polaralias.letsdoit.domain.repository.CalendarRepository
import com.polaralias.letsdoit.domain.repository.PreferencesRepository
import com.polaralias.letsdoit.domain.repository.TaskRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class CreateTaskUseCaseTest {

    private val repository = mockk<TaskRepository>(relaxed = true)
    private val alarmScheduler = mockk<AlarmScheduler>(relaxed = true)
    private val calendarRepository = mockk<CalendarRepository>(relaxed = true)
    private val preferencesRepository = mockk<PreferencesRepository>(relaxed = true)
    private val createTaskUseCase = CreateTaskUseCase(repository, alarmScheduler, calendarRepository, preferencesRepository)

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
            createdAt = java.time.LocalDateTime.now(),
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
            createdAt = java.time.LocalDateTime.now(),
            isSynced = false
        )

        createTaskUseCase(task)

        coVerify { repository.createTask(task) }
        coVerify { alarmScheduler.scheduleAlarm(task) }
    }
}
