package ru.leather.calc

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max

// ---------- модель ----------

enum class PieceKind(val label: String, val k: Double) {
    BIG("Крупная", 1.18),
    MID("Средняя", 1.25),
    SMALL("Мелочь", 1.35),
    SCRAP("Лоскут", 1.50)
}

enum class PurchaseMode { PIECE, AREA }

data class Piece(
    val name: String = "",
    val kind: PieceKind = PieceKind.BIG,
    val w: String = "",
    val h: String = "",
    val qty: String = "1",
    /** Произвольный контур в мм: "x,y;x,y;…". Пусто — значит обычный прямоугольник. */
    val contour: String = ""
)

fun parseContour(raw: String): List<Pair<Float, Float>> {
    if (raw.isBlank()) return emptyList()
    return raw.split(";").mapNotNull { pair ->
        val xy = pair.split(",")
        if (xy.size < 2) return@mapNotNull null
        val x = num(xy[0]).toFloat()
        val y = num(xy[1]).toFloat()
        x to y
    }
}

fun contourAreaMm2(points: List<Pair<Float, Float>>): Double {
    if (points.size < 3) return 0.0
    var sum = 0.0
    for (i in points.indices) {
        val (x1, y1) = points[i]
        val (x2, y2) = points[(i + 1) % points.size]
        sum += x1.toDouble() * y2 - x2.toDouble() * y1
    }
    return kotlin.math.abs(sum) / 2.0
}

fun contourToString(points: List<Pair<Float, Float>>): String =
    points.joinToString(";") { "${fmt(it.first.toDouble(), 1)},${fmt(it.second.toDouble(), 1)}" }

data class Defect(val x: Float, val y: Float, val r: Float)

data class Project(
    val id: String = "",
    val name: String = "",
    val payload: String = "",
    val updatedAt: Long = 0L
)

data class HwItem(
    val name: String = "",
    val price: String = "",
    val qty: String = "1"
)

data class StockItem(
    val name: String = "",
    val area: String = "",
    val price: String = "",
    val note: String = ""
)

fun num(s: String): Double = s.replace(',', '.').trim().toDoubleOrNull() ?: 0.0

fun fmt(v: Double, digits: Int): String =
    String.format(Locale.US, "%.${digits}f", v).replace('.', ',')

fun money(v: Double): String =
    String.format(Locale.forLanguageTag("ru-RU"), "%,.0f", v)
        .replace('\u00A0', ' ').replace('\u202F', ' ')

fun stitchesFor(seamMm: Double, stepMm: Double): Int =
    if (stepMm <= 0) 0 else ceil(seamMm / stepMm).toInt()

fun threadFor(seamMm: Double, stepMm: Double, pack: Double, allowanceMm: Double): Double =
    stitchesFor(seamMm, stepMm) * (2 * pack + stepMm) + allowanceMm

val kindPresets: Map<String, Pair<Double, Double>> = mapOf(
    "Полушкура" to (120.0 to 75.0),
    "Лист А3" to (12.5 to 95.0),
    "Лист А4" to (6.3 to 95.0),
    "Крупон" to (80.0 to 85.0),
    "Чепрак" to (60.0 to 80.0),
    "Вороток" to (40.0 to 70.0),
    "Лоскут" to (10.0 to 90.0)
)

