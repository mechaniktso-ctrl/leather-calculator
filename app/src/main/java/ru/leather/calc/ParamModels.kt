package ru.leather.calc

import kotlin.math.roundToInt

/**
 * Параметрические модели: изделие задаётся габаритами, детали считаются по правилам.
 * Как в CAD: меняешь 280×210×85 — получаешь новый комплект лекал.
 */
data class ParamParam(val key: String, val labelRu: String, val labelEn: String, val default: Int)

data class ParamModel(
    val id: String,
    val titleRu: String,
    val titleEn: String,
    val params: List<ParamParam>,
    val build: (Map<String, Int>) -> List<Piece>
)

private fun pc(name: String, kind: PieceKind, w: Int, h: Int, qty: Int = 1): Piece =
    Piece(name, kind, w.toString(), h.toString(), qty.toString())

object ParamModels {

    private val wKey = "w"
    private val hKey = "h"
    private val dKey = "d"

    val all: List<ParamModel> = listOf(
        ParamModel(
            "bag_flap", "Сумка с клапаном", "Bag with flap",
            listOf(
                ParamParam(wKey, "Ширина, мм", "Width, mm", 280),
                ParamParam(hKey, "Высота, мм", "Height, mm", 210),
                ParamParam(dKey, "Глубина, мм", "Depth, mm", 85)
            )
        ) { p ->
            val w = p[wKey] ?: 280
            val h = p[hKey] ?: 210
            val d = p[dKey] ?: 85
            listOf(
                pc("Перед/зад", PieceKind.BIG, w, h, 2),
                pc("Боковина-дно", PieceKind.MID, w + 2 * d, h),
                pc("Клапан", PieceKind.BIG, w, (h * 0.55).roundToInt()),
                pc("Ремень", PieceKind.MID, 1100, 30)
            )
        },
        ParamModel(
            "tote", "Шоппер", "Tote",
            listOf(
                ParamParam(wKey, "Ширина, мм", "Width, mm", 380),
                ParamParam(hKey, "Высота, мм", "Height, mm", 400)
            )
        ) { p ->
            val w = p[wKey] ?: 380
            val h = p[hKey] ?: 400
            listOf(
                pc("Перед/зад", PieceKind.BIG, w, h, 2),
                pc("Дно", PieceKind.MID, w, (h * 0.25).roundToInt()),
                pc("Ручка", PieceKind.MID, (h * 1.6).roundToInt(), 30, 2)
            )
        },
        ParamModel(
            "pouch", "Косметичка", "Pouch",
            listOf(
                ParamParam(wKey, "Ширина, мм", "Width, mm", 200),
                ParamParam(hKey, "Высота, мм", "Height, mm", 120),
                ParamParam(dKey, "Глубина, мм", "Depth, mm", 60)
            )
        ) { p ->
            val w = p[wKey] ?: 200
            val h = p[hKey] ?: 120
            val d = p[dKey] ?: 60
            listOf(
                pc("Перед/зад", PieceKind.BIG, w, h, 2),
                pc("Боковина-дно", PieceKind.MID, w + 2 * d, h),
                pc("Обтачка молнии", PieceKind.SMALL, w + 20, 30, 2)
            )
        },
        ParamModel(
            "belt", "Ремень", "Belt",
            listOf(
                ParamParam("waist", "Обхват талии, мм", "Waist, mm", 900),
                ParamParam("bw", "Ширина ремня, мм", "Belt width, mm", 35)
            )
        ) { p ->
            val waist = p["waist"] ?: 900
            val bw = p["bw"] ?: 35
            listOf(pc("Ремень", PieceKind.BIG, waist + 150, bw))
        },
        ParamModel(
            "sheath", "Ножны", "Sheath",
            listOf(
                ParamParam("blade", "Длина клинка, мм", "Blade length, mm", 180),
                ParamParam(dKey, "Ширина клинка, мм", "Blade width, mm", 40)
            )
        ) { p ->
            val blade = p["blade"] ?: 180
            val bw = p[dKey] ?: 40
            listOf(
                pc("Половинка", PieceKind.BIG, blade + 40, bw * 2 + 30, 2),
                pc("Шлёвка", PieceKind.SMALL, 80, 30)
            )
        }
    )

    fun find(id: String): ParamModel? = all.firstOrNull { it.id == id }
}
