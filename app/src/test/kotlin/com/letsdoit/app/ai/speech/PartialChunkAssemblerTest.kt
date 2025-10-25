package com.letsdoit.app.ai.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PartialChunkAssemblerTest {
    @Test
    fun emitsChunkForNewAddition() {
        val assembler = PartialChunkAssembler()
        val chunk = assembler.onPartial("Hello", 100)
        requireNotNull(chunk)
        assertEquals("Hello", chunk.text)
        assertEquals(100, chunk.startMs)
        assertNull(chunk.endMs)
    }

    @Test
    fun mergesSequentialPartials() {
        val assembler = PartialChunkAssembler()
        assembler.onPartial("Hello", 100)
        val firstUpdate = assembler.onPartial("Hello there", 300)
        assertEquals("there", firstUpdate?.text)
        val chunks = assembler.onFinal("Hello there", 500)
        assertEquals(2, chunks.size)
        assertEquals(500, chunks.last().endMs)
    }

    @Test
    fun finalTextNormalisesWhitespace() {
        val assembler = PartialChunkAssembler()
        val text = assembler.finalText(" Hello   world \n")
        assertEquals("Hello world", text)
    }
}
