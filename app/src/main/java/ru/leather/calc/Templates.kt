package ru.leather.calc

import org.json.JSONArray
import org.json.JSONObject

data class ItemTemplate(
    val id: String,
    val titleRu: String,
    val titleEn: String,
    val noteRu: String,
    val noteEn: String,
    val pieces: List<Piece>,
    val custom: Boolean = false
)

/**
 * Библиотека заготовок типовых изделий и пользовательские шаблоны.
 * Размеры — ширина × высота в миллиметрах, без припусков.
 */
object Templates {

    private fun p(name: String, kind: PieceKind, w: Int, h: Int, qty: Int = 1) =
        Piece(name, kind, w.toString(), h.toString(), qty.toString())

    val builtIn: List<ItemTemplate> = listOf(
        ItemTemplate(
            "cardholder", "Картхолдер", "Card holder",
            "2 кармана + спинка, 105×75, кожа 1,1–1,4 мм",
            "2 pockets + back, 105×75, 1.1–1.4 mm",
            listOf(
                p("Карман", PieceKind.BIG, 105, 75, 2),
                p("Спинка", PieceKind.BIG, 105, 75)
            )
        ),
        ItemTemplate(
            "bifold", "Бифолд", "Bifold wallet",
            "Основа 220×95, два кармана 100×60",
            "Body 220×95, two pockets 100×60",
            listOf(
                p("Основа", PieceKind.BIG, 220, 95),
                p("Карман", PieceKind.SMALL, 100, 60, 2),
                p("Подкладка", PieceKind.SMALL, 220, 95)
            )
        ),
        ItemTemplate(
            "longwallet", "Портмоне", "Long wallet",
            "Основа 200×100, карманы 190×70 и 90×70",
            "Body 200×100, pockets 190×70 and 90×70",
            listOf(
                p("Основа", PieceKind.BIG, 200, 100),
                p("Карман большой", PieceKind.MID, 190, 70),
                p("Карман малый", PieceKind.SMALL, 90, 70, 2)
            )
        ),
        ItemTemplate(
            "belt", "Ремень", "Belt",
            "Полоса 1150×35, кожа 3,5–4 мм",
            "Strap 1150×35, 3.5–4 mm",
            listOf(p("Ремень", PieceKind.BIG, 1150, 35))
        ),
        ItemTemplate(
            "sheath", "Ножны", "Sheath",
            "2 половинки 220×90 + шлёвка 80×30",
            "2 halves 220×90 + loop 80×30",
            listOf(
                p("Половинка", PieceKind.BIG, 220, 90, 2),
                p("Шлёвка", PieceKind.SMALL, 80, 30)
            )
        ),
        ItemTemplate(
            "phone", "Чехол на телефон", "Phone sleeve",
            "Основа 160×85 + клапан 160×40",
            "Body 160×85 + flap 160×40",
            listOf(
                p("Основа", PieceKind.BIG, 160, 85),
                p("Клапан", PieceKind.MID, 160, 40)
            )
        ),
        ItemTemplate(
            "tote", "Шоппер", "Tote bag",
            "Перед/зад 380×400, дно 380×100, ручки 600×30 ×2",
            "Front/back 380×400, bottom 380×100, handles 600×30 ×2",
            listOf(
                p("Перед", PieceKind.BIG, 380, 400, 2),
                p("Дно", PieceKind.MID, 380, 100),
                p("Ручка", PieceKind.MID, 600, 30, 2)
            )
        ),
        ItemTemplate(
            "flapbag", "Сумка с клапаном", "Bag with flap",
            "Перед/зад 280×210, боковина 280×85, клапан 280×150, ремень 1100×30",
            "Front/back 280×210, gusset 280×85, flap 280×150, strap 1100×30",
            listOf(
                p("Перед", PieceKind.BIG, 280, 210, 2),
                p("Боковина", PieceKind.MID, 280, 85, 2),
                p("Клапан", PieceKind.BIG, 280, 150),
                p("Ремень", PieceKind.MID, 1100, 30)
            )
        ),
        ItemTemplate(
            "pouch", "Косметичка", "Pouch",
            "Перед/зад 200×120, боковина 200×60, молния 200",
            "Front/back 200×120, gusset 200×60, zipper 200",
            listOf(
                p("Перед", PieceKind.BIG, 200, 120, 2),
                p("Боковина", PieceKind.MID, 200, 60, 2),
                p("Обтачка молнии", PieceKind.SMALL, 220, 30, 2)
            )
        ),
        ItemTemplate(
            "passport", "Обложка на паспорт", "Passport cover",
            "Основа 210×140, карман 90×60 ×2",
            "Body 210×140, pockets 90×60 ×2",
            listOf(
                p("Основа", PieceKind.BIG, 210, 140),
                p("Карман", PieceKind.SMALL, 90, 60, 2)
            )
        ),
        ItemTemplate(
            "glasses", "Очечник", "Glasses case",
            "2 детали 170×80, кожа 1,6–2 мм",
            "2 pieces 170×80, 1.6–2 mm",
            listOf(p("Деталь", PieceKind.MID, 170, 80, 2))
        ),
        ItemTemplate(
            "keyfob", "Брелок", "Key fob",
            "Полоса 120×25 + кольцо",
            "Strap 120×25 + ring",
            listOf(p("Полоса", PieceKind.SMALL, 120, 25))
        ),
        ItemTemplate(
            "collar", "Ошейник", "Dog collar",
            "Полоса по обхвату + 50 мм, ширина 25",
            "Strap = neck + 50 mm, width 25",
            listOf(p("Ошейник", PieceKind.MID, 450, 25))
        ),
        ItemTemplate(
            "bracelet", "Браслет", "Bracelet",
            "Полоса 230×20 ×2 слоя",
            "Strap 230×20 ×2 layers",
            listOf(p("Полоса", PieceKind.SMALL, 230, 20, 2))
        ),
        ItemTemplate(
            "watchstrap", "Ремешок часов", "Watch strap",
            "Две части 115×20 и 75×20, кожа 1,2–1,6 мм",
            "Two parts 115×20 and 75×20, 1.2–1.6 mm",
            listOf(
                p("Длинная часть", PieceKind.SMALL, 115, 20),
                p("Короткая часть", PieceKind.SMALL, 75, 20)
            )
        ),
        ItemTemplate(
            "apron", "Фартук", "Apron",
            "Полотно 600×450, лямки 800×25 ×2",
            "Body 600×450, straps 800×25 ×2",
            listOf(
                p("Полотно", PieceKind.BIG, 600, 450),
                p("Лямка", PieceKind.MID, 800, 25, 2)
            )
        )
    )

