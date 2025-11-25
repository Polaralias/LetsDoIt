package com.example.letsdoit.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists")
    suspend fun getTaskLists(): List<TaskListEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskList(taskListEntity: TaskListEntity)
}
