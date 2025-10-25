package com.letsdoit.app.baseline

import android.content.Context
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.StartupMode
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.letsdoit.app.data.db.AppDatabase
import com.letsdoit.app.data.db.entities.ListEntity
import com.letsdoit.app.data.db.entities.SpaceEntity
import com.letsdoit.app.data.db.entities.TaskEntity
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScrollJankBenchmark {
    @get:Rule
    val macroRule = MacrobenchmarkRule()

    @Test
    fun scrollLongTaskList() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        seedTasks(context)
        macroRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            startupMode = StartupMode.COLD,
            iterations = 3
        ) {
            pressHome()
            startActivityAndWait()
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            device.wait(Until.hasObject(By.text("Add")), 5_000)
            val list = UiScrollable(UiSelector().scrollable(true))
            list.setAsVerticalList()
            list.scrollToEnd(10)
            list.scrollToBeginning(10)
        }
    }

    private fun seedTasks(context: Context) {
        val database = Room.databaseBuilder(context, AppDatabase::class.java, "letsdoit.db")
            .allowMainThreadQueries()
            .build()
        runBlocking {
            database.clearAllTables()
            database.spaceDao().upsert(SpaceEntity(name = "Benchmark Space"))
            val spaceId = requireNotNull(database.spaceDao().findByName("Benchmark Space")).id
            database.listDao().upsert(ListEntity(spaceId = spaceId, name = "Benchmark List"))
            val listId = requireNotNull(database.listDao().findByName("Benchmark List")).id
            repeat(2_000) { index ->
                val now = Instant.now()
                database.taskDao().upsert(
                    TaskEntity(
                        listId = listId,
                        title = "Seeded Task $index",
                        createdAt = now,
                        updatedAt = now,
                        orderInList = index
                    )
                )
            }
        }
        database.close()
    }

    companion object {
        private const val PACKAGE_NAME = "com.letsdoit.app"
    }
}
