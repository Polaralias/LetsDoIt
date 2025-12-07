package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.alarm.AlarmScheduler
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.CalendarRepository
import com.letsdoit.app.domain.repository.PreferencesRepository
import com.letsdoit.app.domain.repository.TaskRepository
import javax.inject.Inject

class CreateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val alarmScheduler: AlarmScheduler,
    private val calendarRepository: CalendarRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(task: Task) {
        if (task.title.isBlank()) {
            throw IllegalArgumentException("Title cannot be empty")
        }

        var taskToSave = task

        if (preferencesRepository.isCalendarSyncEnabled() && task.dueDate != null) {
            val calendarId = preferencesRepository.getSelectedCalendarId()
            if (calendarId != -1L) {
                val eventId = calendarRepository.addEvent(task, calendarId)
                if (eventId != null) {
                    taskToSave = task.copy(calendarEventId = eventId)
                }
            }
        }

        repository.createTask(taskToSave)
        alarmScheduler.scheduleAlarm(taskToSave)
    }
}