fun niceNum(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else fmt(v, 1)

// ---------- раскладка ----------

data class PRect(val w: Float, val h: Float, val name: String, val contour: String = "")
data class PPlaced(
    val x: Float,
    val y: Float,
    val w: Float,
    val h: Float,
    val name: String,
    val contour: String = ""
)

data class PackSheet(val placed: List<PPlaced>, val usedArea: Float, val unplaced: List<PRect>)

fun packSheet(items: List<PRect>, sheetW: Float, sheetH: Float, allowRotate: Boolean): PackSheet {
    val sorted = items.sortedByDescending { it.w * it.h }
    val rows = mutableListOf<FloatArray>()
    var currentY = 0f
    val placed = mutableListOf<PPlaced>()
    val unplaced = mutableListOf<PRect>()

    for (item in sorted) {
        var done = false
        for (i in rows.indices) {
            val row = rows[i]
            if (item.h <= row[1] + 0.01f && row[2] + item.w <= sheetW + 0.01f) {
                placed.add(PPlaced(row[2], row[0], item.w, item.h, item.name, item.contour))
                row[2] += item.w
                done = true
                break
            }
            if (allowRotate && item.w <= row[1] + 0.01f && row[2] + item.h <= sheetW + 0.01f) {
                placed.add(PPlaced(row[2], row[0], item.h, item.w, item.name, item.contour))
                row[2] += item.h
                done = true
                break
            }
        }
        if (done) continue
        if (item.h <= sheetH - currentY + 0.01f && item.w <= sheetW + 0.01f) {
            rows.add(floatArrayOf(currentY, item.h, item.w))
            placed.add(PPlaced(0f, currentY, item.w, item.h, item.name, item.contour))
            currentY += item.h
        } else {
            unplaced.add(item)
        }
    }
    return PackSheet(placed, placed.sumOf { (it.w * it.h).toDouble() }.toFloat(), unplaced)
}

fun packAll(
    items: List<PRect>,
    sheetW: Float,
    sheetH: Float,
    allowRotate: Boolean,
    maxSheets: Int = 12
): List<PackSheet> {
    val sheets = mutableListOf<PackSheet>()
    var rest = items
    var guard = 0
    while (rest.isNotEmpty() && guard < maxSheets) {
        val sheet = packSheet(rest, sheetW, sheetH, allowRotate)
        sheets.add(sheet)
        rest = sheet.unplaced
        guard++
    }
    return sheets
}

// ---------- состояние ----------

class AppState(val prefs: SharedPreferences) {

    val pieces = mutableStateListOf<Piece>()
    val defects = mutableStateListOf<Defect>()
    val stock = mutableStateListOf<StockItem>()
    val customTemplates = mutableStateListOf<ItemTemplate>()
    val hw = mutableStateListOf<HwItem>()
    val fitCalc = mutableStateListOf<Double>()
    val fitActual = mutableStateListOf<Double>()

    var tab by mutableStateOf(0)
    var projName by mutableStateOf(prefs.getString("projName", "Проект 1") ?: "Проект 1")
    var activeId by mutableStateOf(prefs.getString("activeId", "") ?: "")
    val projects = mutableStateListOf<Project>()
    var product by mutableStateOf(prefs.getString("product", "") ?: "")

    var mode by mutableStateOf(
        if (prefs.getString("mode", "PIECE") == "AREA") PurchaseMode.AREA else PurchaseMode.PIECE
    )
    var hideKind by mutableStateOf(prefs.getString("hideKind", "Лист А3") ?: "Лист А3")
    var hideArea by mutableStateOf(prefs.getString("hideArea", "12,5") ?: "12,5")
    var hidePrice by mutableStateOf(prefs.getString("hidePrice", "450") ?: "450")
    var yieldPct by mutableStateOf(prefs.getString("yieldPct", "95") ?: "95")
    var areaPrice by mutableStateOf(prefs.getString("areaPrice", "36") ?: "36")

    var sheetW by mutableStateOf(prefs.getString("sheetW", "420") ?: "420")
    var sheetH by mutableStateOf(prefs.getString("sheetH", "297") ?: "297")
    var allowRotate by mutableStateOf(prefs.getBoolean("allowRotate", true))

    var seamCm by mutableStateOf(prefs.getString("seamCm", "30") ?: "30")
    var layers by mutableStateOf(prefs.getString("layers", "2") ?: "2")
    var layerMm by mutableStateOf(prefs.getString("layerMm", "1,6") ?: "1,6")
    var stepMm by mutableStateOf(prefs.getString("stepMm", "4") ?: "4")
    var allowanceCm by mutableStateOf(prefs.getString("allowanceCm", "20") ?: "20")
    var spoolM by mutableStateOf(prefs.getString("spoolM", "25") ?: "25")

    var hardware by mutableStateOf(prefs.getString("hardware", "450") ?: "450")
    var consumablesPct by mutableStateOf(prefs.getString("consumablesPct", "7") ?: "7")
    var hours by mutableStateOf(prefs.getString("hours", "6") ?: "6")
    var rate by mutableStateOf(prefs.getString("rate", "600") ?: "600")
    var defectPct by mutableStateOf(prefs.getString("defectPct", "7") ?: "7")
    var overheadPct by mutableStateOf(prefs.getString("overheadPct", "10") ?: "10")
    var markupPct by mutableStateOf(prefs.getString("markupPct", "40") ?: "40")

    var myK by mutableStateOf(prefs.getString("myK", "1") ?: "1")
    var actualDm2 by mutableStateOf(prefs.getString("actualDm2", "") ?: "")

    var order by mutableStateOf(prefs.getString("order", "1,2,4,7,0,3,5,6,9,8") ?: "1,2,4,7,0,3,5,6,9,8")

    var themeMode by mutableStateOf(prefs.getInt("themeMode", 0))
    var wizardStep by mutableStateOf(-1)
    var paramModelId by mutableStateOf(prefs.getString("paramModelId", "bag_flap") ?: "bag_flap")
    val paramValues = mutableStateMapOf<String, String>()

    var onboardingDone by mutableStateOf(prefs.getBoolean("onboarded", false))
    var timerStart by mutableStateOf(prefs.getLong("timerStart", 0L))
    var lastLesson by mutableStateOf(prefs.getInt("lastLesson", 0))

    var docLang by mutableStateOf(prefs.getString("docLang", "auto") ?: "auto")

    var langMode by mutableStateOf(prefs.getString("langMode", "system") ?: "system")

    var cur by mutableStateOf(prefs.getString("cur", "₽") ?: "₽")
    var tips by mutableStateOf(prefs.getBoolean("tips", true))
    val lessonsRead = mutableStateListOf<Int>()

    var oz by mutableStateOf(prefs.getString("oz", "6") ?: "6")
    var mm by mutableStateOf(prefs.getString("mm", "2,4") ?: "2,4")

    init {
        val raw = prefs.getString("pieces", null)
        if (raw.isNullOrBlank()) {
            pieces.add(Piece("Передняя панель", PieceKind.BIG, "270", "160", "2"))
            pieces.add(Piece("Боковина", PieceKind.MID, "80", "160", "2"))
        } else {
            raw.split("\n").filter { it.isNotBlank() }.forEach { line ->
                val p = line.split("|")
                when {
                    p.size >= 5 -> {
                        val kind = PieceKind.entries.firstOrNull { it.name == p[1] } ?: PieceKind.BIG
                        pieces.add(Piece(p[0], kind, p[2], p[3], p[4]))
                    }
                    p.size == 4 -> pieces.add(Piece(p[0], PieceKind.BIG, p[1], p[2], p[3]))
                    p.size == 5 -> pieces.add(Piece(p[0], PieceKind.BIG, p[1], p[2], p[3], p[4]))
                }
            }
        }

        prefs.getString("defects", null)?.split("\n")?.forEach { line ->
            val d = line.split("|")
            if (d.size >= 3) defects.add(Defect(num(d[0]).toFloat(), num(d[1]).toFloat(), num(d[2]).toFloat()))
        }

        prefs.getString("lessonsRead", null)?.split(",")?.forEach {
            it.trim().toIntOrNull()?.let { n -> lessonsRead.add(n) }
        }

        prefs.getString("hw", null)?.split("\n")?.forEach { line ->
            val h = line.split("|")
            if (h.size >= 3) hw.add(HwItem(h[0], h[1], h[2]))
        }

        prefs.getString("fit", null)?.split("\n")?.forEach { line ->
            val f = line.split("|")
            if (f.size >= 2) {
                fitCalc.add(num(f[0]))
                fitActual.add(num(f[1]))
            }
        }

        customTemplates.addAll(Templates.fromJson(prefs.getString("customTemplates", null)))

        prefs.getString("paramValues", null)?.split("\n")?.forEach { line ->
            val parts = line.split("=")
            if (parts.size == 2) paramValues[parts[0]] = parts[1]
        }

        prefs.getString("stock", null)?.split("\n")?.forEach { line ->
            val s = line.split("|")
            if (s.size >= 4) stock.add(StockItem(s[0], s[1], s[2], s[3]))
        }
    }

    init {
        prefs.getString("projects", null)?.let { raw ->
            try {
                val arr = org.json.JSONArray(raw)
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    projects.add(
                        Project(
                            o.optString("id"),
                            o.optString("name"),
                            o.optString("payload"),
                            o.optLong("updatedAt")
                        )
                    )
                }
            } catch (e: Exception) {
                // старый формат — просто создадим проект ниже
            }
        }
        if (projects.isEmpty()) {
            val id = "p" + System.currentTimeMillis()
            activeId = id
            projName = if (product.isNotBlank()) product else "Проект 1"
            projects.add(Project(id, projName, snapshotJson(), System.currentTimeMillis()))
        } else if (projects.none { it.id == activeId }) {
            activeId = projects.first().id
            projName = projects.first().name
            restoreFrom(projects.first().payload)
        }
    }

    // ---------- проект: снимок и восстановление ----------

    fun snapshotJson(): String {
        val root = org.json.JSONObject()
        root.put("schema", 2)
        root.put("projName", projName)
        root.put("product", product)
        root.put("mode", if (mode == PurchaseMode.PIECE) "PIECE" else "AREA")
        root.put("hideKind", hideKind)
        root.put("hideArea", hideArea)
        root.put("hidePrice", hidePrice)
        root.put("yieldPct", yieldPct)
        root.put("areaPrice", areaPrice)
        root.put("sheetW", sheetW)
        root.put("sheetH", sheetH)
        root.put("allowRotate", allowRotate)
        root.put("seamCm", seamCm)
        root.put("layers", layers)
        root.put("layerMm", layerMm)
        root.put("stepMm", stepMm)
        root.put("allowanceCm", allowanceCm)
        root.put("spoolM", spoolM)
        root.put("hardware", hardware)
        root.put("consumablesPct", consumablesPct)
        root.put("hours", hours)
        root.put("rate", rate)
        root.put("defectPct", defectPct)
        root.put("myK", myK)
        root.put("oz", oz)
        root.put("mm", mm)
        root.put("markupPct", markupPct)
        root.put("overheadPct", overheadPct)

        val pArr = org.json.JSONArray()
        pieces.forEach {
            pArr.put(
                org.json.JSONObject()
                    .put("n", it.name).put("k", it.kind.name)
                    .put("w", it.w).put("h", it.h).put("q", it.qty).put("c", it.contour)
            )
        }
        root.put("pieces", pArr)

        val dArr = org.json.JSONArray()
        defects.forEach { d -> dArr.put(org.json.JSONObject().put("x", d.x).put("y", d.y).put("r", d.r)) }
        root.put("defects", dArr)

        val hArr = org.json.JSONArray()
        hw.forEach { hArr.put(org.json.JSONObject().put("n", it.name).put("p", it.price).put("q", it.qty)) }
        root.put("hw", hArr)

        val fArr = org.json.JSONArray()
        for (i in fitCalc.indices) {
            fArr.put(org.json.JSONObject().put("c", fitCalc[i]).put("a", fitActual.getOrElse(i) { 0.0 }))
        }
        root.put("fit", fArr)
        return root.toString()
    }

    fun restoreFrom(payload: String) {
        if (payload.isBlank()) return
        try {
            val root = org.json.JSONObject(payload)
            if (root.optInt("schema", 0) > 2) return
            product = root.optString("product", "")
            mode = if (root.optString("mode", "PIECE") == "AREA") PurchaseMode.AREA else PurchaseMode.PIECE
            hideKind = root.optString("hideKind", hideKind)
            hideArea = root.optString("hideArea", hideArea)
            hidePrice = root.optString("hidePrice", hidePrice)
            yieldPct = root.optString("yieldPct", yieldPct)
            areaPrice = root.optString("areaPrice", areaPrice)
            sheetW = root.optString("sheetW", sheetW)
            sheetH = root.optString("sheetH", sheetH)
            allowRotate = root.optBoolean("allowRotate", allowRotate)
            seamCm = root.optString("seamCm", seamCm)
            layers = root.optString("layers", layers)
            layerMm = root.optString("layerMm", layerMm)
            stepMm = root.optString("stepMm", stepMm)
            allowanceCm = root.optString("allowanceCm", allowanceCm)
            spoolM = root.optString("spoolM", spoolM)
            hardware = root.optString("hardware", hardware)
            consumablesPct = root.optString("consumablesPct", consumablesPct)
            hours = root.optString("hours", hours)
            rate = root.optString("rate", rate)
            defectPct = root.optString("defectPct", defectPct)
            overheadPct = root.optString("overheadPct", overheadPct)
            markupPct = root.optString("markupPct", markupPct)
            myK = root.optString("myK", myK)
            oz = root.optString("oz", oz)
            mm = root.optString("mm", mm)

            pieces.clear()
            root.optJSONArray("pieces")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val kind = PieceKind.entries.firstOrNull { it.name == o.optString("k") } ?: PieceKind.BIG
                    pieces.add(Piece(o.optString("n"), kind, o.optString("w"), o.optString("h"), o.optString("q", "1"), o.optString("c")))
                }
            }
            defects.clear()
            root.optJSONArray("defects")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    defects.add(Defect(o.optDouble("x").toFloat(), o.optDouble("y").toFloat(), o.optDouble("r").toFloat()))
                }
            }
            hw.clear()
            root.optJSONArray("hw")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    hw.add(HwItem(o.optString("n"), o.optString("p"), o.optString("q", "1")))
                }
            }
            fitCalc.clear()
            fitActual.clear()
            root.optJSONArray("fit")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    fitCalc.add(o.optDouble("c"))
                    fitActual.add(o.optDouble("a"))
                }
            }
        } catch (e: Exception) {
            // битый снимок — оставляем текущее состояние
        }
    }

    private fun syncActiveProject() {
        val idx = projects.indexOfFirst { it.id == activeId }
        val rec = Project(activeId, projName, snapshotJson(), System.currentTimeMillis())
        if (idx >= 0) projects[idx] = rec else projects.add(rec)
    }

    fun switchProject(id: String) {
        if (id == activeId) return
        syncActiveProject()
        saveNow()
        val target = projects.firstOrNull { it.id == id } ?: return
        activeId = target.id
        projName = target.name
        restoreFrom(target.payload)
        saveNow()
    }

    fun newProject() {
        syncActiveProject()
        saveNow()
        val id = "p" + System.currentTimeMillis()
        projName = "Проект ${projects.size + 1}"
        activeId = id
        product = ""
        pieces.clear()
        defects.clear()
        hw.clear()
        fitCalc.clear()
        fitActual.clear()
        myK = "1"
        actualDm2 = ""
        projects.add(Project(id, projName, snapshotJson(), System.currentTimeMillis()))
        save()
    }

    fun duplicateProject() {
        syncActiveProject()
        save()
        val id = "p" + System.currentTimeMillis()
        projects.add(Project(id, "$projName — копия", snapshotJson(), System.currentTimeMillis()))
        save()
    }

    fun deleteProject() {
        val idx = projects.indexOfFirst { it.id == activeId }
        if (idx < 0) return
        projects.removeAt(idx)
        if (projects.isEmpty()) {
            newProject()
        } else {
            val next = projects.getOrNull(idx.coerceAtMost(projects.size - 1)) ?: projects.first()
            activeId = next.id
            projName = next.name
            restoreFrom(next.payload)
            save()
        }
    }

    var appContext: android.content.Context? = null

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingSave: Runnable? = null

    /** Сохранение с задержкой: не пишем на диск на каждое нажатие клавиши. */
    fun save() {
        pendingSave?.let { mainHandler.removeCallbacks(it) }
        val task = Runnable {
            writePrefs()
            appContext?.let { ctx -> LeatherWidget.refresh(ctx) }
        }
        pendingSave = task
        mainHandler.postDelayed(task, 500)
    }

    /** Немедленная запись (выход с экрана, смена проекта). */
    fun saveNow() {
        pendingSave?.let { mainHandler.removeCallbacks(it) }
        pendingSave = null
        writePrefs()
        appContext?.let { ctx -> LeatherWidget.refresh(ctx) }
    }

    private fun writePrefs() {
        prefs.edit()
            .putInt("tab", tab)
            .putString("product", product)
            .putBoolean("allowRotate", allowRotate)
            .putString("sheetW", sheetW)
            .putString("sheetH", sheetH)
            .putString("mode", if (mode == PurchaseMode.PIECE) "PIECE" else "AREA")
            .putString("hideKind", hideKind)
            .putString("hideArea", hideArea)
            .putString("hidePrice", hidePrice)
            .putString("yieldPct", yieldPct)
            .putString("areaPrice", areaPrice)
            .putString("seamCm", seamCm)
            .putString("layers", layers)
            .putString("layerMm", layerMm)
            .putString("stepMm", stepMm)
            .putString("allowanceCm", allowanceCm)
            .putString("spoolM", spoolM)
            .putString("hardware", hardware)
            .putString("consumablesPct", consumablesPct)
            .putString("hours", hours)
            .putString("rate", rate)
            .putString("defectPct", defectPct)
            .putString("overheadPct", overheadPct)
            .putString("markupPct", markupPct)
            .putString("oz", oz)
            .putString("mm", mm)
            .putString("w_area", fmt(requiredArea, 1))
            .putString("w_cost", money(leatherCost))
            .putString("pieces", pieces.joinToString("\n") {
                listOf(it.name, it.kind.name, it.w, it.h, it.qty, it.contour).joinToString("|")
            })
            .putString("defects", defects.joinToString("\n") { "${fmt(it.x.toDouble(), 0)}|${fmt(it.y.toDouble(), 0)}|${fmt(it.r.toDouble(), 0)}" })
            .putString("myK", myK)
            .putString("cur", cur)
            .putInt("themeMode", themeMode)
            .putString("langMode", langMode)
            .putString("docLang", docLang)
            .putBoolean("onboarded", onboardingDone)
            .putInt("wizardStep", wizardStep)
            .putString("paramModelId", paramModelId)
            .putString("paramValues", paramValues.entries.joinToString("\n") { "${it.key}=${it.value}" })
            .putLong("timerStart", timerStart)
            .putInt("lastLesson", lastLesson)
            .putString("order", order)
            .putBoolean("tips", tips)
            .putString("lessonsRead", lessonsRead.joinToString(","))
            .putString("actualDm2", actualDm2)
            .putString("hw", hw.joinToString("\n") { listOf(it.name, it.price, it.qty).joinToString("|") })
            .putString("fit", fitCalc.indices.joinToString("\n") {
                "${fmt(fitCalc[it], 2)}|${fmt(fitActual[it], 2)}"
            })
            .putString("customTemplates", Templates.toJson(customTemplates))
            .putString("stock", stock.joinToString("\n") {
                listOf(it.name, it.area, it.price, it.note).joinToString("|")
            })
            .putString("projName", projName)
            .putString("activeId", activeId)
            .putString("projects", org.json.JSONArray().apply {
                projects.forEach { p ->
                    put(
                        org.json.JSONObject()
                            .put("id", p.id).put("name", p.name)
                            .put("payload", p.payload).put("updatedAt", p.updatedAt)
                    )
                }
            }.toString())
            .apply()
    }

    // ---------- кожа ----------

    fun pieceArea(p: Piece): Double {
        val contour = parseContour(p.contour)
        val areaMm2 = if (contour.size >= 3) contourAreaMm2(contour) else num(p.w) * num(p.h)
        return areaMm2 / 10000.0 * max(0, p.qty.toIntOrNull() ?: 0)
    }

    val piecesArea: Double get() = pieces.sumOf { pieceArea(it) }
    val baseArea: Double get() = pieces.sumOf { pieceArea(it) * it.kind.k }
    val requiredArea: Double get() = baseArea * num(myK).let { if (it <= 0) 1.0 else it }
    val effectiveK: Double get() = if (piecesArea > 0) requiredArea / piecesArea else 0.0

    val usablePerUnit: Double get() = num(hideArea) * num(yieldPct) / 100.0
    val pricePerUseful: Double
        get() = if (mode == PurchaseMode.PIECE) {
            if (usablePerUnit > 0) num(hidePrice) / usablePerUnit else 0.0
        } else num(areaPrice)

    val unitsNeeded: Int
        get() = if (mode == PurchaseMode.PIECE) {
            if (requiredArea <= 0 || usablePerUnit <= 0) 0 else ceil(requiredArea / usablePerUnit).toInt()
        } else 0

    val leftover: Double
        get() = if (mode == PurchaseMode.PIECE) max(0.0, unitsNeeded * usablePerUnit - requiredArea) else 0.0

    val leatherCost: Double
        get() = if (mode == PurchaseMode.PIECE) unitsNeeded * num(hidePrice)
        else requiredArea * num(areaPrice)

    fun screens(): List<Int> {
        val parsed = order.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 0..9 }.distinct()
        val all = (0..9).toList()
        return parsed + all.filter { it !in parsed }
    }

    fun moveScreen(id: Int, delta: Int) {
        val list = screens().toMutableList()
        val i = list.indexOf(id)
        if (i < 0) return
        val j = (i + delta).coerceIn(0, list.size - 1)
        if (i == j) return
        val tmp = list[i]; list[i] = list[j]; list[j] = tmp
        order = list.joinToString(",")
        save()
    }

    fun startTimer() {
        timerStart = System.currentTimeMillis()
        save()
    }

    /** Останавливает таймер и добавляет отработанное время к часам проекта. */
    fun stopTimer(): Double {
        if (timerStart <= 0L) return 0.0
        val elapsedHours = (System.currentTimeMillis() - timerStart) / 3_600_000.0
        val total = num(hours) + elapsedHours
        hours = fmt(total, 1)
        timerStart = 0L
        save()
        return elapsedHours
    }

    fun timerRunning(): Boolean = timerStart > 0L

    fun saveAsTemplate() {
        if (pieces.isEmpty()) return
        customTemplates.add(templateFromProject(projName, pieces))
        save()
    }

    fun removeTemplate(id: String) {
        customTemplates.removeAll { it.id == id }
        save()
    }

    fun applyTemplate(t: ItemTemplate) {
        pieces.addAll(t.pieces)
        save()
    }

    fun amount(v: Double): String = "${money(v)} $cur"

    fun isLessonRead(id: Int): Boolean = id in lessonsRead

    fun toggleLesson(id: Int) {
        if (id in lessonsRead) lessonsRead.remove(id) else lessonsRead.add(id)
        save()
    }

    // ---------- фурнитура ----------

    val hwItemsSum: Double get() = hw.sumOf { num(it.price) * max(1, it.qty.toIntOrNull() ?: 1) }
    val hardwareTotal: Double get() = num(hardware) + hwItemsSum

    // ---------- подстройка коэффициента ----------

    val fitCount: Int get() = fitCalc.size

    val fitAvgRatio: Double
        get() {
            val ratios = fitCalc.indices.mapNotNull { i ->
                val c = fitCalc[i]
                if (c > 0) fitActual[i] / c else null
            }
            return if (ratios.isEmpty()) 0.0 else ratios.average()
        }

    val lastDeviationPct: Double
        get() = if (fitCalc.isNotEmpty() && fitCalc.last() > 0)
            (fitActual.last() / fitCalc.last() - 1) * 100 else 0.0

    // ---------- склад ----------

    val stockArea: Double get() = stock.sumOf { num(it.area) }
    val stockValue: Double get() = stock.sumOf { num(it.price) }
    val stockAvgPrice: Double get() = if (stockArea > 0) stockValue / stockArea else 0.0
    val stockEnough: Boolean get() = stockArea >= requiredArea

    // ---------- деньги ----------

    val consumables: Double get() = leatherCost * num(consumablesPct) / 100
    val labor: Double get() = num(hours) * num(rate)
    val defectsCost: Double get() = (leatherCost + consumables + hardwareTotal) * num(defectPct) / 100
    val overhead: Double
        get() = (leatherCost + consumables + hardwareTotal + labor + defectsCost) * num(overheadPct) / 100

    val totalCost: Double
        get() = leatherCost + consumables + hardwareTotal + labor + defectsCost + overhead
    val price: Double get() = totalCost * (1 + num(markupPct) / 100)
    val profit: Double get() = price - totalCost
    val profitPerHour: Double get() = if (num(hours) > 0) profit / num(hours) else 0.0

    // ---------- шов ----------

    val seamMm: Double get() = num(seamCm) * 10
    val stitches: Int get() = stitchesFor(seamMm, num(stepMm))
    val pack: Double get() = num(layers) * num(layerMm)
    val threadMm: Double get() = threadFor(seamMm, num(stepMm), pack, num(allowanceCm) * 10)
    val ruleMinMm: Double get() = seamMm * 3.5 + num(allowanceCm) * 10
    val ruleMaxMm: Double get() = seamMm * 4.0 + num(allowanceCm) * 10
    val spoolSeams: Int get() = if (threadMm > 0) (num(spoolM) * 1000 / threadMm).toInt() else 0
}
