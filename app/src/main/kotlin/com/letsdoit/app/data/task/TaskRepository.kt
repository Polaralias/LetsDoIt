package com.letsdoit.app.data.task

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.letsdoit.app.data.db.AppDatabase
import com.letsdoit.app.data.db.dao.ListDao
import com.letsdoit.app.data.db.dao.SpaceDao
import com.letsdoit.app.data.db.dao.TaskDao
import com.letsdoit.app.data.db.dao.TaskOrderDao
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import com.letsdoit.app.data.db.entities.TaskOrderEntity
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.data.subtask.SubtaskRepository
import com.letsdoit.app.reminders.ReminderCoordinator
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface TaskRepository {
    fun observeTasks(): Flow<PagingData<Task>>
    fun observeTimeline(): Flow<PagingData<Task>>
    fun observeBoardColumn(column: String): Flow<PagingData<Task>>
    fun observeLists(): Flow<List<ListEntity>>
    fun observeSpaces(): Flow<List<SpaceEntity>>
    suspend fun listLists(): List<ListEntity>
    suspend fun listSpaces(): List<SpaceEntity>
    suspend fun listAllTasks(): List<Task>
    suspend fun listTodayTasks(): List<Task>
    suspend fun ensureDefaultList(): Long
    suspend fun resolveListByName(name: String): ListEntity?
    suspend fun resolveListBySpace(spaceName: String, listName: String): ListEntity?
    suspend fun addTask(task: NewTask): Long
    suspend fun bulkCreate(items: List<BulkCreateItem>): BulkCreateResult
    suspend fun updateTask(task: Task)
    suspend fun updateCompletion(taskId: Long, completed: Boolean)
    suspend fun deleteTask(taskId: Long)
    suspend fun getTask(taskId: Long): Task?
    suspend fun reorderList(ids: List<Long>)
    suspend fun moveTaskToColumn(taskId: Long, column: String, toIndex: Int)
    suspend fun setTimeline(taskId: Long, startAt: Long?, durationMinutes: Int?)
    suspend fun setPriority(taskId: Long, priority: Int)
    suspend fun setDueDate(taskId: Long, dueAt: Instant?)
}

data class NewTask(
    val listId: Long,
    val title: String,
    val notes: String? = null,
    val dueAt: Instant? = null,
    val repeatRule: String? = null,
    val remindOffsetMinutes: Int? = null
)

data class BulkCreateItem(
    val listId: Long,
    val title: String,
    val notes: String? = null,
    val dueAt: Instant? = null,
    val repeatRule: String? = null,
    val remindOffsetMinutes: Int? = null,
    val priority: Int = 2,
    val column: String? = null,
    val startAt: Instant? = null,
    val durationMinutes: Int? = null
)

data class LineIssue(val lineIndex: Int, val message: String)

