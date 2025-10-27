package com.polaralias.letsdoit.data.search

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.polaralias.letsdoit.data.db.dao.TaskDao
import com.polaralias.letsdoit.data.db.entities.SubtaskEntity
import com.polaralias.letsdoit.data.db.entities.TaskEntity
import com.polaralias.letsdoit.data.db.entities.TaskWithSubtasksRelation
import com.polaralias.letsdoit.data.model.Subtask
import com.polaralias.letsdoit.data.model.Task
import com.polaralias.letsdoit.data.model.TaskWithSubtasks
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao,
    private val dataStore: DataStore<Preferences>,
    private val clock: Clock,
    moshi: Moshi
) : SearchRepository {
    private val historyKey = stringPreferencesKey("search_history")
    private val historyAdapter: JsonAdapter<List<String>> = moshi.adapter(Types.listType)

    override val history: Flow<List<String>> = dataStore.data.map { preferences ->
        val stored = preferences[historyKey]
        if (stored.isNullOrBlank()) emptyList() else runCatching { historyAdapter.fromJson(stored) }.getOrNull() ?: emptyList()
    }

    override fun search(query: String): Flow<List<TaskWithSubtasks>> {
        if (query.isBlank()) return flowOf(emptyList())
        val expression = buildFtsQuery(query)
        return taskDao.searchWithSubtasks(expression).map { relations -> relations.map { it.toModel() } }
    }

    override fun smartFilter(kind: SmartFilterKind): Flow<List<TaskWithSubtasks>> {
        return when (kind) {
            SmartFilterKind.DueToday -> {
                val zone = ZoneId.systemDefault()
                val now = Instant.now(clock).atZone(zone)
                val today = now.toLocalDate()
                val start = today.atStartOfDay(zone).toInstant()
                val end = today.plusDays(1).atStartOfDay(zone).toInstant()
                taskDao.filterDueToday(start, end).map { list -> list.map { it.toModel() } }
            }
            SmartFilterKind.Overdue -> {
                val now = Instant.now(clock)
                taskDao.filterOverdue(now).map { list -> list.map { it.toModel() } }
            }
            SmartFilterKind.NoDueDate -> taskDao.filterNoDueDate().map { list -> list.map { it.toModel() } }
            SmartFilterKind.HighPriority -> taskDao.filterHighPriority().map { list -> list.map { it.toModel() } }
            SmartFilterKind.LinkedToClickUp -> taskDao.filterLinkedToClickUp().map { list -> list.map { it.toModel() } }
            SmartFilterKind.Shared -> taskDao.filterShared().map { list -> list.map { it.toModel() } }
        }
    }

    override suspend fun recordQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        dataStore.edit { preferences ->
            val stored = preferences[historyKey]
            val current = if (stored.isNullOrBlank()) emptyList() else runCatching { historyAdapter.fromJson(stored) }.getOrNull() ?: emptyList()
            val updated = listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) }
            preferences[historyKey] = historyAdapter.toJson(updated.take(MAX_HISTORY))
        }
    }

    override suspend fun removeQuery(query: String) {
        dataStore.edit { preferences ->
            val stored = preferences[historyKey]
            if (stored.isNullOrBlank()) return@edit
            val current = runCatching { historyAdapter.fromJson(stored) }.getOrNull() ?: return@edit
            val updated = current.filterNot { it.equals(query, ignoreCase = true) }
            if (updated.isEmpty()) {
                preferences.remove(historyKey)
            } else {
                preferences[historyKey] = historyAdapter.toJson(updated.take(MAX_HISTORY))
            }
        }
    }

    private fun buildFtsQuery(value: String): String {
        val terms = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (terms.isEmpty()) return ""
        val cleanedTerms = terms.mapNotNull { term ->
            val cleaned = term.replace("'", "").replace("\"", "")
            cleaned.takeIf { it.isNotEmpty() }?.let { "$it*" }
        }
        return cleanedTerms.joinToString(" AND ")
    }

    private fun TaskWithSubtasksRelation.toModel(): TaskWithSubtasks = TaskWithSubtasks(
        task = task.toModel(),
        subtasks = subtasks.sortedBy { it.orderInParent }.map { it.toModel() }
    )

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

    private object Types {
        val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, String::class.java)
    }

    companion object {
        private const val MAX_HISTORY = 20
    }
}
