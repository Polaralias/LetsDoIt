package com.polaralias.letsdoit.bulk

import org.junit.Assert.assertEquals
import org.junit.Test

class BulkLineParserTest {
    @Test
    fun `splitBulkInput ignores blanks and normalises new lines`() {
        val input = "First\r\n\n Second\n\nThird "
        val lines = splitBulkInput(input)
        assertEquals(3, lines.size)
        assertEquals("First", lines[0].content)
        assertEquals("Second", lines[1].content)
        assertEquals("Third", lines[2].content)
        assertEquals(3, lines[2].index)
    }

    @Test
    fun `parseLineTokens removes tokens from text`() {
        val tokens = parseLineTokens("Finish report #Work !high @Doing")
        assertEquals("Finish report", tokens.cleaned)
        assertEquals(ListToken(space = null, list = "Work"), tokens.listToken)
        assertEquals(0, tokens.priority)
        assertEquals("Doing", tokens.column)
    }
}
