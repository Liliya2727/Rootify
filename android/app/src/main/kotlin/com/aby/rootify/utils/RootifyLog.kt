package com.aby.rootify.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * RootifyLog: Native logging utility that writes to Logcat and a persistent file.
 * This file is shared with the Flutter-side AppLogger for cohesive system logging.
 */
object RootifyLog {
    private const val TAG = "Rootify"
    private var logFile: File? = null

    /**
     * Initialize the logger with the application context.
     * Maps to the internal storage 'rootify_app.log' file.
     */
    fun init(context: Context) {
        try {
            // Match Flutter's getApplicationDocumentsDirectory() which is context.filesDir
            logFile = File(context.filesDir, "rootify_app.log")
            
            // Initial log entry
            i("RootifyLog: Initialized native logging at ${logFile?.absolutePath}")
            
            // Check rotation (if native app stays open and writes too much)
            if (logFile!!.exists() && logFile!!.length() > 5 * 1024 * 1024) {
                 val backup = File(context.filesDir, "rootify_app.log.old")
                 if (backup.exists()) backup.delete()
                 logFile!!.renameTo(backup)
            }
        } catch (e: Exception) {
            Log.e(TAG, "RootifyLog: Initialization failed", e)
        }
    }

    fun d(msg: String) = log("DEBUG", msg)
    fun i(msg: String) = log("INFO", msg)
    fun w(msg: String) = log("WARN", msg)
    fun e(msg: String, tr: Throwable? = null) = log("ERROR", msg, tr)

    private fun log(level: String, msg: String, tr: Throwable? = null) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val formatted = "[$timestamp] [$level] $msg"
        
        // 1. Log to Logcat
        when (level) {
            "DEBUG" -> Log.d(TAG, msg, tr)
            "INFO"  -> Log.i(TAG, msg, tr)
            "WARN"  -> Log.w(TAG, msg, tr)
            "ERROR" -> Log.e(TAG, msg, tr)
        }

        // 2. Log to file
        writeToFile(formatted)
        if (tr != null) {
            writeToFile(Log.getStackTraceString(tr))
        }
    }

    private fun writeToFile(text: String) {
        try {
            logFile?.let { file ->
                FileOutputStream(file, true).use { fos ->
                    PrintWriter(fos).use { pw ->
                        pw.println(text)
                    }
                }
            }
        } catch (e: Exception) {
            // Silently fail to avoid recursion/deadlock
        }
    }
}
