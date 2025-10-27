package com.polaralias.letsdoit.bulk

import java.util.Locale

private val whitespaceRegex = Regex("\\s+")
private val listRegex = Regex("#([^\\s]+)")
private val priorityRegex = Regex("!(high|med|low)", RegexOption.IGNORE_CASE)
private val columnRegex = Regex("@([^\\s]+)")

fun splitBulkInput(input: String): List<IndexedLine> {
    if (input.isBlank()) return emptyList()
    val normalised = input.replace("\r\n", "\n").replace('\r', '\n')
    val rawLines = normalised.split('\n')
    val result = mutableListOf<IndexedLine>()
    var index = 1
    rawLines.forEach { raw ->
        val trimmed = raw.trim()
        if (trimmed.isNotEmpty()) {
            result += IndexedLine(index = index, content = trimmed)
            index += 1
        }
    }
    return result
}

data class IndexedLine(val index: Int, val content: String)

data class ListToken(val space: String?, val list: String)

data class ParsedTokens(
    val cleaned: String,
    val listToken: ListToken?,
    val priority: Int?,
    val column: String?
)

data class BulkLineTokens(
    val index: Int,
    val original: String,
    val cleaned: String,
    val listToken: ListToken?,
    val priority: Int?,
    val column: String?
)

fun parseLineTokens(line: String): ParsedTokens {
    var working = line
    var listToken: ListToken? = null
    var priority: Int? = null
    var column: String? = null

    listRegex.findAll(working).forEach { match ->
        val value = match.groupValues[1]
        val parts = value.split('/', limit = 2)
        listToken = if (parts.size == 2) {
            ListToken(space = parts[0], list = parts[1])
        } else {
            ListToken(space = null, list = value)
        }
    }
    working = listRegex.replace(working) { " " }

    priorityRegex.findAll(working).forEach { match ->
        val value = match.groupValues[1].lowercase(Locale.ROOT)
        priority = when (value) {
            "high" -> 0
            "med" -> 1
            "low" -> 3
            else -> 2
        }
    }
    working = priorityRegex.replace(working) { " " }

    columnRegex.findAll(working).forEach { match ->
        column = match.groupValues[1]
    }
    working = columnRegex.replace(working) { " " }

    val cleaned = working.replace(whitespaceRegex, " ").trim()
    return ParsedTokens(
        cleaned = cleaned,
        listToken = listToken,
        priority = priority,
        column = column
    )
}

fun parseBulkLines(input: String): List<BulkLineTokens> {
    return splitBulkInput(input).map { indexed ->
        val tokens = parseLineTokens(indexed.content)
        BulkLineTokens(
            index = indexed.index,
            original = indexed.content,
            cleaned = tokens.cleaned,
            listToken = tokens.listToken,
            priority = tokens.priority,
            column = tokens.column
        )
    }
}
