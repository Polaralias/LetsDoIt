package com.letsdoit.app.reminders

import com.letsdoit.app.data.db.dao.AlarmIndexDao
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.entities.AlarmIndexEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.integrations.alarm.AlarmScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ReminderCoordinatorTest {
    private lateinit var scheduler: FakeAlarmScheduler
    private lateinit var alarmIndexDao: FakeAlarmIndexDao
    private lateinit var taskDao: FakeTaskDao
    private lateinit var coordinator: ReminderCoordinator
    private val clock: Clock = Clock.fixed(Instant.parse("2025-01-01T10:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setup() {
        scheduler = FakeAlarmScheduler()
        alarmIndexDao = FakeAlarmIndexDao()
        taskDao = FakeTaskDao()
        coordinator = ReminderCoordinator(scheduler, alarmIndexDao, taskDao, clock)
    }

    @Test
    fun savingRecurringTaskCreatesAlarmIndex() = runBlocking {
        val task = taskDao.insert(
            TaskEntity(
                id = 1,
                listId = 1,
                title = "Water plants",
                dueAt = Instant.parse("2025-01-05T09:00:00Z"),
                repeatRule = "FREQ=WEEKLY;BYDAY=SU",
                remindOffsetMinutes = 10,
                createdAt = Instant.parse("2024-12-20T09:00:00Z"),
                updatedAt = Instant.parse("2024-12-20T09:00:00Z")
            )
        )

        coordinator.onTaskSaved(task.id)

        val scheduled = scheduler.scheduled[task.id]
        assertNotNull(scheduled)
        val entry = alarmIndexDao.entries[task.id]
        assertNotNull(entry)
        assertEquals(task.dueAt!!.minusSeconds(600).toEpochMilli(), entry?.nextFireAt)
    }

    @Test
    fun updatingDueDateReschedulesAlarm() = runBlocking {
        val base = taskDao.insert(
            TaskEntity(
                id = 2,
                listId = 1,
                title = "Call mum",
                dueAt = Instant.parse("2025-01-05T12:00:00Z"),
                repeatRule = "FREQ=DAILY",
                remindOffsetMinutes = 30,
                createdAt = Instant.parse("2024-12-20T09:00:00Z"),
                updatedAt = Instant.parse("2024-12-20T09:00:00Z")
            )
        )

        coordinator.onTaskSaved(base.id)
        val firstEntry = alarmIndexDao.entries[base.id]!!

        taskDao.update(
            base.copy(
                dueAt = Instant.parse("2025-01-06T15:00:00Z"),
                updatedAt = Instant.parse("2025-01-05T12:05:00Z")
            )
        )

        coordinator.onTaskSaved(base.id)

        val secondEntry = alarmIndexDao.entries[base.id]!!
        assertNotEquals(firstEntry.nextFireAt, secondEntry.nextFireAt)
        assertEquals(Instant.parse("2025-01-06T14:30:00Z").toEpochMilli(), secondEntry.nextFireAt)
    }

    @Test
    fun updatingRepeatRuleRefreshesHash() = runBlocking {
        val task = taskDao.insert(
            TaskEntity(
                id = 3,
                listId = 1,
                title = "Report",
                dueAt = Instant.parse("2025-01-08T09:00:00Z"),
                repeatRule = "FREQ=WEEKLY;BYDAY=WE",
                remindOffsetMinutes = 5,
                createdAt = Instant.parse("2024-12-20T09:00:00Z"),
                updatedAt = Instant.parse("2024-12-20T09:00:00Z")
            )
        )

        coordinator.onTaskSaved(task.id)
        val firstHash = alarmIndexDao.entries[task.id]!!.rruleHash

        taskDao.update(
            task.copy(
                repeatRule = "FREQ=DAILY",
                updatedAt = Instant.parse("2025-01-02T09:00:00Z")
            )
        )

        coordinator.onTaskSaved(task.id)

        val secondHash = alarmIndexDao.entries[task.id]!!.rruleHash
        assertNotEquals(firstHash, secondHash)
    }

    @Test
    fun completingRepeatingTaskRollsForward() = runBlocking {
        val task = taskDao.insert(
            TaskEntity(
                id = 4,
                listId = 2,
                title = "Journal",
                dueAt = Instant.parse("2025-01-01T09:00:00Z"),
                repeatRule = "FREQ=DAILY",
                remindOffsetMinutes = 10,
                createdAt = Instant.parse("2024-12-31T21:00:00Z"),
                updatedAt = Instant.parse("2024-12-31T21:00:00Z")
            )
        )

        coordinator.onTaskCompleted(task.id)

        val updated = taskDao.getById(task.id)!!
        assertEquals(Instant.parse("2025-01-02T09:00:00Z"), updated.dueAt)
        assertEquals(false, updated.completed)
        val scheduled = scheduler.scheduled[task.id]
        assertNotNull(scheduled)
        assertEquals(Instant.parse("2025-01-02T08:50:00Z"), scheduled?.first)
    }

    private class FakeAlarmScheduler : AlarmScheduler {
        val scheduled = mutableMapOf<Long, Pair<Instant, String>>()
        val cancelled = mutableSetOf<Long>()

        override fun schedule(taskId: Long, fireAt: Instant, title: String) {
            scheduled[taskId] = fireAt to title
        }

        override fun cancel(taskId: Long) {
            cancelled += taskId
            scheduled.remove(taskId)
        }
    }

    private class FakeAlarmIndexDao : AlarmIndexDao {
        val entries = mutableMapOf<Long, AlarmIndexEntity>()

        override suspend fun upsert(entity: AlarmIndexEntity): Long {
            entries[entity.taskId] = entity
            return entity.taskId
        }

        override suspend fun findByTaskId(taskId: Long): AlarmIndexEntity? = entries[taskId]

        override suspend fun deleteByTaskId(taskId: Long) {
            entries.remove(taskId)
        }

        override suspend fun listAll(): List<AlarmIndexEntity> = entries.values.toList()

        override suspend fun deleteBefore(threshold: Long) {
            entries.values.removeIf { it.nextFireAt < threshold }
        }
    }

    private class FakeTaskDao : TaskDao {
        private val tasks = linkedMapOf<Long, TaskEntity>()

        fun insert(task: TaskEntity): TaskEntity {
            tasks[task.id] = task
            return task
        }

        fun update(task: TaskEntity) {
            tasks[task.id] = task
        }

        override fun observeAll(): Flow<List<TaskEntity>> = emptyFlow()

        override fun observeByList(listId: Long): Flow<List<TaskEntity>> = emptyFlow()

        override fun observeTimeline(): Flow<List<TaskEntity>> = emptyFlow()

        override suspend fun getById(taskId: Long): TaskEntity? = tasks[taskId]

        override suspend fun upsert(task: TaskEntity): Long {
            tasks[task.id] = task
            return task.id
        }

        override suspend fun maxOrderInList(listId: Long): Int? = 0

        override suspend fun listByOrder(listId: Long): List<TaskEntity> = tasks.values.toList()

        override suspend fun listAll(): List<TaskEntity> = tasks.values.toList()

        override suspend fun updateCompletion(taskId: Long, completed: Boolean, updatedAt: Instant) {
            val current = tasks[taskId] ?: return
            tasks[taskId] = current.copy(completed = completed, updatedAt = updatedAt)
        }

        override suspend fun updateOrderInList(taskId: Long, orderInList: Int, updatedAt: Instant) {
            val current = tasks[taskId] ?: return
            tasks[taskId] = current.copy(orderInList = orderInList, updatedAt = updatedAt)
        }

        override suspend fun updatePriority(taskId: Long, priority: Int, updatedAt: Instant) {
            val current = tasks[taskId] ?: return
            tasks[taskId] = current.copy(priority = priority, updatedAt = updatedAt)
        }

        override suspend fun updateTimeline(taskId: Long, startAt: Long?, durationMinutes: Int?, updatedAt: Instant) {
            val current = tasks[taskId] ?: return
            tasks[taskId] = current.copy(startAt = startAt, durationMinutes = durationMinutes, updatedAt = updatedAt)
        }

        override suspend fun updateCalendarEvent(taskId: Long, calendarEventId: Long?, updatedAt: Instant) {
            val current = tasks[taskId] ?: return
            tasks[taskId] = current.copy(calendarEventId = calendarEventId, updatedAt = updatedAt)
        }

        override suspend fun updateColumn(taskId: Long, column: String, updatedAt: Instant) {
            val current = tasks[taskId] ?: return
            tasks[taskId] = current.copy(column = column, updatedAt = updatedAt)
        }

        override suspend fun delete(taskId: Long) {
            tasks.remove(taskId)
        }
    }
}
