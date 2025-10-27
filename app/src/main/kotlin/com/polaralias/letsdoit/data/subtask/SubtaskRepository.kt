package com.polaralias.letsdoit.data.subtask

import androidx.room.withTransaction
import com.polaralias.letsdoit.data.db.AppDatabase
import com.polaralias.letsdoit.data.db.dao.SubtaskDao
import com.polaralias.letsdoit.data.db.entities.SubtaskEntity
import com.polaralias.letsdoit.data.model.Subtask
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface SubtaskRepository {
    fun observeSubtasks(taskId: Long): Flow<List<Subtask>>
    suspend fun listSubtasks(taskId: Long): List<Subtask>
    suspend fun createSubtasks(taskId: Long, items: List<NewSubtask>): List<Long>
    suspend fun updateCompletion(subtaskId: Long, done: Boolean)
    suspend fun reorder(taskId: Long, orderedIds: List<Long>)
    suspend fun deleteSubtasks(ids: List<Long>)
    suspend fun deleteForTask(taskId: Long)
}

data class NewSubtask(
    val title: String,
    val dueAt: Long? = null,
    val startAt: Long? = null,
    val durationMinutes: Int? = null
)

@Singleton
class SubtaskRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val subtaskDao: SubtaskDao
) : SubtaskRepository {
    override fun observeSubtasks(taskId: Long): Flow<List<Subtask>> = subtaskDao.observeByParent(taskId).map { list ->
        list.sortedBy { it.orderInParent }.map { it.toModel() }
    }

    override suspend fun listSubtasks(taskId: Long): List<Subtask> = subtaskDao.listByParent(taskId).map { it.toModel() }

    override suspend fun createSubtasks(taskId: Long, items: List<NewSubtask>): List<Long> {
        if (items.isEmpty()) return emptyList()
        val ids = mutableListOf<Long>()
        database.withTransaction {
            val existing = subtaskDao.listByParent(taskId)
            var order = existing.maxOfOrNull { it.orderInParent }?.plus(1) ?: 0
            items.forEach { item ->
                val entity = SubtaskEntity(
                    parentTaskId = taskId,
                    title = item.title,
                    done = false,
                    dueAt = item.dueAt,
                    orderInParent = order,
                    startAt = item.startAt,
                    durationMinutes = item.durationMinutes
                )
                val id = subtaskDao.upsert(entity)
                ids.add(id)
                order += 1
            }
        }
        return ids
    }

    override suspend fun updateCompletion(subtaskId: Long, done: Boolean) {
        subtaskDao.updateDone(subtaskId, done)
    }

    override suspend fun reorder(taskId: Long, orderedIds: List<Long>) {
        database.withTransaction {
            orderedIds.forEachIndexed { index, id ->
                subtaskDao.updateOrder(id, index)
            }
        }
    }

    override suspend fun deleteSubtasks(ids: List<Long>) {
        if (ids.isEmpty()) return
        subtaskDao.deleteByIds(ids)
    }

    override suspend fun deleteForTask(taskId: Long) {
        subtaskDao.deleteByParent(taskId)
    }

    private fun SubtaskEntity.toModel(): Subtask = Subtask(
        id = id,
        parentTaskId = parentTaskId,
        title = title,
        done = done,
        dueAt = dueAt,
        orderInParent = orderInParent,
        startAt = startAt,
        durationMinutes = durationMinutes
    )
}
