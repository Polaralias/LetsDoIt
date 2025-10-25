package com.letsdoit.app.reminders

import com.letsdoit.app.data.db.dao.AlarmIndexDao
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.entities.AlarmIndexEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.integrations.alarm.AlarmScheduler
import com.letsdoit.app.recurrence.RecurrenceCalculator
import com.letsdoit.app.recurrence.RecurrenceRule
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.text.Charsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderCoordinator @Inject constructor(
    private val alarmScheduler: AlarmScheduler,
    private val alarmIndexDao: AlarmIndexDao,
    private val taskDao: TaskDao,
    private val clock: Clock
) {
    private val zoneId: ZoneId = clock.zone
    private val calculator: RecurrenceCalculator = RecurrenceCalculator(zoneId)

    suspend fun onTaskSaved(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        handleTask(task)
    }

    suspend fun onTaskCompleted(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        val now = Instant.now(clock)
        if (task.repeatRule != null && task.dueAt != null) {
            val rule = RecurrenceRule.fromRRule(task.repeatRule)
            if (rule != null) {
                val nextDue = calculator.nextOccurrence(task.dueAt, rule, now)
                if (nextDue != null) {
                    val updated = task.copy(dueAt = nextDue, updatedAt = now, completed = false)
                    taskDao.upsert(updated)
                    handleTask(updated)
                    return
                }
            }
        }
        taskDao.updateCompletion(taskId, true, now)
        alarmScheduler.cancel(taskId)
        alarmIndexDao.deleteByTaskId(taskId)
    }

    suspend fun onTaskDeleted(taskId: Long) {
        alarmScheduler.cancel(taskId)
        alarmIndexDao.deleteByTaskId(taskId)
    }

    suspend fun rescheduleAll() {
        val tasks = taskDao.listAll()
        tasks.forEach { task ->
            handleTask(task)
        }
    }

    suspend fun performMaintenance() {
        val now = Instant.now(clock)
        rescheduleAll()
        alarmIndexDao.deleteBefore(now.toEpochMilli())
    }

    suspend fun snooze(taskId: Long, minutes: Long) {
        val task = taskDao.getById(taskId) ?: return
        val fireAt = Instant.now(clock).plusSeconds(minutes * 60)
        alarmScheduler.schedule(taskId, fireAt, task.title)
        val existing = alarmIndexDao.findByTaskId(taskId)
        if (existing != null) {
            val updated = existing.copy(nextFireAt = fireAt.toEpochMilli())
            alarmIndexDao.upsert(updated)
        }
    }

    private suspend fun handleTask(task: TaskEntity) {
        val now = Instant.now(clock)
        if (task.completed) {
            alarmScheduler.cancel(task.id)
            alarmIndexDao.deleteByTaskId(task.id)
            return
        }
        if (task.repeatRule.isNullOrBlank()) {
            alarmIndexDao.deleteByTaskId(task.id)
            scheduleOneOff(task, now)
            return
        }
        val rule = RecurrenceRule.fromRRule(task.repeatRule)
        if (rule == null) {
            alarmIndexDao.deleteByTaskId(task.id)
            scheduleOneOff(task, now)
            return
        }
        val dueAt = task.dueAt
        if (dueAt == null) {
            alarmScheduler.cancel(task.id)
            alarmIndexDao.deleteByTaskId(task.id)
            return
        }
        val targetDue = if (dueAt.isAfter(now)) {
            dueAt
        } else {
            calculator.nextOccurrence(dueAt, rule, now) ?: run {
                alarmScheduler.cancel(task.id)
                alarmIndexDao.deleteByTaskId(task.id)
                return
            }
        }
        if (targetDue != dueAt) {
            val updated = task.copy(dueAt = targetDue, updatedAt = now, completed = false)
            taskDao.upsert(updated)
            scheduleRecurring(updated, rule, now)
        } else {
            scheduleRecurring(task, rule, now)
        }
    }

    private fun computeReminderInstant(dueAt: Instant, offsetMinutes: Int?, now: Instant): Instant? {
        if (offsetMinutes == null) return null
        val candidate = dueAt.minusSeconds(offsetMinutes.toLong() * 60)
        return if (candidate.isBefore(now)) {
            now.plusSeconds(5)
        } else {
            candidate
        }
    }

    private suspend fun scheduleRecurring(task: TaskEntity, rule: RecurrenceRule, now: Instant) {
        val offset = task.remindOffsetMinutes
        if (offset == null) {
            alarmScheduler.cancel(task.id)
            alarmIndexDao.deleteByTaskId(task.id)
            return
        }
        val dueAt = task.dueAt ?: return
        var fireAt = computeReminderInstant(dueAt, offset, now)
        if (fireAt == null) {
            alarmScheduler.cancel(task.id)
            alarmIndexDao.deleteByTaskId(task.id)
            return
        }
        if (fireAt.isBefore(now) && dueAt.isBefore(now)) {
            val nextDue = calculator.nextOccurrence(dueAt, rule, now)
            if (nextDue != null && nextDue != dueAt) {
                val updated = task.copy(dueAt = nextDue, updatedAt = now, completed = false)
                taskDao.upsert(updated)
                scheduleRecurring(updated, rule, now)
                return
            }
        }
        val entry = AlarmIndexEntity(
            taskId = task.id,
            nextFireAt = fireAt.toEpochMilli(),
            rruleHash = hash(rule.toRRule())
        )
        alarmIndexDao.upsert(entry)
        alarmScheduler.schedule(task.id, fireAt, task.title)
    }

    private suspend fun scheduleOneOff(task: TaskEntity, now: Instant) {
        val offset = task.remindOffsetMinutes ?: run {
            alarmScheduler.cancel(task.id)
            return
        }
        val dueAt = task.dueAt ?: run {
            alarmScheduler.cancel(task.id)
            return
        }
        val fireAt = computeReminderInstant(dueAt, offset, now) ?: return
        if (dueAt.isBefore(now)) {
            alarmScheduler.cancel(task.id)
            return
        }
        alarmScheduler.schedule(task.id, fireAt, task.title)
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { byte ->
            val unsigned = byte.toInt() and 0xff
            unsigned.toString(16).padStart(2, '0')
        }
    }
}
