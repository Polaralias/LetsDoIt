package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.alarm.AlarmScheduler
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.CalendarRepository
import com.letsdoit.app.domain.repository.PreferencesRepository
import com.letsdoit.app.domain.repository.TaskRepository
import com.letsdoit.app.domain.util.RecurrenceUtil
import java.util.UUID
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
    private val repository: TaskRepository,
    private val createTaskUseCase: CreateTaskUseCase,
    private val alarmScheduler: AlarmScheduler,
    private val calendarRepository: CalendarRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(task: Task) {
        if (task.title.isBlank()) {
            throw IllegalArgumentException("Title cannot be empty")
        }

        val oldTask = repository.getTask(task.id)
        var taskToSave = task

        if (preferencesRepository.isCalendarSyncEnabled()) {
             if (task.calendarEventId != null) {
                 if (task.dueDate != null && !isCompleted(task.status)) {
                     calendarRepository.updateEvent(task)
                 } else {
                     // Task completed or due date removed
                     if (task.dueDate == null) {
                         calendarRepository.deleteEvent(task.calendarEventId)
                         taskToSave = task.copy(calendarEventId = null)
                     } else {
                         // status is completed/done
                         // Keep it in calendar but update it
                         calendarRepository.updateEvent(task)
                     }
                 }
             } else if (task.dueDate != null && !isCompleted(task.status)) {
                 // Create event if it doesn't exist but should
                 val calendarId = preferencesRepository.getSelectedCalendarId()
                 if (calendarId != -1L) {
                     val eventId = calendarRepository.addEvent(task, calendarId)
                     if (eventId != null) {
                         taskToSave = task.copy(calendarEventId = eventId)
                     }
                 }
             }
        }

        repository.updateTask(taskToSave)

        // Handle Recurrence
        if (oldTask != null && !isCompleted(oldTask.status) && isCompleted(taskToSave.status)) {
            val rule = taskToSave.recurrenceRule
            val date = taskToSave.dueDate
            if (rule != null && date != null) {
                val nextDate = RecurrenceUtil.calculateNextDueDate(date, rule)
                if (nextDate != null) {
                    val newTask = taskToSave.copy(
                        id = UUID.randomUUID().toString(),
                        status = "open",
                        dueDate = nextDate,
                        isSynced = false,
                        calendarEventId = null
                    )
                    createTaskUseCase(newTask)
                }
            }
        }

        if (taskToSave.dueDate != null && !isCompleted(taskToSave.status)) {
            alarmScheduler.scheduleAlarm(taskToSave)
        } else {
            alarmScheduler.cancelAlarm(taskToSave)
        }
    }

    private fun isCompleted(status: String): Boolean {
        return status.equals("Completed", ignoreCase = true) ||
               status.equals("Done", ignoreCase = true) ||
               status.equals("complete", ignoreCase = true)
    }
}
