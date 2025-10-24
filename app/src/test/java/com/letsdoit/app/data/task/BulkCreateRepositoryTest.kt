package com.letsdoit.app.data.task

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.letsdoit.app.data.db.AppDatabase
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import java.time.Clock
import java.time.ZoneOffset
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
        repository = TaskRepositoryImpl(
            database = database,
            taskDao = database.taskDao(),
            taskOrderDao = database.taskOrderDao(),
            listDao = database.listDao(),
            spaceDao = database.spaceDao(),
            clock = clock
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
