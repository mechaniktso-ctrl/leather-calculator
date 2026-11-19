package ru.leather.calc

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Backup {

    /** Сохраняет все настройки и проекты в Download/DSH-Go. Возвращает путь или null. */
    fun export(context: Context, prefs: SharedPreferences): String? {
        return try {
            val obj = JSONObject()
            obj.put("_format", "leathercalc-backup-1")
            prefs.all.forEach { (k, v) ->
                when (v) {
                    is String -> obj.put(k, v)
                    is Int -> obj.put(k, v)
                    is Boolean -> obj.put(k, v)
                    is Long -> obj.put(k, v)
                    else -> Unit
                }
            }
            val json = obj.toString(2)
            val name = "leathercalc-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date()) + ".json"

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DSH-Go")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            "Download/DSH-Go/$name"
        } catch (e: Exception) {
            null
        }
    }

    fun importFrom(context: Context, uri: Uri, prefs: SharedPreferences): Boolean {
        return try {
            val text = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                ?: return false
            val obj = JSONObject(text)
            if (obj.optString("_format") != "leathercalc-backup-1") return false

            val editor = prefs.edit()
            for (key in obj.keys()) {
                if (key.startsWith("_")) continue
                when (val v = obj.get(key)) {
                    is String -> editor.putString(key, v)
                    is Int -> editor.putInt(key, v)
                    is Boolean -> editor.putBoolean(key, v)
                    is Long -> editor.putLong(key, v)
                }
            }
            editor.commit()
        } catch (e: Exception) {
            false
        }
    }
}
