package com.letsdoit.app

import android.content.Context
import com.letsdoit.app.ai.AiActionResult
import com.letsdoit.app.ai.AiService
import com.letsdoit.app.ai.PlanSuggestion
import com.letsdoit.app.security.SecurePrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAiService @Inject constructor(
    securePrefs: SecurePrefs,
    clock: Clock,
    @ApplicationContext context: Context
) : AiService(securePrefs, clock, context) {
    override suspend fun splitIntoSubtasks(title: String, notes: String?): AiActionResult<List<String>> {
        return AiActionResult.Success(listOf("First generated subtask", "Second generated subtask"))
    }

    override suspend fun draftPlan(title: String, notes: String?): AiActionResult<List<PlanSuggestion>> {
        return AiActionResult.Success(
            listOf(
                PlanSuggestion(title = "Morning focus", startAt = 1714551000000, durationMinutes = 45, dueAt = 1714553700000),
                PlanSuggestion(title = "Wrap up", startAt = null, durationMinutes = null, dueAt = null)
            )
        )
    }

    override fun status(): AiActionResult<Unit> = AiActionResult.Success(Unit)
}
