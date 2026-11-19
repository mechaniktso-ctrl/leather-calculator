package ru.leather.calc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.widget.Toast
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

private fun clamp(v: Float, lo: Float, hi: Float): Float = if (hi <= lo) lo else max(lo, min(hi, v))

@Composable
fun LayoutTab(s: AppState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snapshot = s.pieces.toList()
    val sheetW = num(s.sheetW).toFloat().let { if (it <= 0f) 420f else it }
    val sheetH = num(s.sheetH).toFloat().let { if (it <= 0f) 297f else it }

    val rects = remember(snapshot) {
        snapshot.flatMap { p ->
            val qty = max(0, p.qty.toIntOrNull() ?: 0)
            val w = num(p.w).toFloat()
            val h = num(p.h).toFloat()
            if (w <= 0f || h <= 0f) emptyList()
            else List(qty) { PRect(w, h, p.name.ifBlank { "Деталь" }, p.contour) }
        }
    }

    var tick by remember { mutableIntStateOf(0) }
    val auto = remember(rects, sheetW, sheetH, s.allowRotate, tick) {
        packSheet(rects, sheetW, sheetH, s.allowRotate)
    }
    val positions = remember(auto) {
        mutableStateListOf<PPlaced>().apply { addAll(auto.placed) }
    }

    val sheetArea = sheetW.toDouble() * sheetH.toDouble()
    val usedNow = positions.sumOf { (it.w * it.h).toDouble() }
    val fill = if (sheetArea > 0) usedNow / sheetArea * 100 else 0.0
    val sheets = remember(rects, sheetW, sheetH, s.allowRotate) {
        packAll(rects, sheetW, sheetH, s.allowRotate)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            SectionTitle(Lang.t("Лист", "Sheet"), hint = "Размер листа, на котором раскладываете детали. Автораскладка ставит крупные детали первыми, мелкие — в промежутки.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.sheetW, Lang.t("Ширина, мм", "Width, mm"), Modifier.weight(1f)) { s.sheetW = it; s.save() }
                NumField(s.sheetH, Lang.t("Высота, мм", "Height, mm"), Modifier.weight(1f)) { s.sheetH = it; s.save() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip("А3", false) { s.sheetW = "420"; s.sheetH = "297"; s.save(); tick++ }
                AeroChip("А4", false) { s.sheetW = "297"; s.sheetH = "210"; s.save(); tick++ }
                AeroChip(Lang.t("Шкура", "Hide"), false) { s.sheetW = "900"; s.sheetH = "700"; s.save(); tick++ }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(if (s.allowRotate) Lang.t("Поворот: да", "Rotate: on") else Lang.t("Поворот: нет", "Rotate: off"), s.allowRotate) {
                    s.allowRotate = !s.allowRotate; s.save(); tick++
                }
                AeroChip(Lang.t("Автораскладка", "Auto layout"), false) { tick++ }
            }
        }

        GlassCard(tinted = true) {
            Text(
                Lang.t("Раскладка", "Layout"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Aero.accentDeep
            )
            SheetCanvas(
                s = s,
                sheetW = sheetW,
                sheetH = sheetH,
                positions = positions,
                sheetsCount = sheets.size
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("Печать 1:1", "Print 1:1"), false) {
                    try {
                        val file = PdfPieces.build(context, s)
                        val saved = PdfEstimate.saveToDownloads(context, file, "patterns")
                        Toast.makeText(context, saved ?: "PDF", Toast.LENGTH_LONG).show()
                        PdfEstimate.share(context, file)
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "error", Toast.LENGTH_LONG).show()
                    }
                }
                AeroChip("DXF", false) {
                    try {
                        val dxf = DxfExport.build(s)
                        val saved = DxfExport.save(context, dxf)
                        Toast.makeText(context, saved ?: "DXF", Toast.LENGTH_LONG).show()
                        DxfExport.share(context, dxf)
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "error", Toast.LENGTH_LONG).show()
                    }
                }
                AeroChip("SVG", false) {
                    try {
                        val svg = SvgExport.build(s)
                        val saved = SvgExport.save(context, svg)
                        Toast.makeText(context, saved ?: "SVG", Toast.LENGTH_LONG).show()
                        SvgExport.share(context, svg)
                    } catch (e: Exception) {
                        Toast.makeText(context, e.message ?: "error", Toast.LENGTH_LONG).show()
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("＋ дефект", "+ defect"), false) {
                    s.defects.add(Defect(sheetW * 0.7f, sheetH * 0.2f, max(10f, sheetW * 0.04f)))
                    s.save()
                }
                AeroChip(Lang.t("Убрать дефекты", "Clear defects"), false) {
                    s.defects.clear(); s.save()
                }
            }
            KV(Lang.t("Заполнение листа", "Sheet fill"), "${fmt(fill, 0)} %")
            KV(Lang.t("Деталей на листе", "Pieces on sheet"), "${positions.size} из ${rects.size}")
            KV(Lang.t("Свободно", "Free space"), "${fmt((sheetArea - usedNow) / 10000.0, 2)} дм²")
            KV(Lang.t("Листов нужно", "Sheets needed"), "${sheets.size}")
            Text(
                Lang.t("Детали можно перетаскивать пальцем, красные круги — дефекты кожи, их тоже двигают. «Автораскладка» вернёт всё к расчётному варианту.", "Drag pieces with your finger; red circles are hide defects and can be moved too. Auto layout resets to the calculated version."),
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
        }

        GlassCard {
            SectionTitle(Lang.t("Что учесть", "Keep in mind"))
            Text(
                Lang.t("Крупные детали кладутся первыми, мелкие — в промежутки. Детали, которые не должны тянуться, кроят вдоль хребта — это ограничивает раскладку и добавляет отхода.", "Large parts go first, small ones into gaps. Parts that must not stretch are cut along the spine — that limits the layout and adds waste."),
                style = MaterialTheme.typography.bodySmall, color = Aero.ink
            )
            Text(
                Lang.t("Заполнение 60–75 % — норма для шкуры: она неровная, с брюшиной и дефектами. На прямоугольном листе выходит выше, но реальная кожа всегда даёт меньше.", "60–75 % fill is normal for a hide: it is irregular, with belly and defects. A rectangular sheet gives more, but real leather always gives less."),
                style = MaterialTheme.typography.bodySmall, color = Aero.muted
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun SheetCanvas(
    s: AppState,
    sheetW: Float,
    sheetH: Float,
    positions: MutableList<PPlaced>,
    sheetsCount: Int
) {
    var dragPiece by remember { mutableIntStateOf(-1) }
    var dragDefect by remember { mutableIntStateOf(-1) }

    Box(
        Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x33DCECFC))))
            .border(1.dp, Color(0x80FFFFFF), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(sheetW, sheetH) {
                    val scale = min(size.width / sheetW, size.height / sheetH)
                    val ox = (size.width - sheetW * scale) / 2f
                    val oy = (size.height - sheetH * scale) / 2f
                    detectDragGestures(
                        onDragStart = { off ->
                            val mx = (off.x - ox) / scale
                            val my = (off.y - oy) / scale
                            dragDefect = s.defects.indexOfFirst { d ->
                                val dx = mx - d.x
                                val dy = my - d.y
                                dx * dx + dy * dy <= d.r * d.r
                            }
                            dragPiece = if (dragDefect >= 0) -1 else
                                positions.indexOfLast { p ->
                                    mx >= p.x && mx <= p.x + p.w && my >= p.y && my <= p.y + p.h
                                }
                        },
                        onDrag = { change, delta ->
                            change.consume()
                            val dx = delta.x / scale
                            val dy = delta.y / scale
                            if (dragDefect >= 0) {
                                val d = s.defects[dragDefect]
                                s.defects[dragDefect] = d.copy(
                                    x = clamp(d.x + dx, d.r, sheetW - d.r),
                                    y = clamp(d.y + dy, d.r, sheetH - d.r)
                                )
                            } else if (dragPiece in positions.indices) {
                                val p = positions[dragPiece]
                                positions[dragPiece] = p.copy(
                                    x = clamp(p.x + dx, 0f, sheetW - p.w),
                                    y = clamp(p.y + dy, 0f, sheetH - p.h)
                                )
                            }
                        },
                        onDragEnd = {
                            dragPiece = -1
                            dragDefect = -1
                            s.save()
                        }
                    )
                }
        ) {
            val scale = min(size.width / sheetW, size.height / sheetH)
            val sw = sheetW * scale
            val sh = sheetH * scale
            val ox = (size.width - sw) / 2f
            val oy = (size.height - sh) / 2f

            drawRoundRect(
                color = Color(0x66FFFFFF),
                topLeft = Offset(ox, oy),
                size = Size(sw, sh),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color(0x99A9C6E4),
                topLeft = Offset(ox, oy),
                size = Size(sw, sh),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2f)
            )

            val stepMm = num(s.stepMm).toFloat().let { if (it <= 0f) 4f else it }
            val inset = 3f
            positions.forEachIndexed { i, p ->
                val hue = if (i % 2 == 0) Color(0xCC6FA3E0) else Color(0xCC8FBBEA)
                val contour = parseContour(p.contour)
                if (contour.size >= 3) {
                    val path = androidx.compose.ui.graphics.Path()
                    contour.forEachIndexed { k, n ->
                        val px = ox + (p.x + n.first) * scale
                        val py = oy + (p.y + n.second) * scale
                        if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
                    }
                    path.close()
                    drawPath(path, hue)
                    drawPath(path, Color(0xAAFFFFFF), style = Stroke(width = 1.5f))
                }
                if (contour.size < 3) {
                    drawRoundRect(
                        color = hue,
                        topLeft = Offset(ox + p.x * scale, oy + p.y * scale),
                        size = Size(p.w * scale, p.h * scale),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = Color(0xAAFFFFFF),
                        topLeft = Offset(ox + p.x * scale, oy + p.y * scale),
                        size = Size(p.w * scale, p.h * scale),
                        cornerRadius = CornerRadius(4f, 4f),
                        style = Stroke(width = 1.5f)
                    )
                }

                // CAD-вид: линия припуска и отверстия под шов
                val iw = p.w - 2 * inset
                val ih = p.h - 2 * inset
                if (iw > 0f && ih > 0f) {
                    drawRoundRect(
                        color = Color(0xCCFFFFFF),
                        topLeft = Offset(ox + (p.x + inset) * scale, oy + (p.y + inset) * scale),
                        size = Size(iw * scale, ih * scale),
                        cornerRadius = CornerRadius(3f, 3f),
                        style = Stroke(
                            width = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                        )
                    )
                    if (stepMm * scale >= 3.5f) {
                        val r = 1.4f
                        var t = 0f
                        while (t <= iw) {
                            drawCircle(
                                Color(0xCC1A324C), radius = r,
                                center = Offset(ox + (p.x + inset + t) * scale, oy + (p.y + inset) * scale)
                            )
                            drawCircle(
                                Color(0xCC1A324C), radius = r,
                                center = Offset(ox + (p.x + inset + t) * scale, oy + (p.y + inset + ih) * scale)
                            )
                            t += stepMm
                        }
                        var v = stepMm
                        while (v < ih) {
                            drawCircle(
                                Color(0xCC1A324C), radius = r,
                                center = Offset(ox + (p.x + inset) * scale, oy + (p.y + inset + v) * scale)
                            )
                            drawCircle(
                                Color(0xCC1A324C), radius = r,
                                center = Offset(ox + (p.x + inset + iw) * scale, oy + (p.y + inset + v) * scale)
                            )
                            v += stepMm
                        }
                    }
                }
            }

            s.defects.forEach { d ->
                drawCircle(
                    color = Color(0x55C0392B),
                    radius = d.r * scale,
                    center = Offset(ox + d.x * scale, oy + d.y * scale)
                )
                drawCircle(
                    color = Color(0xCC9E2B1B),
                    radius = d.r * scale,
                    center = Offset(ox + d.x * scale, oy + d.y * scale),
                    style = Stroke(width = 2f)
                )
            }
        }
        Text(
            if (sheetsCount > 1) (Lang.t("лист 1 из ", "sheet 1 of ") + sheetsCount) else Lang.t("лист 1", "sheet 1"),
            style = MaterialTheme.typography.labelSmall,
            color = Aero.muted,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}