data class BulkCreateResult(
    val createdCount: Int,
    val issues: List<LineIssue>,
    val createdIds: List<Long>
)

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val taskDao: TaskDao,
    private val taskOrderDao: TaskOrderDao,
    private val listDao: ListDao,
    private val spaceDao: SpaceDao,
    private val clock: Clock,
    private val reminderCoordinator: ReminderCoordinator,
    private val subtaskRepository: SubtaskRepository
) : TaskRepository {
    private val defaultListName = "Inbox"
    private val defaultSpaceName = "Everywhere"
    private val defaultColumn = "To do"

    private val pagingConfig = PagingConfig(pageSize = 60, prefetchDistance = 20, enablePlaceholders = false)

    override fun observeTasks(): Flow<PagingData<Task>> = Pager(
        config = pagingConfig,
        pagingSourceFactory = { taskDao.pagingAll() }
    ).flow.map { data -> data.map { it.toModel() } }

    override fun observeTimeline(): Flow<PagingData<Task>> = Pager(
        config = pagingConfig,
        pagingSourceFactory = { taskDao.pagingTimeline() }
    ).flow.map { data -> data.map { it.toModel() } }

    override fun observeBoardColumn(column: String): Flow<PagingData<Task>> = Pager(
        config = pagingConfig,
        pagingSourceFactory = { taskDao.pagingBoardColumn(column) }
    ).flow.map { data -> data.map { it.toModel() } }

    override fun observeLists(): Flow<List<ListEntity>> = listDao.observeAll()

    override fun observeSpaces(): Flow<List<SpaceEntity>> = spaceDao.observeSpaces()

    override suspend fun listLists(): List<ListEntity> = listDao.listAll()

    override suspend fun listSpaces(): List<SpaceEntity> = spaceDao.listAll()

    override suspend fun listAllTasks(): List<Task> = taskDao.listAll().map { it.toModel() }

    override suspend fun listTodayTasks(): List<Task> {
        val now = Instant.now(clock)
        val zone = clock.zone
        val startOfDay = now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
        val endOfDay = startOfDay.plus(1, ChronoUnit.DAYS)
        val overdue = taskDao.listDueBefore(startOfDay)
        val today = taskDao.listDueBetween(startOfDay, endOfDay)
        return (overdue + today)
            .sortedWith(compareBy({ it.dueAt }, { it.createdAt }))
            .map { it.toModel() }
    }

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
            remindOffsetMinutes = task.remindOffsetMinutes,
            createdAt = now,
            updatedAt = now,
            priority = 2,
            orderInList = 0,
            startAt = null,
            durationMinutes = null,
            calendarEventId = null,
            column = defaultColumn
        )
        val taskId = taskDao.upsert(entity)
        reminderCoordinator.onTaskSaved(taskId)
        return taskId
    }

    override suspend fun bulkCreate(items: List<BulkCreateItem>): BulkCreateResult {
        if (items.isEmpty()) {
            return BulkCreateResult(createdCount = 0, issues = emptyList(), createdIds = emptyList())
        }
        val indexedItems = items.withIndex()
        val createdIds = MutableList<Long?>(items.size) { null }
        database.withTransaction {
            val grouped = indexedItems.groupBy { it.value.listId }
            val columnCounts = mutableMapOf<String, Int>()
            grouped.forEach { (listId, entries) ->
                var orderIndex = taskDao.maxOrderInList(listId)?.plus(1) ?: 0
                entries.forEach { entry ->
                    val item = entry.value
                    val now = Instant.now(clock)
                    val column = item.column ?: defaultColumn
                    val entity = TaskEntity(
                        listId = listId,
                        title = item.title,
                        notes = item.notes,
                        dueAt = item.dueAt,
                        repeatRule = item.repeatRule,
                        remindOffsetMinutes = item.remindOffsetMinutes,
                        createdAt = now,
                        updatedAt = now,
                        priority = item.priority,
                        orderInList = orderIndex,
                        startAt = item.startAt?.toEpochMilli(),
                        durationMinutes = item.durationMinutes,
                        column = column
                    )
                    val taskId = taskDao.upsert(entity)
                    createdIds[entry.index] = taskId
                    val orderInColumn = columnCounts.getOrPut(column) {
                        taskOrderDao.listByColumn(column).size
                    }
                    val orderEntity = TaskOrderEntity(
                        taskId = taskId,
                        column = column,
                        orderInColumn = orderInColumn
                    )
                    taskOrderDao.upsert(orderEntity)
                    columnCounts[column] = orderInColumn + 1
                    orderIndex += 1
                }
            }
        }
        val created = createdIds.mapNotNull { it }
        created.forEach { id ->
            reminderCoordinator.onTaskSaved(id)
        }
        return BulkCreateResult(createdCount = created.size, issues = emptyList(), createdIds = created)
    }

    override suspend fun resolveListByName(name: String): ListEntity? {
        val lower = name.lowercase()
        val all = listDao.listAll()
        val exact = all.firstOrNull { it.name.equals(name, ignoreCase = false) }
        if (exact != null) return exact
        val caseInsensitive = all.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (caseInsensitive != null) return caseInsensitive
        return all.firstOrNull { it.name.lowercase().startsWith(lower) }
    }

    override suspend fun resolveListBySpace(spaceName: String, listName: String): ListEntity? {
        val spaces = spaceDao.listAll()
        val targetSpace = spaces.firstOrNull { it.name.equals(spaceName, ignoreCase = true) }
            ?: spaces.firstOrNull { it.name.lowercase().startsWith(spaceName.lowercase()) }
            ?: return null
        val lists = listDao.listAll().filter { it.spaceId == targetSpace.id }
        val exact = lists.firstOrNull { it.name == listName }
        if (exact != null) return exact
        val caseInsensitive = lists.firstOrNull { it.name.equals(listName, ignoreCase = true) }
        if (caseInsensitive != null) return caseInsensitive
        return lists.firstOrNull { it.name.lowercase().startsWith(listName.lowercase()) }
    }

    override suspend fun updateTask(task: Task) {
        val entity = TaskEntity(
            id = task.id,
            listId = task.listId,
            title = task.title,
            notes = task.notes,
            dueAt = task.dueAt,
            repeatRule = task.repeatRule,
            remindOffsetMinutes = task.remindOffsetMinutes,
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
        reminderCoordinator.onTaskSaved(task.id)
    }

    override suspend fun updateCompletion(taskId: Long, completed: Boolean) {
        if (completed) {
            reminderCoordinator.onTaskCompleted(taskId)
        } else {
            taskDao.updateCompletion(taskId, false, Instant.now(clock))
            reminderCoordinator.onTaskSaved(taskId)
        }
    }

    override suspend fun deleteTask(taskId: Long) {
        taskDao.delete(taskId)
        taskOrderDao.deleteForTask(taskId)
        subtaskRepository.deleteForTask(taskId)
        reminderCoordinator.onTaskDeleted(taskId)
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

    override suspend fun setDueDate(taskId: Long, dueAt: Instant?) {
        val now = Instant.now(clock)
        taskDao.updateDueDate(taskId, dueAt, now)
        reminderCoordinator.onTaskSaved(taskId)
    }

    private fun TaskEntity.toModel(): Task = Task(
        id = id,
        listId = listId,
        title = title,
        notes = notes,
        dueAt = dueAt,
        repeatRule = repeatRule,
        remindOffsetMinutes = remindOffsetMinutes,
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
