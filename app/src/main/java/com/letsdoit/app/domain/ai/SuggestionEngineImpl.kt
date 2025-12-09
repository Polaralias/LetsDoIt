package com.letsdoit.app.domain.ai

import com.letsdoit.app.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

class SuggestionEngineImpl @Inject constructor(
    private val taskRepository: TaskRepository
) : SuggestionEngine {

    override fun getSuggestions(): Flow<List<Suggestion>> {
        return taskRepository.getTasksFlow(null).map { tasks ->
            // Filter only completed tasks for analysis to see habits
            // Or use all tasks to find repetition.
            // "Analyze TaskDao history" usually means completed tasks.
            // But we can also look at recurring tasks.

            // For this phase, let's look at all tasks that were created in the last 60 days
            // and find titles that are repeated.

            val today = LocalDate.now()
            val dayOfWeek = today.dayOfWeek

            val recentTasks = tasks.filter {
                it.createdAt.toLocalDate().isAfter(today.minusDays(60))
            }

            // Group by title (case insensitive)
            val titleGroups = recentTasks.groupBy { it.title.trim().lowercase() }

            val suggestions = mutableListOf<Suggestion>()

            titleGroups.forEach { (lowercaseTitle, group) ->
                if (group.size < 2) return@forEach // Need at least some history

                // Check if this task is already in "To Do" or "In Progress" for today?
                // We shouldn't suggest it if it's already there and active.
                // Assuming "Open" or "To Do" or "In Progress" means active.
                val active = tasks.any {
                    it.title.trim().lowercase() == lowercaseTitle &&
                    (it.status == "Open" || it.status == "To Do" || it.status == "In Progress")
                }

                if (active) return@forEach

                // Analyze day of week
                val dayCounts = group.groupingBy { it.createdAt.dayOfWeek }.eachCount()
                val countOnThisDay = dayCounts[dayOfWeek] ?: 0

                val total = group.size
                val probability = countOnThisDay.toFloat() / total

                // If it happened at least twice on this day, and probability is reasonable
                if (countOnThisDay >= 2 && probability >= 0.3) {
                     val originalTitle = group.first().title // Pick one case
                     suggestions.add(
                         Suggestion(
                             title = originalTitle,
                             confidence = probability,
                             reason = "You usually do this on ${dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}s"
                         )
                     )
                }
            }

            suggestions.sortedByDescending { it.confidence }
        }
    }
}
