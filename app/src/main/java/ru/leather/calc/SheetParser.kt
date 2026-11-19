package ru.leather.calc

import kotlin.math.roundToInt

/**
 * Достаёт размеры деталей из текста, распознанного на фото выкройки.
 *
 * Понимает: «120×80», «120 x 80 мм», «120*80», «120 на 80», «120×80×2»,
 * «12,5 × 8 см» (переводит в мм), «Длина 120, ширина 80», «L=270 W=160»,
 * «120 мм × 80 мм», «120/80» не трогает (это не размер).
 */
object SheetParser {

    data class Found(val w: Int, val h: Int, val qty: Int = 1, val source: String = "")

    private val pair = Regex(
        """(\d{1,4}(?:[.,]\d{1,2})?)\s*(мм|mm|см|cm)?\s*[x×*]\s*(\d{1,4}(?:[.,]\d{1,2})?)\s*(мм|mm|см|cm)?(?:\s*[x×*]\s*(\d{1,2}))?"""
    )

    private val labeled = Regex(
        """(длина|ширина|высота|дл|шир|выс|length|width|height|len|wid|hei|\b[LlWwHh]\b)\s*[:=]?\s*(\d{1,4}(?:[.,]\d{1,2})?)\s*(мм|mm|см|cm)?""",
        RegexOption.IGNORE_CASE
    )

    fun parse(text: String): List<Found> {
        val normalized = text
            .replace('Х', 'x').replace('х', 'x').replace('×', 'x').replace('*', 'x')
            .replace(" на ", " x ")
            .replace('\n', ' ')

        val result = LinkedHashSet<Found>()

        // 1. Пары «120×80», «12,5 x 8 см»
        for (m in pair.findAll(normalized)) {
            // «12,5 × 8 см» — единица справа относится к обеим сторонам
            val unit = when {
                m.groupValues[2].isNotBlank() -> m.groupValues[2]
                m.groupValues[4].isNotBlank() -> m.groupValues[4]
                else -> ""
            }
            val a = toMm(m.groupValues[1], unit) ?: continue
            val b = toMm(m.groupValues[3], unit) ?: continue
            val q = m.groupValues[5].toIntOrNull() ?: 1
            add(result, a, b, q, m.value.trim())
        }

        // 2. Подписанные одиночные числа: «Длина 120», «Ширина 80»
        val labels = labeled.findAll(normalized).toList()
        if (labels.size >= 2) {
            var pending: Double? = null
            for (m in labels) {
                val isWidth = m.groupValues[1].lowercase().startsWith("шир") ||
                    m.groupValues[1].lowercase().startsWith("wid") ||
                    m.groupValues[1].lowercase() == "w"
                val value = toMm(m.groupValues[2], m.groupValues[3]) ?: continue
                if (pending == null) {
                    pending = value
                } else {
                    val first = pending
                    val (w, h) = if (isWidth) minOf(first, value) to maxOf(first, value)
                    else minOf(first, value) to maxOf(first, value)
                    add(result, w, h, 1, "$first / $value")
                    pending = null
                }
            }
        }

        return result.toList()
    }

    private fun add(target: MutableSet<Found>, a: Double, b: Double, qty: Int, source: String) {
        val w = minOf(a, b)
        val h = maxOf(a, b)
        if (w < 15 || h > 4000) return
        target.add(Found(w.roundToInt(), h.roundToInt(), qty.coerceIn(1, 50), source))
    }

    private fun toMm(raw: String, unit: String): Double? {
        val v = raw.replace(',', '.').toDoubleOrNull() ?: return null
        return when (unit.lowercase()) {
            "см", "cm" -> v * 10.0
            else -> v
        }
    }

    fun hasUnits(text: String): Boolean =
        Regex("""\d\s*(мм|mm|см|cm)""", RegexOption.IGNORE_CASE).containsMatchIn(text)
}
