package com.letsdoit.app.widgets

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.GlanceId
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import com.letsdoit.app.MainActivity
import com.letsdoit.app.R
import com.letsdoit.app.data.model.Task
import com.letsdoit.app.data.task.TaskRepository
import com.letsdoit.app.navigation.AppIntentExtras
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val QUICK_ADD_MAX_CHARS = 500
private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TodayWidgetEntryPoint {
    fun taskRepository(): TaskRepository
}

object TodayTasksWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(DpSize(120.dp, 120.dp), DpSize(260.dp, 200.dp))
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = EntryPointAccessors.fromApplication(context, TodayWidgetEntryPoint::class.java).taskRepository()
        val tasks = withContext(Dispatchers.IO) { repository.listTodayTasks() }
        val quickAddIntent = buildQuickAddIntent(context)
        provideContent {
            TodayWidgetContent(tasks = tasks, quickAddAction = actionStartActivity(quickAddIntent))
        }
    }
}

class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTasksWidget
}

private fun buildQuickAddIntent(context: Context): Intent {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val seed = clipboard.primaryClip?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.text
        ?.takeIf { !it.isNullOrBlank() }
        ?.take(QUICK_ADD_MAX_CHARS)
    return Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(AppIntentExtras.OPEN_BULK_FROM_WIDGET, true)
        seed?.let { putExtra(AppIntentExtras.CLIPBOARD_SEED, it.toString()) }
    }
}

private fun toggleIntent(context: Context, taskId: Long): Action {
    val intent = Intent(context, TodayWidgetToggleReceiver::class.java).apply {
        action = TodayWidgetToggleReceiver.ACTION_TOGGLE
        putExtra(EXTRA_WIDGET_TASK_ID, taskId)
    }
    return actionSendBroadcast(intent)
}

@Composable
private fun TodayWidgetContent(tasks: List<Task>, quickAddAction: Action) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        Text(
            text = context.getString(R.string.widget_title_today),
            style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text(
                text = context.getString(R.string.widget_empty_today),
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.GRAY))
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                items(tasks.take(8)) { task ->
                    TaskItem(task)
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(12.dp))
        QuickAddField(action = quickAddAction)
    }
}

@Composable
private fun TaskItem(task: Task) {
    val context = LocalContext.current
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        CheckBox(
            checked = false,
            onCheckedChange = toggleIntent(context, task.id),
            text = task.title,
            style = TextStyle(fontSize = 14.sp)
        )
        task.dueAt?.let { due ->
            Text(
                text = timeFormatter.format(due),
                style = TextStyle(fontSize = 12.sp, color = ColorProvider(Color.DKGRAY))
            )
        }
    }
}

@Composable
private fun QuickAddField(action: Action) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable(action)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = context.getString(R.string.widget_quick_add_hint),
            style = TextStyle(fontSize = 13.sp, color = ColorProvider(Color.DKGRAY))
        )
    }
}

class TodayWidgetToggleReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) return
        val taskId = intent.getLongExtra(EXTRA_WIDGET_TASK_ID, 0L)
        if (taskId == 0L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = EntryPointAccessors.fromApplication(context, TodayWidgetEntryPoint::class.java).taskRepository()
                repository.updateCompletion(taskId, true)
                TodayTasksWidget.updateAll(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.letsdoit.app.widgets.TOGGLE"
    }
}
