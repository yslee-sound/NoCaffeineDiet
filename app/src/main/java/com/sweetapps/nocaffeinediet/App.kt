package com.sweetapps.nocaffeinediet

import android.app.Application
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
    }

    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                val dir = File(filesDir, "crash_logs").apply { if (!exists()) mkdirs() }
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(dir, "crash_$ts.txt")
                file.writeText(buildString {
                    appendLine("Thread: ${t.name} (${t.id})")
                    appendLine("${e::class.java.name}: ${e.message}")
                    appendLine(e.stackTraceToString())
                })
                Log.e("App", "Crash captured -> ${file.absolutePath}")
            } catch (_: Exception) {
                // ignore
            } finally {
                previous?.uncaughtException(t, e) ?: run {
                    // 기본 동작: 즉시 프로세스 종료 위임
                    throw e
                }
            }
        }
    }
}

