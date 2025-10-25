package com.letsdoit.app.ai

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.letsdoit.app.security.SecurePrefs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface AiActionResult<out T> {
    data class Success<T>(val data: T) : AiActionResult<T>
    data object MissingKey : AiActionResult<Nothing>
    data object Offline : AiActionResult<Nothing>
}

data class PlanSuggestion(
    val title: String,
    val startAt: Long?,
    val durationMinutes: Int?,
    val dueAt: Long?
)

@Singleton
open class AiService @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val clock: Clock,
    @ApplicationContext private val context: Context
) {
    open suspend fun splitIntoSubtasks(title: String, notes: String?): AiActionResult<List<String>> = withContext(Dispatchers.Default) {
        when (val status = status()) {
            is AiActionResult.Success -> {
                val prompt = buildString {
                    append(title)
                    if (!notes.isNullOrBlank()) {
                        append(" ")
                        append(notes)
                    }
                }
                val items = prompt
                    .split('.', '\n', ';')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .ifEmpty { listOf(title.trim()) }
                AiActionResult.Success(items)
            }
            else -> status
        }
    }

    open suspend fun draftPlan(title: String, notes: String?): AiActionResult<List<PlanSuggestion>> = withContext(Dispatchers.Default) {
        when (val status = status()) {
            is AiActionResult.Success -> {
                val now = Instant.now(clock).truncatedTo(ChronoUnit.MINUTES)
                val base = now.plus(15, ChronoUnit.MINUTES)
                val segments = buildSequence(title, notes)
                val suggestions = segments.mapIndexed { index, text ->
                    val start = base.plus(index.toLong() * 60, ChronoUnit.MINUTES)
                    PlanSuggestion(
                        title = text,
                        startAt = start.toEpochMilli(),
                        durationMinutes = 60,
                        dueAt = start.plus(60, ChronoUnit.MINUTES).toEpochMilli()
                    )
                }
                AiActionResult.Success(suggestions)
            }
            else -> status
        }
    }

    private fun buildSequence(title: String, notes: String?): List<String> {
        val combined = listOfNotNull(title.takeIf { it.isNotBlank() }, notes?.takeIf { it.isNotBlank() })
            .joinToString(". ")
        val chunks = combined.split('.', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (chunks.isEmpty()) {
            return listOf(title.trim())
        }
        return chunks
    }

    protected open fun status(): AiActionResult<Unit> {
        val key = securePrefs.read("openai_key")
        if (key.isNullOrBlank()) {
            return AiActionResult.MissingKey
        }
        if (key.startsWith("local_")) {
            return AiActionResult.Success(Unit)
        }
        if (!isOnline()) {
            return AiActionResult.Offline
        }
        return AiActionResult.Success(Unit)
    }

    protected open fun isOnline(): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = connectivity.activeNetwork ?: return false
        val capabilities = connectivity.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
