package com.letsdoit.app.ai.speech

class PartialChunkAssembler {
    private val chunks = mutableListOf<TranscriptChunk>()
    private var pending: TranscriptChunk? = null
    private var lastPartial: String = ""

    fun onPartial(text: String, elapsedMs: Long): TranscriptChunk? {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) {
            lastPartial = cleaned
            return null
        }
        val prefix = cleaned.commonPrefixWith(lastPartial)
        val addition = cleaned.substring(prefix.length)
        if (addition.isEmpty()) {
            lastPartial = cleaned
            pending = pending?.copy(endMs = elapsedMs)
            return pending
        }
        val additionText = addition.trimStart()
        if (additionText.isEmpty()) {
            lastPartial = cleaned
            pending = pending?.copy(endMs = elapsedMs)
            return pending
        }
        val previous = pending
        if (previous != null) {
            chunks.add(previous.copy(endMs = elapsedMs))
        }
        val chunk = TranscriptChunk(
            text = additionText,
            startMs = elapsedMs,
            endMs = null,
            speaker = null
        )
        pending = chunk
        lastPartial = cleaned
        return chunk
    }

    fun onFinal(text: String, elapsedMs: Long): List<TranscriptChunk> {
        onPartial(text, elapsedMs)
        val finalPending = pending
        if (finalPending != null) {
            chunks.add(finalPending.copy(endMs = elapsedMs))
            pending = null
        }
        return chunks.toList()
    }

    fun finalText(text: String): String {
        return text.trim().replace("\s+".toRegex(), " ")
    }
}
