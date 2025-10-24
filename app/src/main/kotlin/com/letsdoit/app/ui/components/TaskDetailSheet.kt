package com.letsdoit.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.letsdoit.app.R
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.recurrence.Frequency
import com.letsdoit.app.recurrence.RecurrenceRule
import com.letsdoit.app.recurrence.WeekdaySpecifier
import com.letsdoit.app.ui.util.computeRecurrenceDisplay
import com.letsdoit.app.ui.util.recurrencePresets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private data class ReminderChoice(val id: String, val label: String, val minutes: Int?)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (String?, Int?) -> Unit
) {
    val zoneId = ZoneId.systemDefault()
    val presets = remember(task.id, task.dueAt) { recurrencePresets(zoneId, task.dueAt) }
    var currentRule by remember(task.id) { mutableStateOf(RecurrenceRule.fromRRule(task.repeatRule)) }
    var customFrequency by remember(task.id) { mutableStateOf((currentRule?.frequency ?: Frequency.WEEKLY).name) }
    var customInterval by remember(task.id) { mutableStateOf((currentRule?.interval ?: 1).toString()) }
    var customByDay by remember(task.id) { mutableStateOf(currentRule?.weekDays?.joinToString(",") { specToToken(it) } ?: "") }
    var customByMonthDay by remember(task.id) { mutableStateOf(currentRule?.monthDays?.joinToString(",") ?: "") }
    var customByMonth by remember(task.id) { mutableStateOf(currentRule?.months?.joinToString(",") ?: "") }
    var customCount by remember(task.id) { mutableStateOf(currentRule?.count?.toString() ?: "") }
    var customUntil by remember(task.id) { mutableStateOf(currentRule?.until?.let { formatUntil(it, zoneId) } ?: "") }
    var selectedPresetId by remember(task.id) {
        mutableStateOf(
            currentRule?.let { rule -> presets.firstOrNull { it.matches(rule, task.dueAt) }?.id } ?: currentRule?.let { "custom" }
        )
    }

    LaunchedEffect(task.id) {
        val initialRule = RecurrenceRule.fromRRule(task.repeatRule)
        currentRule = initialRule
        customFrequency = (initialRule?.frequency ?: Frequency.WEEKLY).name
        customInterval = (initialRule?.interval ?: 1).toString()
        customByDay = initialRule?.weekDays?.joinToString(",") { specToToken(it) } ?: ""
        customByMonthDay = initialRule?.monthDays?.joinToString(",") ?: ""
        customByMonth = initialRule?.months?.joinToString(",") ?: ""
        customCount = initialRule?.count?.toString() ?: ""
        customUntil = initialRule?.until?.let { formatUntil(it, zoneId) } ?: ""
        selectedPresetId = initialRule?.let { rule -> presets.firstOrNull { it.matches(rule, task.dueAt) }?.id } ?: initialRule?.let { "custom" }
    }

    val reminderOptions = listOf(
        ReminderChoice("none", stringResource(id = R.string.reminder_none), null),
        ReminderChoice("at_time", stringResource(id = R.string.reminder_at_time), 0),
        ReminderChoice("5", stringResource(id = R.string.reminder_minutes_before, 5), 5),
        ReminderChoice("10", stringResource(id = R.string.reminder_minutes_before, 10), 10),
        ReminderChoice("30", stringResource(id = R.string.reminder_minutes_before, 30), 30),
        ReminderChoice("60", stringResource(id = R.string.reminder_minutes_before, 60), 60)
    )
    val standardMinutes = reminderOptions.mapNotNull { it.minutes }.toSet()
    var selectedReminderId by remember(task.id) {
        mutableStateOf(
            when (task.remindOffsetMinutes) {
                null -> "none"
                0 -> "at_time"
                in standardMinutes -> task.remindOffsetMinutes.toString()
                else -> "custom"
            }
        )
    }
    var customReminderText by remember(task.id) {
        mutableStateOf(
            task.remindOffsetMinutes?.takeIf { it !in standardMinutes && it != 0 }?.toString() ?: ""
        )
    }
    val customReminderValue = customReminderText.toIntOrNull()
    val customReminderError = selectedReminderId == "custom" && (customReminderValue == null || customReminderValue < 0)

    val previewDisplay = remember(currentRule, task.dueAt) {
        currentRule?.toRRule()?.let { value -> computeRecurrenceDisplay(value, task.dueAt, zoneId) }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun applyCustomRule(): RecurrenceRule {
        selectedPresetId = "custom"
        val frequency = runCatching { Frequency.valueOf(customFrequency.uppercase(Locale.UK)) }.getOrDefault(Frequency.WEEKLY)
        val interval = customInterval.toIntOrNull()?.takeIf { it > 0 } ?: 1
        val byDay = customByDay.split(',').mapNotNull { parseWeekdayToken(it.trim()) }
        val byMonthDayValues = customByMonthDay.split(',').mapNotNull { it.trim().toIntOrNull()?.takeIf { value -> value in 1..31 } }
        val byMonthValues = customByMonth.split(',').mapNotNull { it.trim().toIntOrNull()?.takeIf { value -> value in 1..12 } }
        val countValue = customCount.toIntOrNull()?.takeIf { it > 0 }
        val untilValue = parseUntil(customUntil.trim(), zoneId)
        return RecurrenceRule(
            frequency = frequency,
            interval = interval,
            weekDays = byDay,
            monthDays = byMonthDayValues,
            months = byMonthValues,
            count = countValue,
            until = untilValue
        )
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(text = task.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.label_recurrence), style = MaterialTheme.typography.titleMedium)
                if (previewDisplay != null) {
                    Text(text = previewDisplay.summary, style = MaterialTheme.typography.bodyMedium)
                    Text(text = stringResource(id = R.string.label_next_due, formatDisplay(previewDisplay.nextDue, zoneId)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(text = stringResource(id = R.string.label_no_recurrence), style = MaterialTheme.typography.bodyMedium)
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    val selected = selectedPresetId == preset.id
                    AssistChip(
                        onClick = {
                            val rule = preset.build(task.dueAt)
                            currentRule = rule
                            selectedPresetId = preset.id
                            customFrequency = rule.frequency.name
                            customInterval = rule.interval.toString()
                            customByDay = rule.weekDays.joinToString(",") { specToToken(it) }
                            customByMonthDay = rule.monthDays.joinToString(",")
                            customByMonth = rule.months.joinToString(",")
                            customCount = rule.count?.toString() ?: ""
                            customUntil = rule.until?.let { formatUntil(it, zoneId) } ?: ""
                        },
                        label = { Text(text = preset.title) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
                AssistChip(
                    onClick = {
                        selectedPresetId = "custom"
                        val rule = applyCustomRule()
                        currentRule = rule
                    },
                    label = { Text(text = stringResource(id = R.string.preset_custom)) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (selectedPresetId == "custom") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            if (selectedPresetId == "custom") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(id = R.string.label_custom_frequency), style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Frequency.entries.forEach { frequency ->
                            val selected = customFrequency == frequency.name
                            AssistChip(
                                onClick = {
                                    customFrequency = frequency.name
                                    currentRule = applyCustomRule()
                                },
                                label = { Text(text = frequency.name.lowercase(Locale.UK).replaceFirstChar { it.uppercase() }) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customInterval,
                        onValueChange = {
                            customInterval = it.filter { char -> char.isDigit() }
                            currentRule = applyCustomRule()
                        },
                        label = { Text(text = stringResource(id = R.string.label_custom_interval)) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customByDay,
                        onValueChange = {
                            customByDay = it
                            currentRule = applyCustomRule()
                        },
                        label = { Text(text = stringResource(id = R.string.label_custom_byday)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customByMonthDay,
                        onValueChange = {
                            customByMonthDay = it
                            currentRule = applyCustomRule()
                        },
                        label = { Text(text = stringResource(id = R.string.label_custom_bymonthday)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customByMonth,
                        onValueChange = {
                            customByMonth = it
                            currentRule = applyCustomRule()
                        },
                        label = { Text(text = stringResource(id = R.string.label_custom_bymonth)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customCount,
                        onValueChange = {
                            customCount = it.filter { char -> char.isDigit() }
                            currentRule = applyCustomRule()
                        },
                        label = { Text(text = stringResource(id = R.string.label_custom_count)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customUntil,
                        onValueChange = {
                            customUntil = it
                            currentRule = applyCustomRule()
                        },
                        label = { Text(text = stringResource(id = R.string.label_custom_until)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.label_reminders), style = MaterialTheme.typography.titleMedium)
                reminderOptions.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RadioButton(
                            selected = selectedReminderId == option.id,
                            onClick = {
                                selectedReminderId = option.id
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Text(text = option.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButton(
                        selected = selectedReminderId == "custom",
                        onClick = { selectedReminderId = "custom" },
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                    )
                    OutlinedTextField(
                        value = customReminderText,
                        onValueChange = {
                            customReminderText = it.filter { char -> char.isDigit() }
                            selectedReminderId = "custom"
                        },
                        label = { Text(text = stringResource(id = R.string.reminder_custom)) },
                        supportingText = { Text(text = stringResource(id = R.string.hint_minutes)) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (customReminderError) {
                    Text(text = stringResource(id = R.string.message_invalid_custom_reminder), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    currentRule = null
                    selectedPresetId = null
                }) {
                    Text(text = stringResource(id = R.string.action_clear_recurrence))
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.action_close))
                }
                Button(
                    onClick = {
                        val reminderValue = when (selectedReminderId) {
                            "none" -> null
                            "at_time" -> 0
                            "custom" -> customReminderValue
                            else -> selectedReminderId.toIntOrNull()
                        }
                        if (!customReminderError) {
                            onSave(currentRule?.toRRule(), reminderValue)
                            onDismiss()
                        }
                    },
                    enabled = !customReminderError
                ) {
                    Text(text = stringResource(id = R.string.action_save))
                }
            }
        }
    }
}

private fun specToToken(spec: WeekdaySpecifier): String {
    val day = spec.dayOfWeek.name.take(2)
    val ordinal = spec.ordinal?.toString() ?: ""
    return ordinal + day
}

private fun parseWeekdayToken(token: String): WeekdaySpecifier? {
    if (token.isBlank()) return null
    val trimmed = token.uppercase(Locale.UK)
    val dayPart = trimmed.takeLast(2)
    val ordinalPart = trimmed.dropLast(2)
    val day = when (dayPart) {
        "MO" -> java.time.DayOfWeek.MONDAY
        "TU" -> java.time.DayOfWeek.TUESDAY
        "WE" -> java.time.DayOfWeek.WEDNESDAY
        "TH" -> java.time.DayOfWeek.THURSDAY
        "FR" -> java.time.DayOfWeek.FRIDAY
        "SA" -> java.time.DayOfWeek.SATURDAY
        "SU" -> java.time.DayOfWeek.SUNDAY
        else -> return null
    }
    val ordinal = ordinalPart.takeIf { it.isNotBlank() }?.toIntOrNull()
    return WeekdaySpecifier(ordinal = ordinal, dayOfWeek = day)
}

private fun parseUntil(value: String, zoneId: ZoneId): Instant? {
    if (value.isBlank()) return null
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withLocale(Locale.UK)
        val dateTime = LocalDateTime.parse(value, formatter)
        dateTime.atZone(zoneId).toInstant()
    } catch (exception: DateTimeParseException) {
        null
    }
}

private fun formatUntil(value: Instant, zoneId: ZoneId): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zoneId)
    return formatter.format(value)
}

private fun formatDisplay(instant: Instant, zoneId: ZoneId): String {
    val formatter = DateTimeFormatter.ofPattern("EEE dd MMM HH:mm").withZone(zoneId)
    return formatter.format(instant)
}
