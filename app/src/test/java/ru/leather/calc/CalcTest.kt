package ru.leather.calc

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalcTest {

    @Test
    fun packing_fits_simple_sheet() {
        val items = listOf(
            PRect(100f, 80f, "a"),
            PRect(100f, 80f, "b"),
            PRect(60f, 60f, "c")
        )
        val sheet = packSheet(items, 420f, 297f, allowRotate = true)
        assertEquals(3, sheet.placed.size)
        assertTrue(sheet.unplaced.isEmpty())
        // ничего не выходит за границы листа
        sheet.placed.forEach {
            assertTrue(it.x >= 0f && it.y >= 0f)
            assertTrue(it.x + it.w <= 420f + 0.01f)
            assertTrue(it.y + it.h <= 297f + 0.01f)
        }
    }

    @Test
    fun packing_splits_into_sheets() {
        val items = List(12) { PRect(300f, 200f, "p$it") }
        val sheets = packAll(items, 420f, 297f, allowRotate = false)
        assertTrue("нужно больше одного листа", sheets.size >= 2)
        val placed = sheets.sumOf { it.placed.size }
        assertEquals(12, placed)
    }

    @Test
    fun packing_keeps_area() {
        val items = listOf(PRect(100f, 50f, "a"), PRect(40f, 40f, "b"))
        val sheet = packSheet(items, 420f, 297f, allowRotate = true)
        val expected = 100f * 50f + 40f * 40f
        assertEquals(expected, sheet.usedArea, 0.5f)
    }

    @Test
    fun stitches_and_thread() {
        assertEquals(75, stitchesFor(300.0, 4.0))
        val t = threadFor(300.0, 4.0, 3.2, 200.0)
        assertEquals(75 * (2 * 3.2 + 4.0) + 200.0, t, 0.01)
    }

    @Test
    fun numbers_parse_with_comma() {
        assertEquals(12.5, num("12,5"), 0.001)
        assertEquals(0.0, num("abc"), 0.001)
        assertEquals("3,2", fmt(3.2, 1))
    }

    @Test
    fun project_stats_from_payload() {
        val payload = JSONObject().apply {
            put("mode", "AREA")
            put("areaPrice", "40")
            put("myK", "1")
            put(
                "pieces",
                org.json.JSONArray().put(
                    JSONObject().put("n", "деталь").put("k", "BIG").put("w", "200").put("h", "200").put("q", "1")
                )
            )
        }.toString()

        val (area, required, cost) = Export.stats(payload)
        assertEquals(4.0, area, 0.001)          // 200×200 мм = 4 дм²
        assertEquals(4.0 * 1.18, required, 0.001)
        assertEquals(4.0 * 1.18 * 40, cost, 0.01)
    }
}

class SheetParserTest {

    @Test
    fun parses_common_dimension_formats() {
        val text = "Передняя панель 120×80 мм; клапан 90 x 45; ремень 300*25"
        val found = SheetParser.parse(text)
        assertEquals(3, found.size)
        assertTrue(found.any { it.w == 80 && it.h == 120 })
        assertTrue(found.any { it.w == 45 && it.h == 90 })
        assertTrue(found.any { it.w == 25 && it.h == 300 })
    }

    @Test
    fun parses_cyrillic_ha() {
        val found = SheetParser.parse("боковина 160 х 40")
        assertEquals(1, found.size)
        assertEquals(40, found.first().w)
        assertEquals(160, found.first().h)
    }

    @Test
    fun parses_quantity_third_number() {
        val found = SheetParser.parse("деталь 120x80x2")
        assertEquals(1, found.size)
        assertEquals(2, found.first().qty)
    }

    @Test
    fun ignores_tiny_and_huge_numbers() {
        assertTrue(SheetParser.parse("5x8").isEmpty())
        assertTrue(SheetParser.parse("5000x5000").isEmpty())
    }

    @Test
    fun deduplicates_same_size() {
        val found = SheetParser.parse("100x50 и ещё 100x50")
        assertEquals(1, found.size)
    }
}

class TemplatesTest {
    @Test
    fun templates_have_positive_area() {
        Templates.builtIn.forEach {
            assertTrue("пустой шаблон ${it.id}", Templates.areaDm2(it) > 0.15)
        }
    }

    @Test
    fun cardholder_area_is_sane() {
        val t = Templates.builtIn.first { it.id == "cardholder" }
        val area = Templates.areaDm2(t)
        assertTrue("площадь $area", area in 1.5..3.5)
    }

    @Test
    fun belt_area_matches_reference() {
        val t = Templates.builtIn.first { it.id == "belt" }
        val area = Templates.areaDm2(t)
        assertTrue("ремень $area", area in 3.5..5.0)
    }
}

class PdfPiecesTest {
    @Test
    fun small_piece_fits_one_page() {
        assertEquals(1, PdfPieces.pagesFor(100f, 100f))
    }

    @Test
    fun long_belt_needs_two_pages() {
        // ремень 1150 мм не влезает в один A4 по высоте/ширине
        assertTrue(PdfPieces.pagesFor(1150f, 35f) >= 2)
    }

    @Test
    fun printable_area_is_about_a4_minus_margins() {
        assertTrue(PdfPieces.printableWmm in 180f..200f)
        assertTrue(PdfPieces.printableHmm in 270f..285f)
    }
}

class SheetParserUnitsTest {
    @Test
    fun centimetres_become_millimetres() {
        val f = SheetParser.parse("12,5 × 8 см")
        assertEquals(1, f.size)
        assertEquals(80, f.first().w)
        assertEquals(125, f.first().h)
    }

    @Test
    fun labelled_length_and_width() {
        val f = SheetParser.parse("Длина 120, ширина 80")
        assertEquals(1, f.size)
        assertEquals(80, f.first().w)
        assertEquals(120, f.first().h)
    }

