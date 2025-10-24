package com.letsdoit.app.bulk

import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.prefs.BulkPreferences
import com.letsdoit.app.data.prefs.ViewPreferences
import com.letsdoit.app.data.prefs.TimelineMode
import com.letsdoit.app.ui.viewmodel.BulkErrorUnknownBucket
import com.letsdoit.app.ui.viewmodel.BulkErrorUnknownList
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

data class ListResolution(
    val list: ListEntity?,
    val spaceName: String?,
    val error: String?
)

data class ColumnResolution(val value: String?, val error: String?)

data class MergedTaskResult(
    val title: String,
    val dueAt: Instant?,
    val repeatRule: String?,
    val remindOffsetMinutes: Int?,
    val priority: Int,
    val startAt: Instant?,
    val durationMinutes: Int?
)

fun resolveListToken(
    token: ListToken?,
    lists: List<ListEntity>,
    spaces: List<SpaceEntity>,
    defaultListId: Long?
): ListResolution {
    if (token == null) {
        val defaultList = lists.firstOrNull { it.id == defaultListId }
        val spaceName = defaultList?.let { list -> spaces.firstOrNull { it.id == list.spaceId }?.name }
        return ListResolution(list = defaultList, spaceName = spaceName, error = null)
    }
    val candidates = if (token.space == null) {
        lists
    } else {
        val space = resolveSpace(token.space, spaces)
        space?.let { spaceEntity ->
            lists.filter { it.spaceId == spaceEntity.id }
        } ?: return ListResolution(list = null, spaceName = null, error = BulkErrorUnknownList)
    }
    val exact = candidates.firstOrNull { it.name == token.list }
    val caseInsensitive = candidates.firstOrNull { it.name.equals(token.list, ignoreCase = true) }
    val prefix = candidates.firstOrNull { it.name.lowercase().startsWith(token.list.lowercase()) }
    val resolved = exact ?: caseInsensitive ?: prefix
    return if (resolved != null) {
        val spaceName = spaces.firstOrNull { it.id == resolved.spaceId }?.name
        ListResolution(list = resolved, spaceName = spaceName, error = null)
    } else {
        ListResolution(list = null, spaceName = null, error = BulkErrorUnknownList)
    }
}

fun resolveColumnToken(token: String?, columns: List<String>): ColumnResolution {
    if (token == null) return ColumnResolution(null, null)
    if (columns.isEmpty()) return ColumnResolution(token, null)
    val exact = columns.firstOrNull { it == token }
    val caseInsensitive = columns.firstOrNull { it.equals(token, ignoreCase = true) }
    val prefix = columns.firstOrNull { it.lowercase().startsWith(token.lowercase()) }
    val value = exact ?: caseInsensitive ?: prefix
    return if (value != null) {
        ColumnResolution(value, null)
    } else {
        ColumnResolution(null, BulkErrorUnknownBucket)
    }
}

fun mergeParsedTask(
    parsedTitle: String,
    dueAt: Instant?,
    repeatExpression: String?,
    remindOffsetMinutes: Int?,
    tokenPriority: Int?,
    preferences: BulkPreferences,
    viewPreferences: ViewPreferences,
    zoneId: ZoneId
): MergedTaskResult {
    val title = parsedTitle.take(200)
    val timeline = computeTimelineDefaults(dueAt, preferences, viewPreferences, zoneId)
    val priority = tokenPriority ?: 2
    return MergedTaskResult(
        title = title,
        dueAt = dueAt,
        repeatRule = repeatExpression,
        remindOffsetMinutes = remindOffsetMinutes,
        priority = priority,
        startAt = timeline.first,
        durationMinutes = timeline.second
    )
}

fun timelineDefaults(
    dueAt: Instant?,
    preferences: BulkPreferences,
    viewPreferences: ViewPreferences,
    zoneId: ZoneId
): Pair<Instant?, Int?> = computeTimelineDefaults(dueAt, preferences, viewPreferences, zoneId)

private fun resolveSpace(name: String, spaces: List<SpaceEntity>): SpaceEntity? {
    val exact = spaces.firstOrNull { it.name == name }
    if (exact != null) return exact
    val caseInsensitive = spaces.firstOrNull { it.name.equals(name, ignoreCase = true) }
    if (caseInsensitive != null) return caseInsensitive
    return spaces.firstOrNull { it.name.lowercase().startsWith(name.lowercase()) }
}

private fun computeTimelineDefaults(
    dueAt: Instant?,
    preferences: BulkPreferences,
    viewPreferences: ViewPreferences,
    zoneId: ZoneId
): Pair<Instant?, Int?> {
    if (dueAt == null) return Pair(null, null)
    val local = dueAt.atZone(zoneId)
    val hasTime = local.toLocalTime() != LocalTime.MIDNIGHT
    return if (hasTime && viewPreferences.timelineMode != TimelineMode.week) {
        Pair(dueAt, preferences.defaultDurationMinutes)
    } else {
        Pair(null, null)
    }
}
