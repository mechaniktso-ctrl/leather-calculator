package ru.leather.calc

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Экспорт деталей в DXF R12 (ASCII): прямоугольные контуры в миллиметрах.
 * Понимают все САПР и раскройные станки. Слой PIECES — контур, ALLOWANCE — припуск.
 */
object DxfExport {

    private const val ALLOWANCE = 3f

    fun buildFor(piecesIn: List<Triple<String, Float, Float>>): String {
        val sb = StringBuilder()
        fun pair(code: Int, value: String) {
            sb.append(code).append('\n').append(value).append('\n')
        }

        pair(0, "SECTION"); pair(2, "HEADER")
        pair(9, "\$ACADVER"); pair(1, "AC1009")
        pair(9, "\$INSUNITS"); pair(70, "4")   // миллиметры
        pair(0, "ENDSEC")

        pair(0, "SECTION"); pair(2, "ENTITIES")

        var y = 0f
        piecesIn.forEach { (name, w, h) ->
            // припуск — пунктирный слой
            pair(0, "LAYER")
            rect(sb, ::pair, 0f, y, w + ALLOWANCE * 2, h + ALLOWANCE * 2, "ALLOWANCE")
            rect(sb, ::pair, ALLOWANCE, y + ALLOWANCE, w, h, "PIECES")
            // подпись
            pair(0, "TEXT")
            pair(8, "NOTES")
            pair(10, us(ALLOWANCE))
            pair(20, us(y + h + ALLOWANCE + 4f))
            pair(40, "4")
            pair(1, "${name.replace('\n', ' ')} ${w.toInt()}x${h.toInt()} mm")
            y += h + ALLOWANCE * 2 + 12f
        }

        pair(0, "ENDSEC"); pair(0, "EOF")
        return sb.toString()
    }

    fun build(s: AppState): String = buildFor(
        s.pieces.mapNotNull {
            val w = num(it.w).toFloat()
            val h = num(it.h).toFloat()
            if (w <= 0f || h <= 0f) null else Triple(it.name, w, h)
        }
    )

    private fun rect(
        sb: StringBuilder,
        pair: (Int, String) -> Unit,
        x: Float, y: Float, w: Float, h: Float, layer: String
    ) {
        val pts = listOf(x to y, x + w to y, x + w to y + h, x to y + h, x to y)
        for (i in 0 until pts.size - 1) {
            pair(0, "LINE")
            pair(8, layer)
            pair(10, us(pts[i].first)); pair(20, us(pts[i].second)); pair(30, "0")
            pair(11, us(pts[i + 1].first)); pair(21, us(pts[i + 1].second)); pair(31, "0")
        }
    }

    private fun us(v: Float): String = String.format(Locale.US, "%.2f", v)

    fun save(context: Context, dxf: String): String? {
        return try {
            val name = "leather-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date()) + ".dxf"
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/dxf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DSH-Go")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { it.write(dxf.toByteArray(Charsets.UTF_8)) }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            "Download/DSH-Go/$name"
        } catch (e: Exception) {
            null
        }
    }

    fun share(context: Context, dxf: String) {
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "pattern-${System.currentTimeMillis()}.dxf")
        FileOutputStream(file).use { it.write(dxf.toByteArray(Charsets.UTF_8)) }
        PdfEstimate.share(context, file)
    }

    /** Сколько отверстий под седельный шов нужно по периметру детали. */
    fun stitchHoles(wMm: Float, hMm: Float, stepMm: Float, insetMm: Float = 3f): Int {
        if (stepMm <= 0f) return 0
        val pw = (wMm - 2 * insetMm).coerceAtLeast(0f)
        val ph = (hMm - 2 * insetMm).coerceAtLeast(0f)
        if (pw <= 0f || ph <= 0f) return 0
        val perimeter = 2 * (pw + ph)
        return (perimeter / stepMm).toInt() + 1
    }
}
