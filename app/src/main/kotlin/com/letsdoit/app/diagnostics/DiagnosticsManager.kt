package com.letsdoit.app.diagnostics

import android.content.Context
import android.net.Uri
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import com.letsdoit.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class DiagnosticsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val redactor: DiagnosticsRedactor,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val logMutex = Mutex()
    private val memoryEntries = ArrayDeque<String>()
    private var memoryBytes = 0
    private val maxBytes = 512 * 1024
    private val diagnosticsDir: File get() = File(context.filesDir, "diagnostics")
    private val logFile: File get() = File(diagnosticsDir, "log.txt")
    private val bundleAuthority = "${BuildConfig.APPLICATION_ID}.diagnostics"
    @Volatile
    private var enabled = false
    private var handler: DiagnosticsExceptionHandler? = null
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
        .withLocale(Locale.UK)
        .withZone(ZoneId.systemDefault())
    private val crashFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
        .withLocale(Locale.UK)
        .withZone(ZoneId.systemDefault())

    suspend fun setEnabled(value: Boolean) {
        withContext(dispatcher) {
            if (enabled == value) {
                return@withContext
            }
            enabled = value
            if (value) {
                diagnosticsDir.mkdirs()
                installHandler()
                logImmediate("Diagnostics", "Diagnostics enabled")
            } else {
                logImmediate("Diagnostics", "Diagnostics disabled")
                uninstallHandler()
            }
        }
    }

    fun log(tag: String, message: String) {
        if (!enabled) {
            return
        }
        val entry = formatEntry(tag, message)
        scope.launch {
            persistEntry(entry)
        }
    }

    fun recordCrash(throwable: Throwable, thread: Thread = Thread.currentThread()) {
        if (!enabled) {
            return
        }
        val now = Instant.now()
        val name = "crash-${crashFormatter.format(now)}-${UUID.randomUUID().toString().take(8)}.txt"
        val file = File(diagnosticsDir, name)
        runCatching {
            diagnosticsDir.mkdirs()
            val content = buildString {
                appendLine("Time: ${timestampFormatter.format(now)}")
                appendLine("Thread: ${thread.name}")
                appendLine("Exception: ${throwable::class.java.name}")
                appendLine()
                append(throwable.stackTraceToString())
            }
            file.writeText(content)
        }
    }

    suspend fun createSupportBundle(): DiagnosticsBundle? {
        return withContext(dispatcher) {
            runCatching {
                diagnosticsDir.mkdirs()
                cleanupOldBundles()
                val bundleFile = File(context.cacheDir, "support-bundle-${System.currentTimeMillis()}.zip")
                val logEntry = prepareLogEntry()
                ZipOutputStream(FileOutputStream(bundleFile)).use { zip ->
                    writeMetadata(zip)
                    logEntry?.let { writeLog(zip, it) }
                    writeCrashes(zip)
                }
                val uri = FileProvider.getUriForFile(context, bundleAuthority, bundleFile)
                log("Diagnostics", "Support bundle created")
                DiagnosticsBundle(uri = uri, fileName = bundleFile.name)
            }.getOrNull()
        }
    }

    private suspend fun persistEntry(entry: String) {
        logMutex.withLock {
            appendMemory(entry)
            appendFile(entry)
        }
    }

    private fun appendMemory(entry: String) {
        val bytes = entry.toByteArray(Charsets.UTF_8).size
        memoryEntries.addLast(entry)
        memoryBytes += bytes
        while (memoryBytes > maxBytes && memoryEntries.isNotEmpty()) {
            val removed = memoryEntries.removeFirst()
            memoryBytes -= removed.toByteArray(Charsets.UTF_8).size
        }
    }

    private fun appendFile(entry: String) {
        diagnosticsDir.mkdirs()
        val line = entry + "\n"
        logFile.appendText(line, Charsets.UTF_8)
        if (logFile.length() > maxBytes) {
            trimLogFile()
        }
    }

    private fun trimLogFile() {
        val length = logFile.length()
        if (length <= maxBytes) {
            return
        }
        val bytes = ByteArray(maxBytes)
        RandomAccessFile(logFile, "r").use { reader ->
            reader.seek(length - maxBytes)
            reader.readFully(bytes)
        }
        RandomAccessFile(logFile, "rw").use { writer ->
            writer.setLength(0)
            writer.write(bytes)
        }
    }

    private fun formatEntry(tag: String, message: String): String {
        val now = Instant.now()
        return "${timestampFormatter.format(now)} [$tag] $message"
    }

    private suspend fun logImmediate(tag: String, message: String) {
        val entry = formatEntry(tag, message)
        logMutex.withLock {
            appendMemory(entry)
            appendFile(entry)
        }
    }

    private fun installHandler() {
        if (handler != null) {
            return
        }
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val newHandler = DiagnosticsExceptionHandler(previous)
        handler = newHandler
        Thread.setDefaultUncaughtExceptionHandler(newHandler)
    }

    private fun uninstallHandler() {
        val currentHandler = handler ?: return
        val previous = currentHandler.previous
        if (Thread.getDefaultUncaughtExceptionHandler() === currentHandler) {
            Thread.setDefaultUncaughtExceptionHandler(previous)
        }
        handler = null
    }

    private fun cleanupOldBundles() {
        context.cacheDir.listFiles { file -> file.name.startsWith("support-bundle-") }?.forEach { it.delete() }
    }

    private fun writeMetadata(zip: ZipOutputStream) {
        val packageManager = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(context.packageName, 0)
        }
        val versionName = info.versionName ?: ""
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        val metadata = buildString {
            appendLine("App version: $versionName ($versionCode)")
            appendLine("Device model: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android version: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
            appendLine("Feature flags: none")
        }
        val content = redactor.redact(metadata).toByteArray(Charsets.UTF_8)
        zip.putNextEntry(ZipEntry("metadata.txt"))
        zip.write(content)
        zip.closeEntry()
    }

    private suspend fun prepareLogEntry(): ByteArray? {
        val content = logMutex.withLock {
            when {
                logFile.exists() -> logFile.readText(Charsets.UTF_8)
                memoryEntries.isNotEmpty() -> memoryEntries.joinToString(separator = "\n")
                else -> null
            }
        }
        if (content.isNullOrBlank()) {
            return null
        }
        return redactor.redact(content).toByteArray(Charsets.UTF_8)
    }

    private fun writeLog(zip: ZipOutputStream, entryContent: ByteArray) {
        zip.putNextEntry(ZipEntry("log.txt"))
        zip.write(entryContent)
        zip.closeEntry()
    }

    private fun writeCrashes(zip: ZipOutputStream) {
        diagnosticsDir.listFiles { file -> file.name.startsWith("crash-") }?.sortedBy { it.name }?.forEach { file ->
            val content = redactor.redact(file.readText(Charsets.UTF_8)).toByteArray(Charsets.UTF_8)
            zip.putNextEntry(ZipEntry("crashes/${file.name}"))
            zip.write(content)
            zip.closeEntry()
        }
    }

    private inner class DiagnosticsExceptionHandler(val previous: Thread.UncaughtExceptionHandler?) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, throwable: Throwable) {
            runCatching { recordCrash(throwable, thread) }
            previous?.uncaughtException(thread, throwable)
        }
    }
}

data class DiagnosticsBundle(
    val uri: Uri,
    val fileName: String
)
