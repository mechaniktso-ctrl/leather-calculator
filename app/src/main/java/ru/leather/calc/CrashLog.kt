package ru.leather.calc

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashLog {
    private const val DIR = "crash"
    private const val FILE = "last-crash.txt"

    fun file(context: Context): File = File(File(context.filesDir, DIR), FILE)

    fun install(context: Context) {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching {
                val f = file(context)
                f.parentFile?.mkdirs()
                val stamp = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("ru")).format(Date())
                f.writeText(
                    "Когда: $stamp\nПоток: ${thread.name}\n\n" +
                        error.stackTraceToString()
                )
            }
            prev?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? {
        val f = file(context)
        return if (f.exists()) f.readText() else null
    }

    fun clear(context: Context) {
        runCatching { file(context).delete() }
    }

    fun share(context: Context) {
        val f = file(context)
        if (!f.exists()) return
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Отчёт о сбое Leather Calc")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Отправить отчёт"))
    }
}
