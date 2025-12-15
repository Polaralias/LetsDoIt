package com.letsdoit.app.presentation.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.letsdoit.app.domain.model.InsightsData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val insights by viewModel.insights.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Insights") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (insights == null) {
                CircularProgressIndicator()
            } else {
                val data = insights!!

                // Overview Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Active",
                        count = data.totalActive,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primaryContainer
                    )
                    StatCard(
                        title = "Completed",
                        count = data.totalCompleted,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Tasks by Priority",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (data.tasksByPriority.isNotEmpty()) {
                    PriorityPieChart(data.tasksByPriority)
                } else {
                    Text("No tasks with priority set.")
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Tasks by Status",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (data.tasksByStatus.isNotEmpty()) {
                    StatusPieChart(data.tasksByStatus)
                } else {
                    Text("No tasks found.")
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun PriorityPieChart(data: Map<Int, Int>) {
    // Priorities: 1 (Urgent) -> Red, 2 (High) -> Orange, 3 (Normal) -> Blue, 4 (Low) -> Grey, 0 (None) -> LightGrey
    val colors = mapOf(
        1 to Color.Red,
        2 to Color(0xFFFF9800), // Orange
        3 to Color.Blue,
        4 to Color.Gray,
        0 to Color.LightGray
    )

    val total = data.values.sum()
    val angles = data.map { (priority, count) ->
        val sweepAngle = 360f * (count.toFloat() / total)
        Triple(priority, count, sweepAngle)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(150.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                angles.forEach { (priority, _, sweepAngle) ->
                    val color = colors[priority] ?: Color.LightGray
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            angles.sortedBy { it.first }.forEach { (priority, count, _) ->
                val label = when(priority) {
                    1 -> "Urgent"
                    2 -> "High"
                    3 -> "Normal"
                    4 -> "Low"
                    else -> "None"
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp), onDraw = {
                        drawCircle(color = colors[priority] ?: Color.LightGray)
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$label: $count")
                }
            }
        }
    }
}

@Composable
fun StatusPieChart(data: Map<String, Int>) {
    // Map status strings to colors
    val colors = listOf(
        Color(0xFF4CAF50), // Green
        Color(0xFF2196F3), // Blue
        Color(0xFFFFC107), // Amber
        Color(0xFF9C27B0), // Purple
        Color(0xFF00BCD4)  // Cyan
    )

    val total = data.values.sum()
    val entries = data.entries.toList()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(150.dp)) {
             Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                entries.forEachIndexed { index, entry ->
                    val sweepAngle = 360f * (entry.value.toFloat() / total)
                    val color = colors[index % colors.size]
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            entries.forEachIndexed { index, entry ->
                 Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp), onDraw = {
                        drawCircle(color = colors[index % colors.size])
                    })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${entry.key}: ${entry.value}")
                }
            }
        }
    }
}
