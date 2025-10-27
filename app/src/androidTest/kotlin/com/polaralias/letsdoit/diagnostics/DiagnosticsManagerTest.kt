package com.polaralias.letsdoit.diagnostics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DiagnosticsManagerTest {
    private lateinit var context: Context
    private lateinit var manager: DiagnosticsManager
    private lateinit var diagnosticsDir: File

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        manager = DiagnosticsManager(context, DiagnosticsRedactor())
        diagnosticsDir = File(context.filesDir, "diagnostics")
        diagnosticsDir.deleteRecursively()
        manager.setEnabled(true)
    }

    @After
    fun tearDown() = runBlocking {
        manager.setEnabled(false)
        diagnosticsDir.deleteRecursively()
    }

    @Test
    fun writesCrashReportWhenEnabled() {
        val throwable = IllegalStateException("test failure")
        manager.recordCrash(throwable)
        val crashFiles = diagnosticsDir.listFiles { file -> file.name.startsWith("crash-") }
        assertTrue(crashFiles != null && crashFiles.isNotEmpty())
        val content = crashFiles!!.maxByOrNull { it.lastModified() }!!.readText()
        assertTrue(content.contains("IllegalStateException"))
    }
}
