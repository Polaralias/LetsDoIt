package com.letsdoit.app.data.task

import com.letsdoit.app.data.db.dao.ListDao
import com.letsdoit.app.data.db.dao.SpaceDao
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.dao.TaskOrderDao
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.data.db.entities.TaskOrderEntity
import com.letsdoit.app.data.model.Task
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    fun observeTimeline(): Flow<List<Task>>
    fun observeLists(): Flow<List<ListEntity>>
    suspend fun ensureDefaultList(): Long
    suspend fun addTask(task: NewTask): Long
    suspend fun updateTask(task: Task)
    suspend fun updateCompletion(taskId: Long, completed: Boolean)
    suspend fun deleteTask(taskId: Long)
    suspend fun getTask(taskId: Long): Task?
    suspend fun reorderList(ids: List<Long>)
    suspend fun moveTaskToColumn(taskId: Long, column: String, toIndex: Int)
    suspend fun setTimeline(taskId: Long, startAt: Long?, durationMinutes: Int?)
    suspend fun setPriority(taskId: Long, priority: Int)
}

data class NewTask(
    val listId: Long,
    val title: String,
    val notes: String? = null,
    val dueAt: Instant? = null,
    val repeatRule: String? = null
)

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val taskOrderDao: TaskOrderDao,
    private val listDao: ListDao,
    private val spaceDao: SpaceDao,
    private val clock: Clock
) : TaskRepository {
    private val defaultListName = "Inbox"
    private val defaultSpaceName = "Everywhere"
    private val defaultColumn = "To do"

    override fun observeTasks(): Flow<List<Task>> = taskDao.observeAll().map { list ->
        list.map { it.toModel() }
    }

    override fun observeTimeline(): Flow<List<Task>> = taskDao.observeTimeline().map { list ->
        list.map { it.toModel() }
    }

    override fun observeLists(): Flow<List<ListEntity>> = listDao.observeAll()

    override suspend fun ensureDefaultList(): Long {
        val existing = listDao.findByName(defaultListName)
        if (existing != null) {
            return existing.id
        }
        val space = ensureDefaultSpace()
        val entity = ListEntity(name = defaultListName, spaceId = space.id)
        listDao.upsert(entity)
        return listDao.findByName(defaultListName)?.id ?: space.id
    }

    override suspend fun addTask(task: NewTask): Long {
        val now = Instant.now(clock)
        val entity = TaskEntity(
            listId = task.listId,
            title = task.title,
            notes = task.notes,
            dueAt = task.dueAt,
            repeatRule = task.repeatRule,
            createdAt = now,
            updatedAt = now,
            priority = 2,
            orderInList = 0,
            startAt = null,
            durationMinutes = null,
            calendarEventId = null,
            column = defaultColumn
        )
        return taskDao.upsert(entity)
    }

    override suspend fun updateTask(task: Task) {
        val entity = TaskEntity(
            id = task.id,
            listId = task.listId,
            title = task.title,
            notes = task.notes,
            dueAt = task.dueAt,
            repeatRule = task.repeatRule,
            createdAt = task.createdAt,
            updatedAt = Instant.now(clock),
            completed = task.completed,
            priority = task.priority,
            orderInList = task.orderInList,
            startAt = task.startAt?.toEpochMilli(),
            durationMinutes = task.durationMinutes,
            calendarEventId = task.calendarEventId,
            column = task.column
        )
        taskDao.upsert(entity)
    }

    override suspend fun updateCompletion(taskId: Long, completed: Boolean) {
        taskDao.updateCompletion(taskId, completed, Instant.now(clock))
    }

    override suspend fun deleteTask(taskId: Long) {
        taskDao.delete(taskId)
        taskOrderDao.deleteForTask(taskId)
    }

    override suspend fun getTask(taskId: Long): Task? {
        return taskDao.getById(taskId)?.toModel()
    }

    override suspend fun reorderList(ids: List<Long>) {
        val now = Instant.now(clock)
        ids.forEachIndexed { index, id ->
            taskDao.updateOrderInList(id, index, now)
        }
    }

    override suspend fun moveTaskToColumn(taskId: Long, column: String, toIndex: Int) {
        val now = Instant.now(clock)
        val existing = taskOrderDao.findForTask(taskId)
        existing?.let {
            taskOrderDao.deleteForTask(taskId)
            val sourceOrders = taskOrderDao.listByColumn(it.column)
            taskOrderDao.rewrite(it.column, sourceOrders)
        } ?: taskOrderDao.deleteForTask(taskId)
        val targetOrders = taskOrderDao.listByColumn(column).filterNot { it.taskId == taskId }.toMutableList()
        val index = toIndex.coerceIn(0, targetOrders.size)
        val entry = TaskOrderEntity(id = existing?.id ?: 0, taskId = taskId, column = column, orderInColumn = index)
        targetOrders.add(index, entry)
        taskOrderDao.rewrite(column, targetOrders)
        taskDao.updateColumn(taskId, column, now)
    }

    override suspend fun setTimeline(taskId: Long, startAt: Long?, durationMinutes: Int?) {
        val now = Instant.now(clock)
        taskDao.updateTimeline(taskId, startAt, durationMinutes, now)
    }

    override suspend fun setPriority(taskId: Long, priority: Int) {
        val now = Instant.now(clock)
        taskDao.updatePriority(taskId, priority, now)
    }

    private fun TaskEntity.toModel(): Task = Task(
        id = id,
        listId = listId,
        title = title,
        notes = notes,
        dueAt = dueAt,
        repeatRule = repeatRule,
        createdAt = createdAt,
        updatedAt = updatedAt,
        completed = completed,
        priority = priority,
        orderInList = orderInList,
        startAt = startAt?.let { Instant.ofEpochMilli(it) },
        durationMinutes = durationMinutes,
        calendarEventId = calendarEventId,
        column = column
    )

    private suspend fun ensureDefaultSpace(): SpaceEntity {
        val existing = spaceDao.findByName(defaultSpaceName)
        if (existing != null) {
            return existing
        }
        spaceDao.upsert(SpaceEntity(name = defaultSpaceName))
        return spaceDao.findByName(defaultSpaceName) ?: SpaceEntity(name = defaultSpaceName)
    }
}
