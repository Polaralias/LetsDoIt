package com.polaralias.letsdoit.ai.process

import com.polaralias.letsdoit.ai.AiInput
import com.polaralias.letsdoit.ai.model.AiParseResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiParseCache @Inject constructor() {
    private val cache = object : LinkedHashMap<String, CachedParse>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedParse>?): Boolean {
            return size > 50
        }
    }

    fun get(input: AiInput): AiParseResult? {
        val key = keyFor(input)
        return synchronized(cache) { cache[key]?.result }
    }

    fun put(input: AiInput, result: AiParseResult) {
        val key = keyFor(input)
        synchronized(cache) {
            cache[key] = CachedParse(result)
        }
    }

    private fun keyFor(input: AiInput): String {
        return buildString {
            append(input.transcript)
            append("::")
            append(input.projectName ?: "")
            append("::")
            append(input.timezone ?: "")
            if (input.metadata.isNotEmpty()) {
                input.metadata.entries.sortedBy { it.key }.forEach { (k, v) ->
                    append("::")
                    append(k)
                    append("=")
                    append(v)
                }
            }
        }
    }
}

data class CachedParse(val result: AiParseResult)
