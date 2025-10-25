package com.letsdoit.app.data.db

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letsdoit.app.data.db.dao.TaskDao
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
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TaskPagingInstrumentationTest {
    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        seedTasks()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun pagingAllMatchesOrdering() = runBlocking {
        val page = taskDao.pagingAll().load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page
        val data = page.data
        val expected = taskDao.listAll().sortedWith(taskOrdering())
        assertEquals(expected.map { it.id }, data.map { it.id })
        assertEquals(expected.size, data.size)
    }

    @Test
    fun pagingTimelineFiltersAndSorts() = runBlocking {
        val page = taskDao.pagingTimeline().load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 100, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page
        val data = page.data
        assertTrue(data.all { it.dueAt != null })
        val expectedOrder = data.sortedBy { it.dueAt }
        assertEquals(expectedOrder.map { it.id }, data.map { it.id })
    }

    private fun seedTasks() = runBlocking {
        database.spaceDao().upsert(SpaceEntity(name = "Test Space"))
        val spaceId = requireNotNull(database.spaceDao().findByName("Test Space")).id
        database.listDao().upsert(ListEntity(spaceId = spaceId, name = "Test List"))
        val listId = requireNotNull(database.listDao().findByName("Test List")).id
        val base = Instant.parse("2024-01-01T10:00:00Z")
        listOf(
            TaskEntity(listId = listId, title = "Completed later", createdAt = base, updatedAt = base, completed = true, dueAt = base.plusSeconds(3600)),
            TaskEntity(listId = listId, title = "Due soon", createdAt = base.plusSeconds(10), updatedAt = base.plusSeconds(10), dueAt = base.plusSeconds(600)),
            TaskEntity(listId = listId, title = "No due", createdAt = base.plusSeconds(20), updatedAt = base.plusSeconds(20)),
            TaskEntity(listId = listId, title = "Due later", createdAt = base.plusSeconds(30), updatedAt = base.plusSeconds(30), dueAt = base.plusSeconds(7200))
        ).forEach { taskDao.upsert(it) }
    }

    private fun taskOrdering(): Comparator<TaskEntity> {
        return compareBy<TaskEntity>({ if (it.completed) 1 else 0 }, { it.dueAt == null }, { it.dueAt ?: Instant.MAX }, { it.createdAt })
    }
}
