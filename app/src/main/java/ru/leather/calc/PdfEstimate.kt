package ru.leather.calc

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfEstimate {

    private fun paint(size: Float, bold: Boolean = false): Paint = Paint().apply {
        isAntiAlias = true
        textSize = size
        color = android.graphics.Color.rgb(26, 50, 76)
        typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
    }

    fun build(context: Context, s: AppState, productName: String): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val c = page.canvas
        val left = 40f
        val right = 555f
        var y = 56f

        val h1 = paint(20f, true)
        val h2 = paint(12f, true)
        val body = paint(11f)
        val small = paint(9f)

        c.drawText(Lang.doc("Смета изделия", "Item quote"), left, y, h1)
        y += 18f
        val date = SimpleDateFormat("dd.MM.yyyy", Locale("ru")).format(Date())
        c.drawText((productName.ifBlank { Lang.doc("Изделие без названия", "Untitled item") }) + " · $date", left, y, small)
        y += 26f

        fun row(label: String, value: String, bold: Boolean = false) {
            c.drawText(label, left, y, if (bold) paint(11f, true) else body)
            c.drawText(value, right - 120f, y, if (bold) paint(11f, true) else body)
            y += 17f
        }

        fun section(title: String) {
            y += 8f
            c.drawText(title, left, y, h2)
            y += 16f
            c.drawLine(left, y - 8f, right, y - 8f, Paint().apply {
                color = android.graphics.Color.rgb(180, 200, 222)
                strokeWidth = 1f
            })
        }

        section(Lang.doc("Материал", "Material"))
        row(Lang.doc("Площадь деталей", "Pieces area"), "${fmt(s.piecesArea, 2)} дм²")
        row(Lang.doc("Коэффициент раскроя", "Cutting factor"), "×${fmt(s.effectiveK, 2)}")
        row(Lang.doc("Нужно кожи", "Leather needed"), "${fmt(s.requiredArea, 1)} дм²")
        if (s.mode == PurchaseMode.PIECE) {
            row(Lang.doc("Шкур/листов", "Hides/sheets"), "${s.unitsNeeded} × ${fmt(num(s.hideArea), 0)} дм²")
            row(Lang.doc("Цена полезного дм²", "Price per usable dm²"), "${s.amount(s.pricePerUseful)}")
        } else {
            row(Lang.doc("Цена за дм²", "Price per dm²"), "${s.amount(num(s.areaPrice))}")
        }
        row(Lang.doc("Стоимость кожи", "Leather cost"), "${s.amount(s.leatherCost)}")

        section(Lang.doc("Затраты", "Costs"))
        row(Lang.doc("Фурнитура", "Hardware"), "${s.amount(s.hardwareTotal)}")
        row(Lang.doc("Расходники (", "Supplies (") + "${fmt(num(s.consumablesPct), 0)} %)", "${s.amount(s.consumables)}")
        row(Lang.doc("Работа: ", "Labor: ") + "${fmt(num(s.hours), 1)}" + Lang.doc(" ч × ", " h × ") + "${fmt(num(s.rate), 0)} ${s.cur}", "${s.amount(s.labor)}")
        row(Lang.doc("Брак (", "Defects (") + "${fmt(num(s.defectPct), 0)} %)", "${s.amount(s.defectsCost)}")
        row(Lang.doc("Накладные (", "Overhead (") + "${fmt(num(s.overheadPct), 0)} %)", "${s.amount(s.overhead)}")

        section(Lang.doc("Итог", "Total"))
        row(Lang.doc("Себестоимость", "Cost"), "${s.amount(s.totalCost)}", bold = true)
        row(Lang.doc("Цена (+", "Price (+") + "${fmt(num(s.markupPct), 0)} %)", "${s.amount(s.price)}", bold = true)
        row(Lang.doc("Прибыль", "Profit"), "${s.amount(s.profit)}")
        row(Lang.doc("Прибыль в час", "Profit per hour"), "${s.amount(s.profitPerHour)}/ч")

        y += 18f
        c.drawText(
            Lang.doc("Расчёт справочный: площадь по габаритным прямоугольникам, коэффициенты раскроя усреднённые.", "Reference calculation: areas by bounding rectangles, cutting factors averaged."),
            left, y, small
        )

        doc.finishPage(page)
        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "smeta-${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    /** Сохраняет копию в «Загрузки/DSH-Go». Возвращает путь или null. */
    fun saveToDownloads(context: Context, file: File, prefix: String = "smeta"): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return try {
            val name = "$prefix-${SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date())}.pdf"
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DSH-Go")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            "Download/DSH-Go/$name"
        } catch (e: Exception) {
            null
        }
    }

    fun share(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Смета изделия")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Отправить смету"))
    }
}
