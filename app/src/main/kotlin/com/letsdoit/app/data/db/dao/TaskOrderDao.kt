package com.letsdoit.app.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.letsdoit.app.data.db.entities.TaskOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskOrderDao {
    @Query("SELECT * FROM task_order WHERE column = :column ORDER BY orderInColumn")
    fun observeColumn(column: String): Flow<List<TaskOrderEntity>>

    @Query("SELECT * FROM task_order WHERE column = :column ORDER BY orderInColumn")
    suspend fun listByColumn(column: String): List<TaskOrderEntity>

    @Query("SELECT * FROM task_order WHERE taskId = :taskId")
    suspend fun findForTask(taskId: Long): TaskOrderEntity?

    @Upsert
    suspend fun upsert(order: TaskOrderEntity)

    @Query("DELETE FROM task_order WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)

    @Transaction
    suspend fun rewrite(column: String, orders: List<TaskOrderEntity>) {
        orders.forEachIndexed { index, entity ->
            upsert(entity.copy(column = column, orderInColumn = index))
        }
    }
}
