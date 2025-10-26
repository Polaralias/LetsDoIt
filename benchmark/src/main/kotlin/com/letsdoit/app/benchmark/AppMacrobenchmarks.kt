package com.letsdoit.app.benchmark

import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkRule
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import androidx.room.Room
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
class AppMacrobenchmarks {
    @get:Rule
    val macroRule = MacrobenchmarkRule()

    @Test
    fun coldStartToList() {
        macroRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(StartupTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.COLD,
            setupBlock = {
                device().executeShellCommand("am force-stop $PACKAGE_NAME")
            }
        ) {
            pressHome()
            startActivityAndWait()
            waitForQuickAdd()
            openTab("List")
        }
    }

    @Test
    fun openBoardTab() {
        macroRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.WARM,
            setupBlock = {
                launchFromHome()
            }
        ) {
            val device = device()
            device.findObject(By.text("Buckets"))?.click()
            device.wait(Until.hasObject(By.text("No buckets available")), 5_000)
            device.waitForIdle()
        }
    }

    @Test
    fun openTimelineTab() {
        macroRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.WARM,
            setupBlock = {
                launchFromHome()
            }
        ) {
            val device = device()
            device.findObject(By.text("Timeline"))?.click()
            device.wait(Until.hasObject(By.text("Nothing on your list yet")), 5_000)
            device.waitForIdle()
        }
    }

    @Test
    fun addQuickTask() {
        macroRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            iterations = 5,
            startupMode = StartupMode.WARM,
            setupBlock = {
                clearDatabase()
                launchFromHome()
            }
        ) {
            val device = device()
            val placeholder = "Add a task with natural language"
            val input = device.findObject(By.text(placeholder))
            input?.click()
            input?.text = "Macrobench quick task ${System.nanoTime()}"
            device.findObject(By.text("Add"))?.click()
            device.wait(Until.hasObject(By.textStartsWith("Macrobench quick task")), 5_000)
            device.waitForIdle()
        }
    }

    @Test
    fun scrollPopulatedList() {
        macroRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()),
            iterations = 3,
            startupMode = StartupMode.WARM,
            setupBlock = {
                seedLongList()
                launchFromHome()
            }
        ) {
            val device = device()
            val list = UiScrollable(UiSelector().scrollable(true))
            list.setAsVerticalList()
            list.scrollToEnd(10)
            list.scrollToBeginning(10)
            device.waitForIdle()
        }
    }

    private fun MacrobenchmarkScope.launchFromHome() {
        pressHome()
        startActivityAndWait()
        waitForQuickAdd()
        openTab("List")
    }

    private fun MacrobenchmarkScope.waitForQuickAdd() {
        val device = device()
        device.wait(Until.hasObject(By.text("Add")), 5_000)
    }

    private fun MacrobenchmarkScope.openTab(label: String) {
        val device = device()
        device.findObject(By.text(label))?.click()
        device.waitForIdle()
    }

    private fun MacrobenchmarkScope.clearDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.databaseBuilder(context, AppDatabase::class.java, "letsdoit.db")
            .allowMainThreadQueries()
            .build()
        runBlocking {
            database.clearAllTables()
        }
        database.close()
    }

    private fun MacrobenchmarkScope.seedLongList() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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

    private fun device(): UiDevice {
        return UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    companion object {
        private const val PACKAGE_NAME = "com.letsdoit.app"
    }
}
