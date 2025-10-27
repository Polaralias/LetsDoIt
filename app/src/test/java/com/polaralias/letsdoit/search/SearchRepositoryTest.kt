package com.polaralias.letsdoit.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.polaralias.letsdoit.data.db.AppDatabase
import com.polaralias.letsdoit.data.db.dao.TaskDao
import com.polaralias.letsdoit.data.db.entities.ListEntity
import com.polaralias.letsdoit.data.db.entities.SpaceEntity
import com.polaralias.letsdoit.data.db.entities.SubtaskEntity
import com.polaralias.letsdoit.data.db.entities.TaskEntity
import com.polaralias.letsdoit.data.model.TaskWithSubtasks
import com.polaralias.letsdoit.data.search.SearchRepository
import com.polaralias.letsdoit.data.search.SearchRepositoryImpl
import com.squareup.moshi.Moshi
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class SearchRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var taskDao: TaskDao
    private lateinit var repository: SearchRepository
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var scope: TestScope
    private lateinit var context: Context

    @BeforeTest
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        taskDao = database.taskDao()
        scope = TestScope(UnconfinedTestDispatcher())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) {
            context.preferencesDataStoreFile("search-test")
        }
        val clock = Clock.fixed(Instant.parse("2024-06-01T10:00:00Z"), ZoneOffset.UTC)
        repository = SearchRepositoryImpl(taskDao, dataStore, clock, Moshi.Builder().build())
    }

    @AfterTest
    fun tearDown() {
        database.close()
        scope.cancel()
    }

    @Test
    fun searchMatchesTitlesAndNotes() = runTest {
        val listId = insertList()
        val taskId = taskDao.upsert(
            TaskEntity(
                listId = listId,
                title = "Plan quarterly release",
                notes = "Coordinate with marketing",
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        val results = repository.search("plan release").first()
        assertContainsTask(results, taskId)
    }

    @Test
    fun searchMatchesSubtaskContent() = runTest {
        val listId = insertList()
        val taskId = taskDao.upsert(
            TaskEntity(
                listId = listId,
                title = "Prepare launch",
                notes = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )
        )
        database.subtaskDao().upsert(
            SubtaskEntity(
                parentTaskId = taskId,
                title = "Draft press kit",
                done = false,
                dueAt = null,
                orderInParent = 0,
                startAt = null,
                durationMinutes = null
            )
        )
        val results = repository.search("press kit").first()
        assertContainsTask(results, taskId)
    }

    private suspend fun insertList(): Long {
        database.spaceDao().upsert(SpaceEntity(name = "Space"))
        val spaceId = requireNotNull(database.spaceDao().findByName("Space")).id
        database.listDao().upsert(ListEntity(spaceId = spaceId, name = "List"))
        return requireNotNull(database.listDao().findByName("List")).id
    }

    private fun assertContainsTask(results: List<TaskWithSubtasks>, taskId: Long) {
        assertTrue(results.any { it.task.id == taskId }, "Expected task $taskId in results")
    }
}
