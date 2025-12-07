package com.letsdoit.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.letsdoit.app.data.local.dao.TaskDao
import com.letsdoit.app.data.local.entity.ListEntity
import com.letsdoit.app.data.local.entity.SpaceEntity
import com.letsdoit.app.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TaskDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var taskDao: TaskDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        taskDao = db.taskDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun insertAndGetTask() = runBlocking {
        // Need space and list due to foreign key constraints
        val space = SpaceEntity("space1", "My Space", 0, 0)
        db.spaceDao().insertSpace(space)
        val list = ListEntity("list1", null, "space1", "My List", "#FFFFFF")
        db.listDao().insertList(list)

        val task = TaskEntity(
            id = "task1",
            listId = "list1",
            title = "Test Task",
            description = "Description",
            status = "OPEN",
            dueDate = null,
            priority = 1,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        taskDao.insertTask(task)

        val byId = taskDao.getTaskById("task1").first()
        assertEquals(task.title, byId?.title)
        assertEquals(task.id, byId?.id)
    }
}