    fun areaDm2(t: ItemTemplate): Double =
        t.pieces.sumOf { num(it.w) * num(it.h) / 10000.0 * maxOf(0, it.qty.toIntOrNull() ?: 0) }

    // ---------- пользовательские шаблоны ----------

    fun toJson(list: List<ItemTemplate>): String {
        val arr = JSONArray()
        list.forEach { t ->
            val pieces = JSONArray()
            t.pieces.forEach {
                pieces.put(
                    JSONObject()
                        .put("n", it.name).put("k", it.kind.name)
                        .put("w", it.w).put("h", it.h).put("q", it.qty)
                )
            }
            arr.put(
                JSONObject()
                    .put("id", t.id).put("ru", t.titleRu).put("en", t.titleEn)
                    .put("noteRu", t.noteRu).put("noteEn", t.noteEn)
                    .put("pieces", pieces)
            )
        }
        return arr.toString()
    }

    fun fromJson(raw: String?): List<ItemTemplate> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val piecesArr = o.optJSONArray("pieces") ?: JSONArray()
                val pieces = (0 until piecesArr.length()).mapNotNull { j ->
                    val p = piecesArr.getJSONObject(j)
                    val kind = PieceKind.entries.firstOrNull { it.name == p.optString("k") }
                        ?: PieceKind.MID
                    Piece(p.optString("n"), kind, p.optString("w"), p.optString("h"), p.optString("q", "1"))
                }
                if (pieces.isEmpty()) null else ItemTemplate(
                    o.optString("id"), o.optString("ru"), o.optString("en"),
                    o.optString("noteRu"), o.optString("noteEn"), pieces, custom = true
                )
            }
        }.getOrDefault(emptyList())
    }
}

/** Собирает шаблон из деталей текущего проекта. */
fun templateFromProject(nameRu: String, pieces: List<Piece>): ItemTemplate =
    ItemTemplate(
        id = "custom-" + System.currentTimeMillis(),
        titleRu = nameRu.ifBlank { "Мой шаблон" },
        titleEn = nameRu.ifBlank { "My template" },
        noteRu = "Свой шаблон · ${pieces.size} деталей",
        noteEn = "Custom template · ${pieces.size} pieces",
        pieces = pieces.toList(),
        custom = true
    )
