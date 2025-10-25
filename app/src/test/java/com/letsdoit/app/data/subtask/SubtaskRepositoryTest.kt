package com.letsdoit.app.data.subtask

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.letsdoit.app.data.db.AppDatabase
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class SubtaskRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: SubtaskRepository

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SubtaskRepositoryImpl(database, database.subtaskDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun createAndUndoSubtasks() = runBlocking {
        val spaceDao = database.spaceDao()
        val listDao = database.listDao()
        val taskDao = database.taskDao()
        spaceDao.upsert(SpaceEntity(name = "Test"))
        val space = spaceDao.findByName("Test")!!
        listDao.upsert(ListEntity(name = "Inbox", spaceId = space.id))
        val list = listDao.findByName("Inbox")!!
        val now = Instant.parse("2024-01-01T10:00:00Z")
        val taskId = taskDao.upsert(
            TaskEntity(
                listId = list.id,
                title = "Parent",
                createdAt = now,
                updatedAt = now,
                priority = 2,
                orderInList = 0,
                column = "To do"
            )
        )
        val ids = repository.createSubtasks(taskId, listOf(NewSubtask(title = "One"), NewSubtask(title = "Two")))
        assertEquals(2, ids.size)
        val stored = repository.listSubtasks(taskId)
        assertEquals(listOf("One", "Two"), stored.map { it.title })
        repository.deleteSubtasks(ids)
        val remaining = repository.listSubtasks(taskId)
        assertTrue(remaining.isEmpty())
    }
}
