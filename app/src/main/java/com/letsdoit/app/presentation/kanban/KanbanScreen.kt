package com.letsdoit.app.presentation.kanban

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.letsdoit.app.domain.model.Task
import com.letsdoit.app.presentation.navigation.Screen
import kotlin.math.roundToInt

@Composable
fun KanbanScreen(
    navController: NavController,
    viewModel: KanbanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // State for drag and drop
    var draggingTask by remember { mutableStateOf<Task?>(null) }
    var draggingTaskPosition by remember { mutableStateOf(Offset.Zero) }
    var draggingTaskSize by remember { mutableStateOf(Offset.Zero) } // To center the drag

    // Parent box global position to adjust drag offset
    var parentPosition by remember { mutableStateOf(Offset.Zero) }

    // Map to store bounds of each column
    val columnBounds = remember { mutableStateMapOf<KanbanColumn, Rect>() }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                Text(
                    text = "Kanban Board",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .onGloballyPositioned { coordinates ->
                    parentPosition = coordinates.boundsInWindow().topLeft
                }
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                val columns = remember { KanbanColumn.values() }
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)
                ) {
                    items(columns) { column ->
                        KanbanColumnView(
                            column = column,
                            tasks = uiState.columns[column] ?: emptyList(),
                            onTaskClick = { taskId ->
                                navController.navigate(Screen.TaskDetails.createRoute(taskId))
                            },
                            onTaskDragStart = { task, offset, size ->
                                draggingTask = task
                                draggingTaskPosition = offset - parentPosition
                                draggingTaskSize = size
                            },
                            onTaskDrag = { dragAmount ->
                                draggingTaskPosition += dragAmount
                            },
                            onTaskDragEnd = {
                                // Check which column contains the drop position
                                // We use the center of the dragged item for better accuracy
                                val dropCenter = draggingTaskPosition + parentPosition + (draggingTaskSize / 2f)

                                val targetColumn = columnBounds.entries.firstOrNull { (_, rect) ->
                                    rect.contains(dropCenter)
                                }?.key

                                if (targetColumn != null && draggingTask != null) {
                                    viewModel.moveTask(draggingTask!!, targetColumn)
                                }
                                draggingTask = null
                            },
                            onGloballyPositioned = { rect ->
                                columnBounds[column] = rect
                            },
                        )
                    }
                }
            }

            // Overlay for dragged task
            draggingTask?.let { task ->
                KanbanTaskCard(
                    task = task,
                    onClick = {},
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                draggingTaskPosition.x.roundToInt(),
                                draggingTaskPosition.y.roundToInt()
                            )
                        }
                        .width(280.dp) // Fixed width similar to column items
                        .zIndex(1f)
                        .graphicsLayer {
                            alpha = 0.9f
                            scaleX = 1.05f
                            scaleY = 1.05f
                        }
                )
            }
        }
    }
}

@Composable
fun KanbanColumnView(
    column: KanbanColumn,
    tasks: List<Task>,
    onTaskClick: (String) -> Unit,
    onTaskDragStart: (Task, Offset, Offset) -> Unit,
    onTaskDrag: (Offset) -> Unit,
    onTaskDragEnd: () -> Unit,
    onGloballyPositioned: (Rect) -> Unit
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium)
            .padding(8.dp)
            .onGloballyPositioned { layoutCoordinates ->
                onGloballyPositioned(layoutCoordinates.boundsInWindow())
            }
    ) {
        Text(
            text = column.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(8.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            items(tasks) { task ->
                DraggableTaskItem(
                    task = task,
                    onClick = { onTaskClick(task.id) },
                    onDragStart = onTaskDragStart,
                    onDrag = onTaskDrag,
                    onDragEnd = onTaskDragEnd
                )
            }
        }
    }
}

@Composable
fun DraggableTaskItem(
    task: Task,
    onClick: () -> Unit,
    onDragStart: (Task, Offset, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    var itemSize by remember { mutableStateOf(Offset.Zero) }

    KanbanTaskCard(
        task = task,
        onClick = onClick,
        modifier = Modifier
            .onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                itemPosition = bounds.topLeft
                itemSize = Offset(bounds.width, bounds.height)
            }
            .pointerInput(task) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
                        onDragStart(task, itemPosition, itemSize)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragEnd() }
                )
            }
    )
}

@Composable
fun KanbanTaskCard(
    task: Task,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            if (!task.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
