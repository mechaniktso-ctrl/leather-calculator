package ru.leather.calc

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil

/**
 * Печать деталей в масштабе 1:1 на A4: каждая страница — фрагмент с рамкой,
 * названием детали и контрольным квадратом 50×50 мм для проверки масштаба печати.
 */
object PdfPieces {

    private const val MM = 2.83465f          // пунктов в миллиметре
    private const val PAGE_W = 595f          // A4, точки
    private const val PAGE_H = 842f
    private const val MARGIN = 28f           // ~10 мм

    val printableWmm: Float get() = (PAGE_W - MARGIN * 2) / MM
    val printableHmm: Float get() = (PAGE_H - MARGIN * 2) / MM

    fun pagesFor(wMm: Float, hMm: Float): Int {
        val cols = ceil(wMm / printableWmm).toInt().coerceAtLeast(1)
        val rows = ceil(hMm / printableHmm).toInt().coerceAtLeast(1)
        return cols * rows
    }

    fun build(context: Context, s: AppState): File {
        val doc = PdfDocument()
        var pageNo = 1
        val title = Paint().apply {
            isAntiAlias = true; textSize = 12f
            color = android.graphics.Color.rgb(20, 40, 60)
        }
        val small = Paint().apply {
            isAntiAlias = true; textSize = 9f
            color = android.graphics.Color.rgb(90, 110, 130)
        }
        val line = Paint().apply {
            isAntiAlias = true; style = Paint.Style.STROKE
            strokeWidth = 1f
            color = android.graphics.Color.rgb(30, 50, 70)
        }
        val dash = Paint().apply {
            isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 0.7f
            color = android.graphics.Color.rgb(180, 196, 214)
        }

        s.pieces.forEach { piece ->
            val w = num(piece.w).toFloat()
            val h = num(piece.h).toFloat()
            if (w <= 0f || h <= 0f) return@forEach
            val qty = (piece.qty.toIntOrNull() ?: 1).coerceAtLeast(1)

            for (copy in 1..qty) {
                val cols = ceil(w / printableWmm).toInt().coerceAtLeast(1)
                val rows = ceil(h / printableHmm).toInt().coerceAtLeast(1)

                for (row in 0 until rows) {
                    for (col in 0 until cols) {
                        val page = doc.startPage(
                            PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), pageNo).create()
                        )
                        val c = page.canvas
                        c.drawText(piece.name + " · ${w.toInt()}×${h.toInt()} " + Lang.doc("мм", "mm") + (if (qty > 1) Lang.doc(" · копия ", " · copy ") + "$copy/$qty" else ""), MARGIN, MARGIN, title)
                        c.drawText(
                            Lang.doc("фрагмент ", "fragment ") + "${row * cols + col + 1}" + Lang.doc(" из ", " of ") + "${rows * cols} · 1:1",
                            MARGIN, MARGIN + 14f, small
                        )

                        val offsetX = col * printableWmm
                        val offsetY = row * printableHmm
                        val visibleW = minOf(printableWmm, w - offsetX)
                        val visibleH = minOf(printableHmm, h - offsetY)

                        val left = MARGIN
                        val top = MARGIN + 26f
                        c.drawRect(
                            left, top,
                            left + visibleW * MM, top + visibleH * MM,
                            line
                        )
                        // сетка 10 мм
                        var x = 0f
                        while (x < visibleW) {
                            c.drawLine(left + x * MM, top, left + x * MM, top + visibleH * MM, dash)
                            x += 10f
                        }
                        var y = 0f
                        while (y < visibleH) {
                            c.drawLine(left, top + y * MM, left + visibleW * MM, top + y * MM, dash)
                            y += 10f
                        }

                        // контрольный квадрат 50×50 мм на последней странице детали
                        if (row == rows - 1 && col == cols - 1) {
                            val sx = left
                            val sy = top + visibleH * MM + 12f
                            if (sy + 50 * MM < PAGE_H - MARGIN) {
                                c.drawRect(sx, sy, sx + 50 * MM, sy + 50 * MM, line)
                                c.drawText(Lang.doc("50×50 мм — проверка масштаба", "50×50 mm — scale check"), sx, sy + 50 * MM + 10f, small)
                            }
                        }

                        c.drawText("$pageNo", PAGE_W - MARGIN - 10f, PAGE_H - MARGIN + 10f, small)
                        doc.finishPage(page)
                        pageNo++
                    }
                }
            }
        }

        if (pageNo == 1) {
            val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), 1).create())
            page.canvas.drawText(Lang.doc("Нет деталей с размерами", "No pieces with sizes"), MARGIN, MARGIN + 20f, title)
            doc.finishPage(page)
        }

        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "patterns-${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
