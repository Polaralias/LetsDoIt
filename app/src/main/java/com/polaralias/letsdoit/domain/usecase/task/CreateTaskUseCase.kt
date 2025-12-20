package com.polaralias.letsdoit.domain.usecase.task

import com.polaralias.letsdoit.domain.alarm.AlarmScheduler
import com.polaralias.letsdoit.domain.model.Task
import com.polaralias.letsdoit.domain.repository.CalendarRepository
import com.polaralias.letsdoit.domain.repository.PreferencesRepository
import com.polaralias.letsdoit.domain.repository.TaskRepository
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
