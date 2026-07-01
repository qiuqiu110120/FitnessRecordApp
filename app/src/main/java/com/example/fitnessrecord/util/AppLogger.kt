package com.example.fitnessrecord.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

object AppLogger {
    private const val FILE_NAME = "fra-runtime.log"
    private const val CRASH_MARKER_FILE_NAME = "last_crash.txt"
    private const val MAX_LOG_BYTES = 1024 * 1024L
    private const val MAX_LOG_FILES = 5
    private const val MAX_FATAL_STACK_CHARS = 16 * 1024
    private const val REDACTED = "[REDACTED]"

    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US)
    private val fileLock = Any()

    @Volatile
    private var filesDir: File? = null

    @Volatile
    private var previousCrashSummary: String? = null

    @Volatile
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    @Volatile
    private var isDebuggable: Boolean = false

    @Volatile
    private var environmentInfo: String = "environment=unknown"

    val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        e("Coroutine", "Unhandled coroutine exception", throwable)
    }

    fun init(context: Context) {
        val appContext = context.applicationContext
        filesDir = appContext.filesDir
        isDebuggable = (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        environmentInfo = buildEnvironmentSummary(appContext)
        registerCrashHandler()
        writeStartupInfo()
        recordPreviousCrashIfPresent()
    }

    fun d(tag: String, message: String) {
        if (!isDebuggable) return
        Log.d(tag, message)
        append("DEBUG", tag, message)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        append("INFO", tag, message)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        append("WARN", tag, formatMessage(message, throwable))
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        append("ERROR", tag, formatMessage(message, throwable))
    }

    fun fatal(tag: String, message: String, throwable: Throwable) {
        Log.e(tag, message, throwable)
        append("FATAL", tag, formatMessage(message, throwable))
    }

    fun sanitizeForLog(value: String): String = sanitize(value)

    fun hasPreviousCrash(): Boolean = previousCrashSummary != null || crashMarkerFile()?.exists() == true

    fun clearPreviousCrash() {
        previousCrashSummary = null
        runCatching { crashMarkerFile()?.delete() }
    }

    suspend fun read(): String = withContext(Dispatchers.IO) {
        val text = readAllLogsLocked()
        if (text.isBlank()) "No runtime logs yet." else text
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        synchronized(fileLock) {
            logFilesOldestFirst().forEach { file ->
                runCatching { if (file.exists()) file.delete() }
            }
            runCatching { logFile().writeText("") }
            clearPreviousCrash()
        }
    }

    suspend fun exportText(): String = withContext(Dispatchers.IO) {
        val text = readAllLogsLocked(includeHeader = true)
        if (text.isBlank()) "No runtime logs yet." else text
    }

    private fun registerCrashHandler() {
        if (previousHandler != null) return
        val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
        previousHandler = currentHandler
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeFatalCrashMarker(thread, throwable)
            writeFatalLine(thread, throwable)
            currentHandler?.uncaughtException(thread, throwable) ?: exitProcess(10)
        }
    }

    private fun writeStartupInfo() {
        append(
            level = "INFO",
            tag = "App",
            message = "App started $environmentInfo"
        )
    }

    private fun recordPreviousCrashIfPresent() {
        val marker = crashMarkerFile() ?: return
        val markerText = runCatching {
            if (marker.exists()) marker.readText().take(MAX_FATAL_STACK_CHARS) else null
        }.getOrNull() ?: return
        val alreadyLogged = markerText.lineSequence().any { it == "loggedToRuntime=true" }
        val summary = markerText.lineSequence()
            .filterNot { it == "loggedToRuntime=true" }
            .joinToString("\n")

        previousCrashSummary = summary
        if (!alreadyLogged) {
            append("ERROR", "Crash", "Previous crash detected\n$summary")
            runCatching { marker.writeText("$summary\nloggedToRuntime=true\n") }
        }
    }

    private fun writeFatalCrashMarker(thread: Thread, throwable: Throwable) {
        val marker = crashMarkerFile() ?: return
        val summary = fatalCrashSummary(thread, throwable)
        runCatching {
            marker.writeText(summary)
        }
    }

    private fun writeFatalLine(thread: Thread, throwable: Throwable) {
        val file = logFileOrNull() ?: return
        val timestamp = now()
        val line = buildString {
            append(timestamp)
            append(" FATAL [Crash] [")
            append(sanitize(thread.name))
            append("] Uncaught exception. Crash marker written.\n")
            append(fatalCrashSummary(thread, throwable))
            append("\n")
        }
        runCatching {
            file.appendText(sanitize(line))
        }
    }

    private fun fatalCrashSummary(thread: Thread, throwable: Throwable): String {
        val root = throwable.rootCause()
        return buildString {
            append("crashTime=").append(now()).append('\n')
            append("thread=").append(thread.name).append('\n')
            append("exception=").append(throwable::class.java.name).append('\n')
            append("message=").append(throwable.message.orEmpty()).append('\n')
            append("topCause=").append(root::class.java.name).append(": ").append(root.message.orEmpty()).append('\n')
            append("stackTrace=\n")
            append(throwable.stackTraceToString().take(MAX_FATAL_STACK_CHARS))
        }.let(::sanitize)
    }

    private fun append(level: String, tag: String, message: String) {
        val file = logFileOrNull() ?: return
        runCatching {
            synchronized(fileLock) {
                rotateIfNeeded(file)
                val safeMessage = sanitize(message)
                val safeTag = sanitize(tag)
                val safeThread = sanitize(Thread.currentThread().name)
                file.appendText("${now()} $level [$safeTag] [$safeThread] $safeMessage\n")
            }
        }
    }

    private fun readAllLogsLocked(includeHeader: Boolean = false): String {
        return synchronized(fileLock) {
            val builder = StringBuilder()
            if (includeHeader) {
                builder.append("===== environment =====\n")
                builder.append(environmentInfo).append("\n\n")
                builder.append("日志可能包含设备信息、错误信息和部分接口响应片段，请确认后再分享。\n\n")
            }
            logFilesOldestFirst()
                .filter { it.exists() && it.length() > 0L }
                .forEach { file ->
                    builder.append("===== ").append(file.name).append(" =====\n")
                    builder.append(runCatching { file.readText() }.getOrDefault(""))
                    if (!builder.endsWith("\n")) builder.append('\n')
                    builder.append('\n')
                }
            builder.toString().trimEnd()
        }
    }

    private fun rotateIfNeeded(file: File) {
        if (!file.exists() || file.length() < MAX_LOG_BYTES) return
        for (index in MAX_LOG_FILES - 1 downTo 1) {
            val source = rotatedLogFile(index)
            val target = rotatedLogFile(index + 1)
            if (source.exists()) {
                if (index == MAX_LOG_FILES - 1) {
                    source.delete()
                } else {
                    source.renameTo(target)
                }
            }
        }
        file.renameTo(rotatedLogFile(1))
        file.writeText("")
    }

    private fun logFilesOldestFirst(): List<File> {
        return (MAX_LOG_FILES - 1 downTo 1).map(::rotatedLogFile) + logFile()
    }

    private fun rotatedLogFile(index: Int): File = File(requireNotNull(filesDir), "fra-runtime.$index.log")

    private fun logFile(): File = File(requireNotNull(filesDir), FILE_NAME)

    private fun logFileOrNull(): File? = filesDir?.let { File(it, FILE_NAME) }

    private fun crashMarkerFile(): File? = filesDir?.let { File(it, CRASH_MARKER_FILE_NAME) }

    private fun formatMessage(message: String, throwable: Throwable?): String {
        return if (throwable == null) {
            message
        } else {
            "$message\n${throwable.stackTraceToString()}"
        }
    }

    private fun buildEnvironmentSummary(context: Context): String {
        val packageInfo = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode ?: 0L
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong() ?: 0L
        }
        return buildString {
            append("appVersion=").append(packageInfo?.versionName ?: "unknown")
            append(" versionCode=").append(versionCode)
            append(" buildType=").append(if (isDebuggable) "debug" else "release")
            append(" androidVersion=").append(Build.VERSION.RELEASE)
            append(" sdkInt=").append(Build.VERSION.SDK_INT)
            append(" deviceManufacturer=").append(Build.MANUFACTURER)
            append(" deviceModel=").append(Build.MODEL)
            append(" processName=").append(currentProcessName())
        }.let(::sanitize)
    }

    private fun currentProcessName(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            android.app.Application.getProcessName()
        } else {
            "unknown"
        }
    }

    private fun now(): String = synchronized(timestampFormat) {
        timestampFormat.format(Date())
    }

    private fun Throwable.rootCause(): Throwable {
        var current = this
        while (current.cause != null && current.cause !== current) {
            current = current.cause ?: break
        }
        return current
    }

    // This logger assumes a single-process app. If services/providers use android:process,
    // switch to file locks or per-process log files before writing from multiple processes.
    private fun sanitize(value: String): String {
        var result = value
        result = authorizationHeaderRegex.replace(result) { match ->
            "${match.groupValues[1]}$REDACTED"
        }
        result = sensitiveHeaderRegex.replace(result) { match ->
            "${match.groupValues[1]}$REDACTED"
        }
        result = jsonSecretRegex.replace(result) { match ->
            "${match.groupValues[1]}$REDACTED${match.groupValues[3]}"
        }
        result = urlSecretRegex.replace(result) { match ->
            "${match.groupValues[1]}$REDACTED"
        }
        result = bearerTokenRegex.replace(result) { match ->
            "${match.groupValues[1]} $REDACTED"
        }
        result = skTokenRegex.replace(result, "sk-$REDACTED")
        result = uriRegex.replace(result) { match ->
            "${match.groupValues[1]}$REDACTED"
        }
        return result
    }

    private val authorizationHeaderRegex =
        Regex("""(?im)\b(authorization\s*[:=]\s*bearer\s+)[^\s,;]+""")
    private val sensitiveHeaderRegex =
        Regex("""(?im)\b((?:x-api-key|access-token|api-key)\s*[:=]\s*)([^\s,;]+)""")
    private val jsonSecretRegex =
        Regex("""(?i)(["'](?:api_key|apiKey|access_token|accessToken|token)["']\s*:\s*["'])([^"']+)(["'])""")
    private val urlSecretRegex =
        Regex("""(?i)([?&](?:api_key|apiKey|access_token|accessToken|token)=)([^&#\s]+)""")
    private val bearerTokenRegex =
        Regex("""(?i)\b(Bearer)\s+[A-Za-z0-9._~+/=-]{12,}""")
    private val skTokenRegex =
        Regex("""\bsk-[A-Za-z0-9._-]{8,}""")
    private val uriRegex =
        Regex("""(?i)\b(file|content)://[^\s]+""")
}
