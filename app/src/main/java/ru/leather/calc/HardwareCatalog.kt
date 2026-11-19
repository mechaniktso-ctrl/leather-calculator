package ru.leather.calc

/** Каталог типовой фурнитуры с ориентировочными ценами (₽ за штуку). */
data class HwCatalogItem(
    val nameRu: String,
    val nameEn: String,
    val price: Int,
    val sizeRu: String = "",
    val sizeEn: String = ""
)

object HardwareCatalog {
    val items = listOf(
        HwCatalogItem("Кнопка 12 мм", "Snap 12 mm", 18, "12 мм", "12 mm"),
        HwCatalogItem("Кнопка 15 мм", "Snap 15 mm", 25, "15 мм", "15 mm"),
        HwCatalogItem("Магнитная кнопка 18", "Magnetic snap 18", 45, "18 мм", "18 mm"),
        HwCatalogItem("Заклёпка 8 мм", "Rivet 8 mm", 6, "8 мм", "8 mm"),
        HwCatalogItem("Заклёпка 9 мм", "Rivet 9 mm", 8, "9 мм", "9 mm"),
        HwCatalogItem("Кольцо D 20", "D-ring 20", 22, "20 мм", "20 mm"),
        HwCatalogItem("Кольцо D 25", "D-ring 25", 26, "25 мм", "25 mm"),
        HwCatalogItem("Полукольцо 25", "Semi-ring 25", 20, "25 мм", "25 mm"),
        HwCatalogItem("Карабин 30", "Swivel hook 30", 60, "30 мм", "30 mm"),
        HwCatalogItem("Молния 20 см", "Zipper 20 cm", 95, "20 см", "20 cm"),
        HwCatalogItem("Молния 60 см", "Zipper 60 cm", 150, "60 см", "60 cm"),
        HwCatalogItem("Люверс 10 мм", "Eyelet 10 mm", 5, "10 мм", "10 mm")
    )

    fun title(item: HwCatalogItem): String = if (Lang.isEn()) item.nameEn else item.nameRu
}
