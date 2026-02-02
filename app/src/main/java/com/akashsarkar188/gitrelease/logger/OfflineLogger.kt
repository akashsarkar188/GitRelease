package com.akashsarkar188.gitrelease.logger

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * OfflineLogger
 * 
 * A simple, copy-pasteable logger for Android apps to save logs to storage.
 * - Path: /sdcard/Android/media/{package_name}/logs/{yyyy-MM-dd}/log_X.txt
 * - Rotation: Max 10 files, 5MB each.
 */
object OfflineLogger {

    var isLoggingEnabled = true
    private const val TAG = "OfflineLogger"
    private const val MAX_FILE_SIZE = 5 * 1024 * 1024 // 5 MB
    private const val MAX_FILES = 10
    private val EXECUTOR = Executors.newSingleThreadExecutor()
    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val TIME_FORMAT = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    // Cached directory for today
    private var currentLogDir: File? = null
    private var currentDateStr: String = ""

    fun log(context: Context, tag: String, message: String) {
        if (!isLoggingEnabled) return
        
        // Also log to Logcat
        Log.d(tag, message)

        EXECUTOR.execute {
            try {
                writeLog(context, tag, message)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write log", e)
            }
        }
    }

    private fun writeLog(context: Context, tag: String, message: String) {
        val now = Date()
        val today = DATE_FORMAT.format(now)

        // Update directory if date changed
        if (currentLogDir == null || today != currentDateStr) {
            currentDateStr = today
            currentLogDir = getLogDirectory(context, today)
        }

        val dir = currentLogDir ?: return
        if (!dir.exists()) dir.mkdirs()

        val logFile = getRotatedLogFile(dir)
        val timestamp = TIME_FORMAT.format(now)
        val logEntry = "$timestamp [$tag]: $message\n"

        try {
            FileWriter(logFile, true).use { writer ->
                writer.append(logEntry)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLogDirectory(context: Context, dateStr: String): File {
        // Use external media dir: /sdcard/Android/media/com.package/logs/2023-10-27
        val externalMediaDirs = context.externalMediaDirs
        val baseDir = if (externalMediaDirs.isNotEmpty() && externalMediaDirs[0] != null) {
            externalMediaDirs[0]
        } else {
            // Fallback to external files dir
            context.getExternalFilesDir(null)
        }

        return File(baseDir, "logs/$dateStr")
    }

    private fun getRotatedLogFile(dir: File): File {
        // Find the latest log file that is under size limit
        // We name files log_0.txt, log_1.txt ...
        
        // 1. Check existing files
        val files = dir.listFiles { _, name -> name.startsWith("log_") && name.endsWith(".txt") }
            ?.sortedBy { it.name }
            ?.toMutableList() ?: mutableListOf()

        if (files.isEmpty()) {
            return File(dir, "log_0.txt")
        }

        val lastFile = files.last()
        if (lastFile.length() < MAX_FILE_SIZE) {
            return lastFile
        }

        // 2. Rotate
        if (files.size >= MAX_FILES) {
            // Delete oldest (log_0.txt ideally, but here we just take the first sorted)
            val oldest = files.removeAt(0)
            oldest.delete()
        }

        // Create new file
        // Parse index from last file: log_5.txt -> 5
        val lastIndex = try {
            lastFile.name.substringAfter("log_").substringBefore(".txt").toInt()
        } catch (e: Exception) { 0 }
        
        return File(dir, "log_${lastIndex + 1}.txt")
    }
}
