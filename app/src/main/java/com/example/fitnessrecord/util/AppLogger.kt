package com.example.fitnessrecord.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private const val FILE_NAME = "fra-runtime.log"
    private const val MAX_BYTES = 512 * 1024
    private const val TRIM_BYTES = 384 * 1024
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Volatile
    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.applicationContext.filesDir, FILE_NAME)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        append("INFO", tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val detail = buildString {
            append(message)
            throwable?.let {
                append("\n")
                append(it.stackTraceToString())
            }
        }
        append("ERROR", tag, detail)
    }

    suspend fun read(): String = withContext(Dispatchers.IO) {
        val file = logFile ?: return@withContext "Log is not initialized."
        if (!file.exists() || file.length() == 0L) return@withContext "No runtime logs yet."
        file.readText()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        logFile?.writeText("")
    }

    private fun append(level: String, tag: String, message: String) {
        val file = logFile ?: return
        runCatching {
            synchronized(this) {
                trimIfNeeded(file)
                val timestamp = timestampFormat.format(Date())
                file.appendText("[$timestamp][$level][$tag] $message\n")
            }
        }
    }

    private fun trimIfNeeded(file: File) {
        if (!file.exists() || file.length() <= MAX_BYTES) return
        val text = file.readText()
        val keepFrom = (text.length - TRIM_BYTES).coerceAtLeast(0)
        file.writeText(text.substring(keepFrom).substringAfter('\n'))
    }
}
