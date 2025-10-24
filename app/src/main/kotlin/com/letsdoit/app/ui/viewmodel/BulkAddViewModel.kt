package com.letsdoit.app.ui.viewmodel

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.letsdoit.app.bulk.BulkLineTokens
import com.letsdoit.app.bulk.ListToken
import com.letsdoit.app.bulk.parseBulkLines
import com.letsdoit.app.bulk.ColumnResolution
import com.letsdoit.app.bulk.ListResolution
import com.letsdoit.app.bulk.MergedTaskResult
import com.letsdoit.app.bulk.BulkErrorTitleRequired
import com.letsdoit.app.bulk.BulkErrorUnknownBucket
import com.letsdoit.app.bulk.BulkErrorUnknownList
import com.letsdoit.app.bulk.BulkWarningDuePast
import com.letsdoit.app.bulk.mergeParsedTask
import com.letsdoit.app.bulk.resolveColumnToken
import com.letsdoit.app.bulk.resolveListToken
import com.letsdoit.app.bulk.timelineDefaults
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.prefs.BulkPreferences
import com.letsdoit.app.data.prefs.PreferencesRepository
import com.letsdoit.app.data.prefs.ViewPreferences
import com.letsdoit.app.data.task.BulkCreateItem
import com.letsdoit.app.data.task.LineIssue
import com.letsdoit.app.data.task.TaskRepository
import com.letsdoit.app.integrations.calendar.CalendarBridge
import com.letsdoit.app.nlp.NaturalLanguageParser
import com.letsdoit.app.nlp.ParsedTask
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BulkAddViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val parser: NaturalLanguageParser,
    private val preferencesRepository: PreferencesRepository,
    private val calendarBridge: CalendarBridge,
    private val clock: Clock
) : ViewModel() {
    private val zoneId: ZoneId = clock.zone

    private val textField = MutableStateFlow(TextFieldValue(""))
    private val mode = MutableStateFlow(BulkMode.Text)
    private val csvRows = MutableStateFlow<List<CsvRow>>(emptyList())
    private val lastCreatedIds = MutableStateFlow<List<Long>>(emptyList())

    private val eventsFlow = MutableSharedFlow<BulkEvent>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val context: StateFlow<BulkContext> = combine(
        taskRepository.observeLists(),
        taskRepository.observeSpaces(),
        preferencesRepository.viewPreferences,
        preferencesRepository.bulkPreferences
    ) { lists, spaces, viewPrefs, bulkPrefs ->
        BulkContext(
            lists = lists,
            spaces = spaces,
            viewPreferences = viewPrefs,
            bulkPreferences = bulkPrefs
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BulkContext())

    private val parsedState = combine(textField, mode, csvRows, context) { value, currentMode, rows, ctx ->
        when (currentMode) {
            BulkMode.Text -> parseText(value.text, ctx)
            BulkMode.Csv -> parseCsv(rows, ctx)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ParseResult())

    val uiState: StateFlow<BulkUiState> = combine(textField, mode, parsedState, context) { value, currentMode, parsed, ctx ->
        BulkUiState(
            textFieldValue = value,
            mode = currentMode,
            lines = parsed.previews,
            issues = parsed.issues,
            validCount = parsed.validCount,
            rememberTokens = ctx.bulkPreferences.rememberLastTokens,
            defaultListName = ctx.lists.firstOrNull { it.id == ctx.bulkPreferences.defaultListId }?.name,
            warnings = parsed.warnings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BulkUiState())

    val events = eventsFlow.asSharedFlow()

    fun onTextChanged(value: TextFieldValue) {
        textField.value = value
        mode.value = BulkMode.Text
    }

    fun insertToken(token: String) {
        val current = textField.value
        val selection = current.selection
        val index = selection.start.coerceAtLeast(0)
        val newText = StringBuilder(current.text).insert(index, token).toString()
        val newCursor = (index + token.length).coerceAtMost(newText.length)
        textField.value = current.copy(text = newText, selection = androidx.compose.ui.text.TextRange(newCursor))
    }

    fun clear() {
        when (mode.value) {
            BulkMode.Text -> textField.value = TextFieldValue("")
            BulkMode.Csv -> {
                csvRows.value = emptyList()
                mode.value = BulkMode.Text
            }
        }
    }

    fun toggleRememberTokens(value: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateRememberBulkTokens(value)
        }
    }

    fun importCsv(stream: InputStream) {
        val rows = parseCsvStream(stream)
        csvRows.value = rows
        mode.value = BulkMode.Csv
    }

    fun createTasks() {
        val parsed = parsedState.value
        if (parsed.validCount == 0) return
        viewModelScope.launch {
            val ctx = context.value
            val defaultListId = ctx.bulkPreferences.defaultListId ?: taskRepository.ensureDefaultList()
                val items = parsed.validLines.map { line ->
                    val listId = line.listId ?: defaultListId
                    BulkCreateItem(
                        listId = listId,
                        title = line.base.title,
                        notes = line.base.notes,
                        dueAt = line.base.dueAt,
                        repeatRule = line.base.repeatRule,
                        remindOffsetMinutes = line.base.remindOffsetMinutes,
                        priority = line.base.priority,
                        column = line.base.column,
                        startAt = line.base.startAt,
                        durationMinutes = line.base.durationMinutes
                    )
            }
            if (items.isEmpty()) return@launch
            val result = taskRepository.bulkCreate(items)
            if (result.createdCount > 0) {
                recordCalendar(items, result.createdIds)
                eventsFlow.tryEmit(BulkEvent.Created(result.createdCount))
                lastCreatedIds.value = result.createdIds
                if (mode.value == BulkMode.Text) {
                    textField.value = TextFieldValue("")
                } else {
                    csvRows.value = emptyList()
                }
            }
        }
    }

    fun undoLast() {
        val ids = lastCreatedIds.value
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { id ->
                taskRepository.deleteTask(id)
            }
            lastCreatedIds.value = emptyList()
            eventsFlow.tryEmit(BulkEvent.UndoComplete)
        }
    }

    private fun parseText(input: String, ctx: BulkContext): ParseResult {
        if (input.isBlank()) return ParseResult()
        val lines = parseBulkLines(input)
        return parseLines(lines.map { ParsedSource.Text(it) }, ctx)
    }

    private fun parseCsv(rows: List<CsvRow>, ctx: BulkContext): ParseResult {
        if (rows.isEmpty()) return ParseResult()
        return parseLines(rows.map { ParsedSource.Csv(it) }, ctx)
    }

    private fun parseLines(sources: List<ParsedSource>, ctx: BulkContext): ParseResult {
        if (sources.isEmpty()) return ParseResult()
        val previews = mutableListOf<BulkPreviewLine>()
        val issues = mutableListOf<LineIssue>()
        val warnings = mutableListOf<BulkWarning>()
        val validLines = mutableListOf<ParsedValidLine>()
        val now = Instant.now(clock)
        sources.forEach { source ->
            when (source) {
                is ParsedSource.Text -> handleTextLine(source.tokens, ctx, previews, issues, warnings, validLines, now)
                is ParsedSource.Csv -> handleCsvRow(source.row, ctx, previews, issues, warnings, validLines, now)
            }
        }
        return ParseResult(
            previews = previews,
            issues = issues,
            validCount = validLines.size,
            warnings = warnings,
            validLines = validLines
        )
    }

    private fun handleTextLine(
        tokens: BulkLineTokens,
        ctx: BulkContext,
        previews: MutableList<BulkPreviewLine>,
        issues: MutableList<LineIssue>,
        warnings: MutableList<BulkWarning>,
        validLines: MutableList<ParsedValidLine>,
        now: Instant
    ) {
        if (tokens.cleaned.isBlank()) {
            val message = BulkErrorTitleRequired
            issues += LineIssue(tokens.index, message)
            previews += BulkPreviewLine(lineIndex = tokens.index, title = tokens.original, errorMessage = message)
            return
        }
        val parsed = parser.parse(tokens.cleaned)
        val merged = mergeParsedTask(
            parsedTitle = parsed.title,
            dueAt = parsed.dueAt,
            repeatExpression = parsed.repeatRule,
            remindOffsetMinutes = parsed.remindOffsetMinutes,
            tokenPriority = tokens.priority,
            preferences = ctx.bulkPreferences,
            viewPreferences = ctx.viewPreferences,
            zoneId = zoneId
        )
        val resolution = resolveListToken(tokens.listToken, ctx.lists, ctx.spaces, ctx.bulkPreferences.defaultListId)
        if (resolution.error != null) {
            issues += LineIssue(tokens.index, resolution.error)
        }
        val columnResolution = resolveColumnToken(tokens.column, ctx.viewPreferences.boardColumns)
        val errors = listOfNotNull(resolution.error, columnResolution.error)
        val warning = merged.dueAt?.let { due ->
            if (due.isBefore(now.minus(1, ChronoUnit.DAYS))) {
                BulkWarning(tokens.index, BulkWarningDuePast)
            } else null
        }
        warning?.let { warnings += it }
        val preview = BulkPreviewLine(
            lineIndex = tokens.index,
            title = merged.title,
            dueAt = merged.dueAt,
            listName = resolution.list?.name,
            spaceName = resolution.spaceName,
            priority = merged.priority,
            column = columnResolution.value,
            bucketLabel = columnResolution.value,
            warningMessage = warning?.message,
            errorMessage = errors.joinToString("; ").ifBlank { null }
        )
        previews += preview
        if (errors.isEmpty()) {
            validLines += ParsedValidLine(
                lineIndex = tokens.index,
                listId = resolution.list?.id,
                base = ParsedBase(
                    title = merged.title,
                    notes = null,
                    dueAt = merged.dueAt,
                    repeatRule = merged.repeatRule,
                    remindOffsetMinutes = merged.remindOffsetMinutes,
                    priority = merged.priority,
                    column = columnResolution.value,
                    startAt = merged.startAt,
                    durationMinutes = merged.durationMinutes
                )
            )
        }
    }

    private fun handleCsvRow(
        row: CsvRow,
        ctx: BulkContext,
        previews: MutableList<BulkPreviewLine>,
        issues: MutableList<LineIssue>,
        warnings: MutableList<BulkWarning>,
        validLines: MutableList<ParsedValidLine>,
        now: Instant
    ) {
        val title = row.title?.take(200)?.trim().orEmpty()
        if (title.isBlank()) {
            val message = BulkErrorTitleRequired
            issues += LineIssue(row.index, message)
            previews += BulkPreviewLine(lineIndex = row.index, title = row.title ?: "", errorMessage = message)
            return
        }
        val token = row.listToken
        val resolution = resolveListToken(token, ctx.lists, ctx.spaces, ctx.bulkPreferences.defaultListId)
        val columnResolution = resolveColumnToken(row.bucket, ctx.viewPreferences.boardColumns)
        val errors = mutableListOf<String>()
        resolution.error?.let { errors += it }
        columnResolution.error?.let { errors += it }
        val warning = row.dueAt?.let { due ->
            if (due.isBefore(now.minus(1, ChronoUnit.DAYS))) {
                BulkWarning(row.index, BulkWarningDuePast)
            } else null
        }
        warning?.let { warnings += it }
        previews += BulkPreviewLine(
            lineIndex = row.index,
            title = title,
            dueAt = row.dueAt,
            listName = resolution.list?.name,
            spaceName = resolution.spaceName,
            priority = row.priority ?: 2,
            column = columnResolution.value,
            bucketLabel = columnResolution.value,
            warningMessage = warning?.message,
            errorMessage = errors.joinToString("; ").ifBlank { null }
        )
        if (errors.isEmpty()) {
            val mergedPriority = row.priority ?: 2
            val startAt = timelineDefaults(row.dueAt, ctx.bulkPreferences, ctx.viewPreferences, zoneId)
            validLines += ParsedValidLine(
                lineIndex = row.index,
                listId = resolution.list?.id,
                base = ParsedBase(
                    title = title,
                    notes = row.notes,
                    dueAt = row.dueAt,
                    repeatRule = null,
                    remindOffsetMinutes = null,
                    priority = mergedPriority,
                    column = columnResolution.value,
                    startAt = startAt.first,
                    durationMinutes = startAt.second
                )
            )
        }
    }

    private fun recordCalendar(items: List<BulkCreateItem>, ids: List<Long>) {
        val now = Instant.now(clock)
        items.zip(ids).forEach { (item, _) ->
            item.dueAt?.let { due ->
                if (due.isAfter(now)) {
                    calendarBridge.insertEvent(item.title, due)
                }
            }
        }
    }

    private fun parseCsvStream(stream: InputStream): List<CsvRow> {
        BufferedReader(InputStreamReader(stream)).use { reader ->
            val headerLine = reader.lineSequence().firstOrNull { it.isNotBlank() } ?: return emptyList()
            val header = parseCsvLine(headerLine).map { it.trim().lowercase() }
            if (header.isEmpty()) return emptyList()
            val rows = mutableListOf<CsvRow>()
            var index = 1
            reader.lineSequence().forEach { line ->
                if (line.isBlank()) return@forEach
                val values = parseCsvLine(line)
                val map = header.zip(values.map { it.trim() }).toMap()
                val dueAt = map["dueat"]?.takeIf { it.isNotBlank() }?.let { value ->
                    runCatching { Instant.parse(value) }.getOrNull()
                }
                val priority = map["priority"]?.toIntOrNull()?.coerceIn(0, 3)
                val bucket = map["bucket"]?.takeIf { it.isNotBlank() }
                val listValue = map["listname"]?.takeIf { it.isNotBlank() }
                rows += CsvRow(
                    index = index,
                    title = map["title"],
                    notes = map["notes"],
                    dueAt = dueAt,
                    priority = priority,
                    bucket = bucket,
                    listToken = listValue?.let { parseListNameToken(it) }
                )
                index += 1
            }
            return rows
        }
    }

    private fun parseListNameToken(value: String): ListToken {
        val parts = value.split('/', limit = 2)
        return if (parts.size == 2) {
            ListToken(parts[0], parts[1])
        } else {
            ListToken(null, value)
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val values = mutableListOf<String>()
        val builder = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val char = line[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        builder.append('"')
                        i += 1
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == ',' && !inQuotes -> {
                    values += builder.toString()
                    builder.clear()
                }
                else -> builder.append(char)
            }
            i += 1
        }
        values += builder.toString()
        return values
    }
}

private data class BulkContext(
    val lists: List<ListEntity> = emptyList(),
    val spaces: List<SpaceEntity> = emptyList(),
    val viewPreferences: ViewPreferences = ViewPreferences.Default,
    val bulkPreferences: BulkPreferences = BulkPreferences.Default
)

enum class BulkMode { Text, Csv }

data class BulkUiState(
    val textFieldValue: TextFieldValue = TextFieldValue(""),
    val mode: BulkMode = BulkMode.Text,
    val lines: List<BulkPreviewLine> = emptyList(),
    val issues: List<LineIssue> = emptyList(),
    val validCount: Int = 0,
    val rememberTokens: Boolean = false,
    val defaultListName: String? = null,
    val warnings: List<BulkWarning> = emptyList()
)

sealed class BulkEvent {
    data class Created(val count: Int) : BulkEvent()
    data object UndoComplete : BulkEvent()
}

data class BulkPreviewLine(
    val lineIndex: Int,
    val title: String,
    val dueAt: Instant? = null,
    val listName: String? = null,
    val spaceName: String? = null,
    val priority: Int? = null,
    val column: String? = null,
    val bucketLabel: String? = null,
    val warningMessage: String? = null,
    val errorMessage: String? = null
)

data class BulkWarning(val lineIndex: Int, val message: String)

data class ParsedValidLine(
    val lineIndex: Int,
    val listId: Long?,
    val base: ParsedBase
)

data class ParsedBase(
    val title: String,
    val notes: String?,
    val dueAt: Instant?,
    val repeatRule: String?,
    val remindOffsetMinutes: Int?,
    val priority: Int,
    val column: String?,
    val startAt: Instant?,
    val durationMinutes: Int?
)

data class ParseResult(
    val previews: List<BulkPreviewLine> = emptyList(),
    val issues: List<LineIssue> = emptyList(),
    val validCount: Int = 0,
    val warnings: List<BulkWarning> = emptyList(),
    val validLines: List<ParsedValidLine> = emptyList()
)

private sealed class ParsedSource {
    data class Text(val tokens: BulkLineTokens) : ParsedSource()
    data class Csv(val row: CsvRow) : ParsedSource()
}

data class CsvRow(
    val index: Int,
    val title: String?,
    val notes: String?,
    val dueAt: Instant?,
    val priority: Int?,
    val bucket: String?,
    val listToken: ListToken?
)
