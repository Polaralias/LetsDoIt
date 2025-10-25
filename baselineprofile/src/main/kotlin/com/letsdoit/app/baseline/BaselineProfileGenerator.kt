package com.letsdoit.app.baseline

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineRule = BaselineProfileRule()

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun generate() {
        baselineRule.collectBaselineProfile(packageName = PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.text("Add")), 5_000)
            openList()
            openTimeline()
            openBoard()
            addTask()
        }
    }

    private fun openList() {
        device.findObject(By.text("List"))?.click()
        device.waitForIdle()
    }

    private fun openTimeline() {
        device.findObject(By.text("Timeline"))?.click()
        device.waitForIdle()
    }

    private fun openBoard() {
        device.findObject(By.text("Buckets"))?.click()
        device.waitForIdle()
    }

    private fun addTask() {
        device.findObject(By.text("List"))?.click()
        val input = device.findObject(By.text("Add a task with natural language"))
        input?.click()
        input?.text = "Baseline quick task"
        device.findObject(By.text("Add"))?.click()
        device.wait(Until.hasObject(By.text("Baseline quick task")), 5_000)
    }

    companion object {
        private const val PACKAGE_NAME = "com.letsdoit.app"
    }
}
