package com.letsdoit.app.domain.usecase.task

import com.letsdoit.app.domain.alarm.AlarmScheduler
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.domain.repository.CalendarRepository
import com.letsdoit.app.domain.repository.PreferencesRepository
import com.letsdoit.app.domain.repository.TaskRepository
import javax.inject.Inject

class UpdateTaskUseCase @Inject constructor(
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

        if (preferencesRepository.isCalendarSyncEnabled()) {
             if (task.calendarEventId != null) {
                 if (task.dueDate != null && task.status != "Completed" && task.status != "Done") {
                     calendarRepository.updateEvent(task)
                 } else {
                     // Task completed or due date removed, maybe we should not delete the calendar event but just leave it?
                     // Or maybe we want to delete it? The requirement says "deleteEvent(eventId: Long)".
                     // But usually completed tasks stay on calendar.
                     // However, if due date is removed, we should probably delete it or it will stay at old date.
                     if (task.dueDate == null) {
                         calendarRepository.deleteEvent(task.calendarEventId)
                         taskToSave = task.copy(calendarEventId = null)
                     } else {
                         // status is completed/done
                         // For now, let's keep it in calendar.
                         calendarRepository.updateEvent(task)
                     }
                 }
             } else if (task.dueDate != null && task.status != "Completed" && task.status != "Done") {
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

        if (taskToSave.dueDate != null && taskToSave.status != "Completed" && taskToSave.status != "Done") {
            alarmScheduler.scheduleAlarm(taskToSave)
        } else {
            alarmScheduler.cancelAlarm(taskToSave)
        }
    }
}
