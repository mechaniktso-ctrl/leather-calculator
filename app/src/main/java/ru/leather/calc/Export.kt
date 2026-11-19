package ru.leather.calc

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

object Export {

    private fun numOf(s: String): Double = s.replace(',', '.').trim().toDoubleOrNull() ?: 0.0

    private fun kindK(name: String): Double = when (name) {
        "BIG" -> 1.18
        "MID" -> 1.25
        "SMALL" -> 1.35
        "SCRAP" -> 1.50
        else -> 1.18
    }

    /** Площадь деталей, площадь с раскроем и стоимость кожи по снимку проекта. */
    fun stats(payload: String): Triple<Double, Double, Double> {
        return try {
            val o = JSONObject(payload)
            val arr = o.optJSONArray("pieces") ?: JSONArray()
            var area = 0.0
            var required = 0.0
            for (i in 0 until arr.length()) {
                val p = arr.getJSONObject(i)
                val one = numOf(p.optString("w")) * numOf(p.optString("h")) / 10000.0 *
                    maxOf(0, p.optString("q", "1").toIntOrNull() ?: 0)
                area += one
                required += one * kindK(p.optString("k"))
            }
            val myK = numOf(o.optString("myK", "1")).let { if (it <= 0) 1.0 else it }
            required *= myK

            val cost = if (o.optString("mode", "PIECE") == "AREA") {
                required * numOf(o.optString("areaPrice", "0"))
            } else {
                val hideArea = numOf(o.optString("hideArea", "0"))
                val yield = numOf(o.optString("yieldPct", "0"))
                val price = numOf(o.optString("hidePrice", "0"))
                val usable = hideArea * yield / 100.0
                val units = if (usable > 0) ceil(required / usable).toInt() else 0
                units * price
            }
            Triple(area, required, cost)
        } catch (e: Exception) {
            Triple(0.0, 0.0, 0.0)
        }
    }

    private fun csvCell(v: String): String = "\"" + v.replace("\"", "\"\"") + "\""

    private fun line(vararg cells: String): String = cells.joinToString(";") + "\n"

    private fun d(ru: String, en: String): String = Lang.doc(ru, en)

    /** Экспорт расчётов: текущий проект подробно + сводка по всем проектам. */
    fun exportCsv(context: Context, s: AppState): String? {
        return try {
            val sb = StringBuilder()
            sb.append('\uFEFF')
            val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru")).format(Date())

            sb.append(line(d("Проект", "Project"), csvCell(s.projName)))
            sb.append(line(d("Дата", "Date"), csvCell(date)))
            sb.append(line(d("Валюта", "Currency"), csvCell(s.cur)))
            sb.append("\n")

            sb.append(line(d("Параметры", "Settings"), ""))
            sb.append(line(d("Режим покупки", "Purchase mode"),
                if (s.mode == PurchaseMode.PIECE) d("шкура/лист", "hide/sheet") else d("за дм²", "per dm²")))
            sb.append(line(d("Площадь единицы, дм²", "Unit area, dm²"), s.hideArea))
            sb.append(line(d("Цена единицы", "Unit price"), s.hidePrice))
            sb.append(line(d("Полезный выход, %", "Usable yield, %"), s.yieldPct))
            sb.append(line(d("Цена за дм²", "Price per dm²"), s.areaPrice))
            sb.append(line(d("Личный коэффициент", "Personal factor"), s.myK))
            sb.append("\n")

            sb.append(line(
                d("Деталь", "Piece"),
                d("Длина, мм", "Length, mm"),
                d("Ширина, мм", "Width, mm"),
                d("Кол-во", "Qty"),
                d("Площадь, дм²", "Area, dm²"),
                d("Коэффициент", "Factor"),
                d("С раскроем, дм²", "With cut, dm²")
            ))
            s.pieces.forEach { p ->
                val area = num(p.w) * num(p.h) / 10000.0 * maxOf(0, p.qty.toIntOrNull() ?: 0)
                sb.append(line(
                    csvCell(p.name), p.w, p.h, p.qty,
                    fmt(area, 2), fmt(p.kind.k, 2), fmt(area * p.kind.k, 2)
                ))
            }
            sb.append("\n")

            sb.append(line(d("Итоги текущего проекта", "Current project totals"), ""))
            sb.append(line(d("Площадь деталей, дм²", "Pieces area, dm²"), fmt(s.piecesArea, 2)))
            sb.append(line(d("Нужно кожи, дм²", "Leather needed, dm²"), fmt(s.requiredArea, 2)))
            sb.append(line(d("Стоимость кожи", "Leather cost"), money(s.leatherCost)))
            sb.append(line(d("Фурнитура", "Hardware"), money(s.hardwareTotal)))
            sb.append(line(d("Расходники", "Supplies"), money(s.consumables)))
            sb.append(line(d("Работа", "Labor"), money(s.labor)))
            sb.append(line(d("Брак", "Defects"), money(s.defectsCost)))
            sb.append(line(d("Накладные", "Overhead"), money(s.overhead)))
            sb.append(line(d("Себестоимость", "Cost"), money(s.totalCost)))
            sb.append(line(d("Цена", "Price"), money(s.price)))
            sb.append(line(d("Прибыль", "Profit"), money(s.profit)))
            sb.append("\n")

            sb.append(line(d("Все проекты", "All projects"), ""))
            sb.append(line(
                d("Проект", "Project"),
                d("Площадь деталей, дм²", "Pieces area, dm²"),
                d("Нужно кожи, дм²", "Leather needed, dm²"),
                d("Кожа, ", "Leather, ") + s.cur
            ))
            s.projects.forEach { pr ->
                val (area, required, cost) = stats(pr.payload)
                sb.append(line(
                    csvCell(pr.name.ifBlank { d("Без названия", "Untitled") }),
                    fmt(area, 2), fmt(required, 2), money(cost)
                ))
            }

            val body = sb.toString()
            val name = "rasschet-" + SimpleDateFormat("yyyy-MM-dd-HHmm", Locale.US).format(Date()) + ".csv"

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/DSH-Go")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(body.toByteArray(Charsets.UTF_8))
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            "Download/DSH-Go/$name"
        } catch (e: Exception) {
            null
        }
    }
}
