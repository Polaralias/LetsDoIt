package com.letsdoit.app.test

import androidx.paging.PagingData
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.data.task.BulkCreateItem
import com.letsdoit.app.data.task.BulkCreateResult
import com.letsdoit.app.data.task.NewTask
import com.letsdoit.app.data.task.TaskRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTaskRepository : TaskRepository {
    private val tasks = mutableListOf<Task>()
    val completionUpdates = mutableListOf<Pair<Long, Boolean>>()
    private var defaultListId = 1L

    fun setTasks(items: List<Task>) {
        tasks.clear()
        tasks.addAll(items)
    }

    override fun observeTasks(): Flow<PagingData<Task>> = flowOf(PagingData.empty())

    override fun observeTimeline(): Flow<PagingData<Task>> = flowOf(PagingData.empty())

    override fun observeBoardColumn(column: String): Flow<PagingData<Task>> = flowOf(PagingData.empty())

    override fun observeLists(): Flow<List<ListEntity>> = flowOf(emptyList())

    override fun observeSpaces(): Flow<List<SpaceEntity>> = flowOf(emptyList())

    override suspend fun listLists(): List<ListEntity> = emptyList()

    override suspend fun listSpaces(): List<SpaceEntity> = emptyList()

    override suspend fun listAllTasks(): List<Task> = tasks.toList()

    override suspend fun listTodayTasks(): List<Task> = tasks.toList()

    override suspend fun ensureDefaultList(): Long = defaultListId

    override suspend fun resolveListByName(name: String): ListEntity? = null

    override suspend fun resolveListBySpace(spaceName: String, listName: String): ListEntity? = null

    override suspend fun addTask(task: NewTask): Long = ++defaultListId

    override suspend fun bulkCreate(items: List<BulkCreateItem>): BulkCreateResult =
        BulkCreateResult(createdCount = 0, issues = emptyList(), createdIds = emptyList())

    override suspend fun updateTask(task: Task) {
        tasks.replaceAll { existing -> if (existing.id == task.id) task else existing }
    }

    override suspend fun updateCompletion(taskId: Long, completed: Boolean) {
        completionUpdates.add(taskId to completed)
        if (completed) {
            tasks.removeAll { it.id == taskId }
        }
    }

    override suspend fun deleteTask(taskId: Long) {
        tasks.removeAll { it.id == taskId }
    }

    override suspend fun getTask(taskId: Long): Task? = tasks.find { it.id == taskId }

    override suspend fun reorderList(ids: List<Long>) {}

    override suspend fun moveTaskToColumn(taskId: Long, column: String, toIndex: Int) {}

    override suspend fun setTimeline(taskId: Long, startAt: Long?, durationMinutes: Int?) {}

    override suspend fun setPriority(taskId: Long, priority: Int) {}

    override suspend fun setDueDate(taskId: Long, dueAt: Instant?) {}

    override suspend fun linkCalendarEvent(taskId: Long, calendarEventId: Long) {}

    override suspend fun removeFromCalendar(taskId: Long) {}
}
