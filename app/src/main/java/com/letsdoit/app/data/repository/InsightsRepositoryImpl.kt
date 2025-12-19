package com.letsdoit.app.data.repository

import com.letsdoit.app.data.local.dao.TaskDao
import com.letsdoit.app.domain.model.InsightsData
import com.letsdoit.app.domain.repository.InsightsRepository
import com.letsdoit.app.domain.util.TaskStatusUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class InsightsRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : InsightsRepository {

    override fun getInsights(): Flow<InsightsData> {
        return taskDao.getAllTasks().map { tasks ->
             val activeTasks = tasks.filter { !TaskStatusUtil.isCompleted(it.status) }
             val completedTasks = tasks.filter { TaskStatusUtil.isCompleted(it.status) }

             val byPriority = tasks.groupingBy { it.priority }.eachCount()
             val byStatus = tasks.groupingBy { it.status }.eachCount()

             InsightsData(
                 totalActive = activeTasks.size,
                 totalCompleted = completedTasks.size,
                 tasksByPriority = byPriority,
                 tasksByStatus = byStatus
             )
        }
    }
}
