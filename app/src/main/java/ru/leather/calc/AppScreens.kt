package ru.leather.calc

object AppScreens {
    fun title(id: Int): String = when (id) {
        0 -> Lang.t("Проекты", "Projects")
        1 -> Lang.t("Кожа", "Leather")
        2 -> Lang.t("Раскладка", "Layout")
        3 -> Lang.t("Склад", "Stock")
        4 -> Lang.t("Учебник", "Learn")
        5 -> Lang.t("Справка", "Guide")
        6 -> Lang.t("Шов", "Stitch")
        7 -> Lang.t("Деньги", "Costs")
        9 -> Lang.t("Замер шкуры", "Hide measure")
        else -> Lang.t("Настройки", "Settings")
    }

    val icons = mapOf(
        0 to "🗂", 1 to "🧶", 2 to "📐", 3 to "📦",
        4 to "📚", 5 to "📖", 6 to "🪡", 7 to "💰", 8 to "⚙️", 9 to "📏"
    )
}
