package com.meet.libraryinsight.common

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date

object Logger {

    private val logFile: File by lazy {
        val dir = MavenResolver.cacheDir.parentFile
        if (!dir.exists()) {
            dir.mkdirs()
        }
        File(dir, "library-insight.log")
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    fun info(message: String) {
        log("INFO", message)
    }

    fun warn(message: String) {
        log("WARN", message)
    }

    fun error(message: String, throwable: Throwable? = null) {
        val extra = if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            "\n$sw"
        } else ""
        log("ERROR", "$message$extra")
    }

    @Synchronized
    private fun log(level: String, message: String) {
        try {
            val timestamp = dateFormat.format(Date())
            val logLine = "[$timestamp] [$level] $message\n"
            logFile.appendText(logLine)
        } catch (e: Exception) {
            // Fallback silently if logging fails to write
        }
    }
}