    @Test
    fun latin_labels() {
        val f = SheetParser.parse("L=270 W=160")
        assertEquals(1, f.size)
        assertEquals(160, f.first().w)
        assertEquals(270, f.first().h)
    }

    @Test
    fun unit_per_side() {
        val f = SheetParser.parse("120 мм x 80 мм")
        assertEquals(1, f.size)
        assertEquals(80, f.first().w)
    }
}

class SvgExportTest {
    @Test
    fun svg_has_mm_units_and_reference_rectangle() {
        val svg = SvgExport.buildFor(listOf(Triple("Деталь", 100f, 50f)))
        assertTrue(svg.contains("mm"))
        assertTrue(svg.contains("width=\"100.0\""))
        assertTrue(svg.contains("Деталь"))
    }

    @Test
    fun svg_skips_empty_sizes() {
        val svg = SvgExport.buildFor(emptyList())
        assertTrue(svg.contains("<svg"))
    }
}

class ParamModelsTest {
    @Test
    fun bag_pieces_follow_dimensions() {
        val model = ParamModels.find("bag_flap")!!
        val pieces = model.build(mapOf("w" to 300, "h" to 200, "d" to 80))
        assertEquals(4, pieces.size)
        val gusset = pieces.first { it.name.startsWith("Боковина") }
        assertEquals("460", gusset.w)   // 300 + 2*80
        assertEquals("200", gusset.h)
        val flap = pieces.first { it.name == "Клапан" }
        assertEquals("110", flap.h)     // 200 * 0.55
    }

    @Test
    fun tote_handles_scale_with_height() {
        val pieces = ParamModels.find("tote")!!.build(mapOf("w" to 400, "h" to 500))
        val handle = pieces.first { it.name == "Ручка" }
        assertEquals("800", handle.w)
        assertEquals("2", handle.qty)
    }

    @Test
    fun belt_length_adds_overlap() {
        val pieces = ParamModels.find("belt")!!.build(mapOf("waist" to 1000, "bw" to 40))
        assertEquals("1150", pieces.first().w)
        assertEquals("40", pieces.first().h)
    }

    @Test
    fun sheath_doubles_width() {
        val pieces = ParamModels.find("sheath")!!.build(mapOf("blade" to 200, "d" to 45))
        assertEquals("120", pieces.first().h) // 45*2 + 30
    }
}

class SvgFormatTest {
    @Test
    fun svg_uses_dot_decimals_only() {
        val svg = SvgExport.buildFor(listOf(Triple("Деталь", 100f, 50f)))
        // в SVG координаты должны быть с точкой, иначе файл невалиден
        assertTrue("есть запятая в координатах", !svg.contains("100,0"))
        assertTrue(svg.contains("100.0"))
        assertTrue(svg.contains("50.0"))
    }
}

class DxfExportTest {
    @Test
    fun dxf_has_sections_and_lines() {
        val dxf = DxfExport.buildFor(listOf(Triple("Деталь", 100f, 50f)))
        assertTrue(dxf.contains("SECTION"))
        assertTrue(dxf.contains("ENTITIES"))
        assertTrue(dxf.contains("LINE"))
        assertTrue(dxf.contains("AC1009"))
        assertTrue(dxf.contains("103.00"))
    }

    @Test
    fun stitch_holes_count() {
        // периметр внутри 3 мм: 94×44 = 276 мм; шаг 4 мм → 69 + 1
        assertEquals(70, DxfExport.stitchHoles(100f, 50f, 4f))
        assertEquals(0, DxfExport.stitchHoles(4f, 4f, 4f))
    }
}

class AreaMathTest {
    @Test
    fun square_area_in_pixels() {
        val sq = listOf(0f to 0f, 100f to 0f, 100f to 100f, 0f to 100f)
        assertEquals(10000.0, AreaMath.polygonAreaPx(sq), 0.001)
    }

    @Test
    fun scale_converts_to_dm2() {
        // 1000 px = 100 мм → 1000×1000 px квадрат = 100×100 мм = 1 дм²
        val sq = listOf(0f to 0f, 1000f to 0f, 1000f to 1000f, 0f to 1000f)
        assertEquals(1.0, AreaMath.areaDm2(sq, 1000.0, 100.0), 0.001)
    }

    @Test
    fun degenerate_polygon_is_zero() {
        assertEquals(0.0, AreaMath.areaDm2(listOf(0f to 0f, 10f to 10f), 100.0, 10.0), 0.001)
    }

    @Test
    fun distance_between_points() {
        assertEquals(5.0, AreaMath.distancePx(0f to 0f, 3f to 4f), 0.001)
    }
}

class ContourTest {
    @Test
    fun contour_parsing_roundtrip() {
        val pts = listOf(0f to 0f, 100f to 0f, 100f to 60f, 0f to 60f)
        val str = contourToString(pts)
        val back = parseContour(str)
        assertEquals(4, back.size)
        assertEquals(100f, back[1].first, 0.1f)
    }

    @Test
    fun contour_area_beats_bounding_box() {
        // треугольник 100×100 имеет площадь 5000 мм², а не 10000
        val tri = listOf(0f to 0f, 100f to 0f, 0f to 100f)
        assertEquals(5000.0, contourAreaMm2(tri), 0.01)
    }

    @Test
    fun svg_uses_polygon_for_contour_piece() {
        val piece = Piece("Клин", PieceKind.BIG, "100", "100", "1", "0,0;100,0;0,100")
        val svg = SvgExport.buildPieces(listOf(piece))
        assertTrue(svg.contains("<polygon"))
        assertTrue(svg.contains("3.0 12.0"))  // контур смещён на припуск и зазор
    }
}
