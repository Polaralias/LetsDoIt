package com.polaralias.letsdoit.data.task

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.polaralias.letsdoit.data.db.AppDatabase
import com.polaralias.letsdoit.data.db.entities.ListEntity
import com.polaralias.letsdoit.data.db.entities.SpaceEntity
import com.polaralias.letsdoit.data.subtask.SubtaskRepositoryImpl
import com.polaralias.letsdoit.integrations.alarm.AlarmScheduler
import com.polaralias.letsdoit.integrations.calendar.CalendarBridge
import com.polaralias.letsdoit.reminders.ReminderCoordinator
import java.time.Clock
import java.time.ZoneOffset
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class BulkCreateRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: TaskRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val clock = Clock.system(ZoneOffset.UTC)
        val reminderCoordinator = ReminderCoordinator(
            object : AlarmScheduler {
                override fun schedule(taskId: Long, triggerAt: Instant, title: String) = Unit
                override fun cancel(taskId: Long) = Unit
            },
            database.alarmIndexDao(),
            database.taskDao(),
            clock
        )
        val subtaskRepository = SubtaskRepositoryImpl(database, database.subtaskDao())
        val calendarBridge = CalendarBridge(context)
        repository = TaskRepositoryImpl(
            database = database,
            taskDao = database.taskDao(),
            taskOrderDao = database.taskOrderDao(),
            listDao = database.listDao(),
            spaceDao = database.spaceDao(),
            clock = clock,
            reminderCoordinator = reminderCoordinator,
            subtaskRepository = subtaskRepository,
            calendarBridge = calendarBridge
        )
        runBlocking {
            database.spaceDao().upsert(SpaceEntity(id = 1, name = "Work"))
            database.listDao().upsert(ListEntity(id = 1, spaceId = 1, name = "Backlog"))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun bulkCreate_producesContiguousOrder() = runBlocking {
        val list = database.listDao().findByName("Backlog")!!
        val items = listOf(
            BulkCreateItem(listId = list.id, title = "Task A", column = "Doing"),
            BulkCreateItem(listId = list.id, title = "Task B", column = "Doing"),
            BulkCreateItem(listId = list.id, title = "Task C", column = "Doing")
        )
        repository.bulkCreate(items)
        val tasks = database.taskDao().listByOrder(list.id)
        assertEquals(listOf(0, 1, 2), tasks.map { it.orderInList })
        val orders = database.taskOrderDao().listByColumn("Doing")
        assertEquals(listOf(0, 1, 2), orders.map { it.orderInColumn })
    }
}
