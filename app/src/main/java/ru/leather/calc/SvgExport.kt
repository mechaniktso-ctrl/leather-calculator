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
 * Экспорт деталей в SVG в миллиметрах, 1:1 — для станков, лазера и печати.
 * Детали идут столбцом с зазором 10 мм, у каждой подпись и припуск пунктиром.
 */
object SvgExport {

    private const val GAP = 12f
    private const val ALLOWANCE = 3f

    fun build(s: AppState): String = buildPieces(s.pieces)

    /** Экспорт с поддержкой произвольных контуров. */
    fun buildPieces(pieces: List<Piece>): String {
        val withContour = pieces.filter { parseContour(it.contour).size >= 3 && num(it.w) > 0 }
        if (withContour.isEmpty()) {
            return buildFor(
                pieces.mapNotNull {
                    val w = num(it.w).toFloat()
                    val h = num(it.h).toFloat()
                    if (w <= 0f || h <= 0f) null else Triple(it.name, w, h)
                }
            )
        }
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        var totalH = GAP.toFloat()
        pieces.forEach { p ->
            val h = num(p.h).toFloat()
            if (h > 0f) totalH += h + GAP
        }
        val width = (pieces.maxOfOrNull { num(it.w).toFloat() } ?: 100f) + ALLOWANCE * 2 + GAP * 2
        sb.append(
            """<svg xmlns="http://www.w3.org/2000/svg" width="${us(width)}mm" height="${us(totalH)}mm" """ +
                """viewBox="0 0 ${us(width)} ${us(totalH)}">""" + "\n"
        )
        sb.append("""<g stroke="#1A324C" stroke-width="0.4" fill="none">""").append('\n')
        var y = GAP
        pieces.forEach { p ->
            val w = num(p.w).toFloat()
            val h = num(p.h).toFloat()
            if (w <= 0f || h <= 0f) return@forEach
            val contour = parseContour(p.contour)
            if (contour.size >= 3) {
                val pts = contour.joinToString(" ") { "${us(it.first + ALLOWANCE)} ${us(it.second + y)}" }
                sb.append("""<polygon points="$pts"/>""").append('\n')
                sb.append(
                    """<text x="${us(ALLOWANCE)}" y="${us(y + h + 6f)}" font-family="sans-serif" font-size="4" """ +
                        """fill="#16324C" stroke="none">${escape(p.name)} ${w.toInt()}x${h.toInt()} ${Lang.doc("мм", "mm")}</text>""" + "\n"
                )
            } else {
                // прямоугольник — как раньше
                sb.append(
                    """<rect x="${us(ALLOWANCE)}" y="${us(y)}" width="${us(w)}" height="${us(h)}"/>""" + "\n"
                )
                sb.append(
                    """<text x="${us(ALLOWANCE)}" y="${us(y + h + 6f)}" font-family="sans-serif" font-size="4" """ +
                        """fill="#16324C" stroke="none">${escape(p.name)} ${w.toInt()}x${h.toInt()} ${Lang.doc("мм", "mm")}</text>""" + "\n"
                )
            }
            y += h + GAP
        }
        sb.append("</g>\n</svg>\n")
        return sb.toString()
    }

    fun buildFor(piecesIn: List<Triple<String, Float, Float>>): String {
        val pieces = piecesIn

        val totalH = pieces.sumOf { (it.third + GAP).toDouble() }.toFloat() + GAP
        val width = (pieces.maxOfOrNull { it.second } ?: 100f) + ALLOWANCE * 2 + GAP * 2
        val height = maxOf(totalH, 50f)

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append(
            """<svg xmlns="http://www.w3.org/2000/svg" width="${us(width)}mm" """ +
                """height="${us(height)}mm" viewBox="0 0 ${us(width)} ${us(height)}">""" +
                "\n"
        )
        sb.append("""<g stroke="#1A324C" stroke-width="0.4" fill="none">""").append('\n')

        var y = GAP
        pieces.forEach { (name, w, h) ->
            val x = GAP + ALLOWANCE
            sb.append(
                """<rect x="${us(x)}" y="${us(y)}" """ +
                    """width="${us(w)}" height="${us(h)}"/>""" + "\n"
            )
            sb.append(
                """<rect x="${us(x - ALLOWANCE)}" y="${us(y - ALLOWANCE)}" """ +
                    """width="${us(w + ALLOWANCE * 2)}" height="${us(h + ALLOWANCE * 2)}" """ +
                    """stroke-dasharray="2 2" stroke="#5D7A96"/>""" + "\n"
            )
            sb.append(
                """<text x="${us(x)}" y="${us(y + h + 6)}" """ +
                    """font-family="sans-serif" font-size="4" fill="#16324C" stroke="none">""" +
                    escape(name) + " " + w.toInt() + "×" + h.toInt() + " " + Lang.doc("мм", "mm") +
                    "</text>\n"
            )
            y += h + GAP
        }
        sb.append("</g>\n</svg>\n")
        return sb.toString()
    }

    private fun us(v: Float): String = String.format(Locale.US, "%.1f", v)

    private fun escape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    /** Сохраняет SVG в Download/DSH-Go и возвращает путь. */
    fun save(context: Context, svg: String): String? {
        return try {
            val name = "leather-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date()) + ".svg"
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/svg+xml")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DSH-Go")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(svg.toByteArray(Charsets.UTF_8))
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            "Download/DSH-Go/$name"
        } catch (e: Exception) {
            null
        }
    }

    /** Кладёт SVG во временный файл для «Поделиться». */
    fun share(context: Context, svg: String) {
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "pattern-${System.currentTimeMillis()}.svg")
        FileOutputStream(file).use { it.write(svg.toByteArray(Charsets.UTF_8)) }
        PdfEstimate.share(context, file)
    }
}
