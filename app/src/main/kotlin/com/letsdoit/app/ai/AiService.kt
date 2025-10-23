package com.letsdoit.app.ai

import com.letsdoit.app.security.SecurePrefs
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class AiService @Inject constructor(private val securePrefs: SecurePrefs) {
    suspend fun suggestSubtasks(prompt: String): List<String> = withContext(Dispatchers.Default) {
        val key = securePrefs.read("openai_key")
        if (key.isNullOrBlank()) {
            emptyList()
        } else {
            prompt.split(" and ", ",", ".")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .mapIndexed { index, text ->
                    if (text.contains(" ")) text else "Step ${index + 1}: $text"
                }
        }
    }
}
