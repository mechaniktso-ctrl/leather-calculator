package ru.leather.calc

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Замер площади шкуры по фото: контур + эталонный отрезок.
 * Считается автоматически, как только есть 3+ точки контура и обе точки эталона.
 */
@Composable
fun HideMeasureScreen(s: AppState) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var points by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var refA by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var refB by remember { mutableStateOf<Pair<Float, Float>?>(null) }
    var refMm by remember { mutableStateOf("297") }
    var mode by remember { mutableStateOf("contour") }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { picked ->
        if (picked != null) {
            points = emptyList(); refA = null; refB = null
            bitmap = runCatching {
                context.contentResolver.openInputStream(picked)?.use { input ->
                    BitmapFactory.decodeStream(input)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    val refPx = if (refA != null && refB != null) AreaMath.distancePx(refA!!, refB!!) else 0.0
    val areaDm2 = remember(points, refPx, refMm) {
        AreaMath.areaDm2(points, refPx, num(refMm))
    }
    val ready = points.size >= 3 && refPx > 0 && num(refMm) > 0

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            Text(
                Lang.t("Замер шкуры по фото", "Hide measurement"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Aero.accentDeep
            )
            Text(
                Lang.t(
                    "1. Сфотографируйте шкуру сверху, положив рядом линейку или лист А4.\n" +
                        "2. Режим «Контур» — обведите шкуру по краю.\n" +
                        "3. Режим «Эталон» — ткните два конца линейки (или стороны листа).\n" +
                        "4. Укажите реальную длину эталона в мм — площадь посчитается сама.",
                    "1. Shoot the hide from above with a ruler or A4 sheet next to it.\n" +
                        "2. Contour mode — outline the hide edge.\n" +
                        "3. Reference mode — tap both ends of the ruler (or sheet side).\n" +
                        "4. Enter the real length in mm — the area is computed automatically."
                ),
                style = MaterialTheme.typography.bodySmall,
                color = Aero.ink
            )
            AeroChip(
                if (bitmap == null) Lang.t("Выбрать фото", "Pick a photo")
                else Lang.t("Другое фото", "Another photo"),
                true
            ) { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        }

        val img = bitmap
        if (img == null) {
            GlassCard {
                Text(
                    Lang.t("Пока нет фото. Выберите снимок, чтобы начать.", "No photo yet. Pick one to start."),
                    color = Aero.muted
                )
            }
            return@Column
        }

        GlassCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("Контур", "Contour"), mode == "contour") { mode = "contour" }
                AeroChip(Lang.t("Эталон", "Reference"), mode == "reference") { mode = "reference" }
                AeroChip(Lang.t("Отменить точку", "Undo point"), false) {
                    if (mode == "contour") {
                        if (points.isNotEmpty()) points = points.dropLast(1)
                    } else {
                        if (refB != null) refB = null else refA = null
                    }
                }
                AeroChip(Lang.t("Очистить", "Clear"), false) {
                    points = emptyList(); refA = null; refB = null
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x22000000))
                    .border(1.dp, Aero.glassEdge, RoundedCornerShape(12.dp))
            ) {
                Image(
                    bitmap = img,
                    contentDescription = Lang.t("Фото шкуры", "Hide photo"),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
                Canvas(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(mode) {
                            detectTapGestures { off ->
                                if (mode == "contour") {
                                    points = points + (off.x to off.y)
                                } else {
                                    when {
                                        refA == null -> refA = off.x to off.y
                                        refB == null -> refB = off.x to off.y
                                        else -> { refA = off.x to off.y; refB = null }
                                    }
                                }
                            }
                        }
                ) {
                    if (points.size >= 2) {
                        for (i in 0 until points.size - 1) {
                            drawLine(
                                Color(0xCC2F6BD8),
                                Offset(points[i].first, points[i].second),
                                Offset(points[i + 1].first, points[i + 1].second),
                                strokeWidth = 3f
                            )
                        }
                        if (points.size >= 3) {
                            drawLine(
                                Color(0xCC2F6BD8),
                                Offset(points.last().first, points.last().second),
                                Offset(points.first().first, points.first().second),
                                strokeWidth = 3f
                            )
                        }
                    }
                    points.forEach {
                        drawCircle(Color.White, 6f, Offset(it.first, it.second))
                        drawCircle(Color(0xFF2F6BD8), 6f, Offset(it.first, it.second), style = Stroke(2f))
                    }
                    if (refA != null && refB != null) {
                        drawLine(
                            Color(0xCCE08A2B),
                            Offset(refA!!.first, refA!!.second),
                            Offset(refB!!.first, refB!!.second),
                            strokeWidth = 4f
                        )
                    }
                    refA?.let { drawCircle(Color(0xFFE08A2B), 7f, Offset(it.first, it.second)) }
                    refB?.let { drawCircle(Color(0xFFE08A2B), 7f, Offset(it.first, it.second)) }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Lang.t("Точек контура", "Contour points"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Aero.muted,
                    modifier = Modifier.weight(1f)
                )
                Text("${points.size}", color = Aero.ink, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Lang.t("Эталон", "Reference"),
                    style = MaterialTheme.typography.bodySmall,
                    color = Aero.muted,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    when {
                        refA == null -> Lang.t("нет", "none")
                        refB == null -> Lang.t("одна точка", "one point")
                        else -> Lang.t("готов", "set")
                    },
                    color = if (refB != null) Aero.accentDeep else Aero.warn,
                    fontWeight = FontWeight.SemiBold
                )
            }

            NumField(refMm, Lang.t("Длина эталона, мм", "Reference length, mm"), Modifier.fillMaxWidth()) {
                refMm = it
            }

            if (!ready) {
                Text(
                    Lang.t(
                        "Нужно: 3+ точки контура и две точки эталона.",
                        "Needed: 3+ contour points and two reference points."
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = Aero.warn
                )
            } else {
                KV(Lang.t("Площадь", "Area"), "${fmt(areaDm2, 2)} дм²", bold = true)
                KV(Lang.t("В сантиметрах", "In cm²"), "${fmt(areaDm2 * 100, 0)} см²")
                AeroButton(Lang.t("Добавить в склад", "Add to stock"), Modifier.fillMaxWidth()) {
                    s.stock.add(
                        StockItem(
                            Lang.t("Шкура с фото", "Hide from photo"),
                            fmt(areaDm2, 1),
                            "",
                            Lang.t("Замер по фото", "Measured from photo")
                        )
                    )
                    s.saveNow()
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
