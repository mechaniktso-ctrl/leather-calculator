package ru.leather.calc

import kotlin.math.abs

/** Геометрия для замера площади по фото. Все координаты — пиксели снимка. */
object AreaMath {

    /** Площадь многоугольника по формуле шнуровки, в пикселях². */
    fun polygonAreaPx(points: List<Pair<Float, Float>>): Double {
        if (points.size < 3) return 0.0
        var sum = 0.0
        for (i in points.indices) {
            val (x1, y1) = points[i]
            val (x2, y2) = points[(i + 1) % points.size]
            sum += x1.toDouble() * y2 - x2.toDouble() * y1
        }
        return abs(sum) / 2.0
    }

    /**
     * Площадь контура в дм².
     * @param referencePx длина эталонного отрезка на фото в пикселях
     * @param referenceMm его реальная длина в миллиметрах
     */
    fun areaDm2(
        points: List<Pair<Float, Float>>,
        referencePx: Double,
        referenceMm: Double
    ): Double {
        if (referencePx <= 0.0 || referenceMm <= 0.0) return 0.0
        val pxPerMm = referencePx / referenceMm
        val mm2 = polygonAreaPx(points) / (pxPerMm * pxPerMm)
        return mm2 / 10000.0
    }

    fun distancePx(a: Pair<Float, Float>, b: Pair<Float, Float>): Double {
        val dx = (a.first - b.first).toDouble()
        val dy = (a.second - b.second).toDouble()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
