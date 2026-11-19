package ru.leather.calc

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Пошаговый мастер проекта: от изделия к деталям и смете.
 * Шаги 0..3; шаг −1 означает, что мастер закрыт.
 */
@Composable
fun WizardScreen(s: AppState) {
    val step = s.wizardStep
    val steps = listOf(
        Lang.t("Изделие", "Item"),
        Lang.t("Материал", "Material"),
        Lang.t("Детали", "Pieces"),
        Lang.t("Смета", "Quote")
    )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(tinted = true) {
            Text(
                Lang.t("Новый проект", "New project"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Aero.accentDeep
            )
            Text(
                Lang.t("Шаг ${step + 1} из 4", "Step ${step + 1} of 4"),
                style = MaterialTheme.typography.labelMedium,
                color = Aero.muted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                steps.forEachIndexed { i, title ->
                    Box(
                        Modifier
                            .height(6.dp)
                            .weight(1f)
                            .clip(CircleShape)
                            .background(
                                if (i <= step) Brush.horizontalGradient(listOf(Aero.accentLight, Aero.accent))
                                else Brush.horizontalGradient(listOf(Color(0x33000000), Color(0x22000000)))
                            )
                    )
                }
            }
            Text(
                steps[step.coerceIn(0, 3)],
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Aero.ink
            )
        }

        when (step) {
            0 -> StepItem(s)
            1 -> StepMaterial(s)
            2 -> StepPieces(s)
            else -> StepQuote(s)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (step > 0) {
                AeroChip(Lang.t("Назад", "Back"), false) { s.wizardStep = step - 1; s.save() }
            }
            Spacer(Modifier.weight(1f))
            if (step < 3) {
                AeroButton(Lang.t("Дальше", "Next")) { s.wizardStep = step + 1; s.save() }
            } else {
                AeroButton(Lang.t("Готово", "Done")) {
                    s.wizardStep = -1
                    s.tab = 1
                    s.save()
                }
            }
        }
        AeroChip(Lang.t("Выйти из мастера", "Exit wizard"), false) {
            s.wizardStep = -1
            s.save()
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepItem(s: AppState) {
    GlassCard {
        SectionTitle(Lang.t("Название проекта", "Project name"))
        OutlinedTextFieldLite(
            value = s.projName,
            label = Lang.t("Например: кошелёк для Сергея", "For example: wallet for Sergey"),
            onValue = { s.projName = it; s.save() }
        )
    }
    GlassCard {
        SectionTitle(
            Lang.t("Начните с шаблона", "Start from a template"),
            hint = Lang.t(
                "Шаблон сразу добавит детали с типовыми размерами — их можно поправить на следующем шаге.",
                "A template adds parts with typical sizes — you can adjust them on the next step."
            )
        )
        Templates.builtIn.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pair.forEach { t ->
                    AeroChip(if (Lang.isEn()) t.titleEn else t.titleRu, false) {
                        s.applyTemplate(t)
                        s.wizardStep = 1
                        s.save()
                    }
                }
            }
        }
        AeroChip(Lang.t("Начать с нуля", "Start from scratch"), false) { s.wizardStep = 1; s.save() }
    }

    GlassCard {
        SectionTitle(
            Lang.t("Или постройте по размерам", "Or build by dimensions"),
            hint = Lang.t(
                "Выберите модель и задайте габариты — детали пересчитаются автоматически.",
                "Pick a model and set dimensions — the pieces are recalculated automatically."
            )
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ParamModels.all.forEach { m ->
                AeroChip(if (Lang.isEn()) m.titleEn else m.titleRu, s.paramModelId == m.id) {
                    s.paramModelId = m.id
                    s.save()
                }
            }
        }
        val model = ParamModels.find(s.paramModelId) ?: ParamModels.all.first()
        model.params.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { param ->
                    val value = s.paramValues[param.key] ?: param.default.toString()
                    NumField(
                        value,
                        if (Lang.isEn()) param.labelEn else param.labelRu,
                        Modifier.weight(1f)
                    ) { v ->
                        s.paramValues[param.key] = v.filter { it.isDigit() }.take(4)
                        s.save()
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        val pw = s.paramValues["w"]?.toIntOrNull() ?: 280
        val ph = s.paramValues["h"]?.toIntOrNull() ?: 210
        val pd = s.paramValues["d"]?.toIntOrNull() ?: 85
        IsoBox(pw, ph, pd)
        AeroButton(Lang.t("Построить лекала", "Build pattern")) {
            val params = model.params.associate { it.key to (s.paramValues[it.key]?.toIntOrNull() ?: it.default) }
            s.pieces.clear()
            s.pieces.addAll(model.build(params))
            s.wizardStep = 2
            s.save()
        }
    }
}

@Composable
private fun StepMaterial(s: AppState) {
    GlassCard {
        SectionTitle(Lang.t("Как покупаете кожу", "How you buy leather"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AeroChip(Lang.t("Шкура/лист", "Hide/sheet"), s.mode == PurchaseMode.PIECE) {
                s.mode = PurchaseMode.PIECE; s.save()
            }
            AeroChip(Lang.t("За дм²", "Per dm²"), s.mode == PurchaseMode.AREA) {
                s.mode = PurchaseMode.AREA; s.save()
            }
        }
        if (s.mode == PurchaseMode.PIECE) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.hideArea, Lang.t("Площадь, дм²", "Area, dm²"), Modifier.weight(1f)) { s.hideArea = it; s.save() }
                NumField(s.hidePrice, Lang.t("Цена", "Price"), Modifier.weight(1f)) { s.hidePrice = it; s.save() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.yieldPct, Lang.t("Выход, %", "Usable, %"), Modifier.weight(1f)) { s.yieldPct = it; s.save() }
                KindChip(s.hideKind, Modifier.weight(1f)) { kind ->
                    s.hideKind = kind
                    kindPresets[kind]?.let { (area, y) ->
                        s.hideArea = niceNum(area)
                        s.yieldPct = niceNum(y)
                    }
                    s.save()
                }
            }
        } else {
            NumField(s.areaPrice, Lang.t("Цена дм²", "Price per dm²"), Modifier.fillMaxWidth()) {
                s.areaPrice = it; s.save()
            }
        }
    }
    GlassCard {
        SectionTitle(Lang.t("Лист для раскладки", "Sheet for layout"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(s.sheetW, Lang.t("Ширина, мм", "Width, mm"), Modifier.weight(1f)) { s.sheetW = it; s.save() }
            NumField(s.sheetH, Lang.t("Высота, мм", "Height, mm"), Modifier.weight(1f)) { s.sheetH = it; s.save() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AeroChip("А3", false) { s.sheetW = "420"; s.sheetH = "297"; s.save() }
            AeroChip("А4", false) { s.sheetW = "297"; s.sheetH = "210"; s.save() }
            AeroChip(Lang.t("Шкура", "Hide"), false) { s.sheetW = "900"; s.sheetH = "700"; s.save() }
        }
    }
}

@Composable
private fun StepPieces(s: AppState) {
    GlassCard {
        SectionTitle(Lang.t("Детали кроя", "Pattern pieces"))
        if (s.pieces.isEmpty()) {
            Text(
                Lang.t("Пока пусто — добавьте деталь или вернитесь к шаблону.", "Empty — add a piece or go back to templates."),
                style = MaterialTheme.typography.bodySmall,
                color = Aero.muted
            )
        }
        s.pieces.forEachIndexed { index, p ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(p.name.ifBlank { Lang.t("Деталь", "Piece") }, color = Aero.ink)
                    Text(
                        "${p.w} × ${p.h} " + Lang.t("мм", "mm") + " · ${p.qty} " + Lang.t("шт", "pcs"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Aero.muted
                    )
                }
                IconGlass("✕", { s.pieces.removeAt(index); s.save() })
            }
        }
        AeroChip(Lang.t("+ деталь", "+ piece"), false) {
            s.pieces.add(Piece(Lang.t("Деталь", "Piece"), PieceKind.MID, "", "", "1"))
            s.save()
        }
        KV(Lang.t("Площадь деталей", "Pieces area"), "${fmt(s.piecesArea, 2)} дм²")
        KV(Lang.t("С раскроем", "With cutting"), "${fmt(s.requiredArea, 1)} дм²", bold = true)
    }
}

@Composable
private fun StepQuote(s: AppState) {
    GlassCard {
        SectionTitle(Lang.t("Работа и цена", "Labour and price"))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(s.hours, Lang.t("Часов", "Hours"), Modifier.weight(1f)) { s.hours = it; s.save() }
            NumField(s.rate, Lang.t("Ставка, ₽/ч", "Rate per hour"), Modifier.weight(1f)) { s.rate = it; s.save() }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(s.hardware, Lang.t("Фурнитура", "Hardware"), Modifier.weight(1f)) { s.hardware = it; s.save() }
            NumField(s.markupPct, Lang.t("Наценка, %", "Markup, %"), Modifier.weight(1f)) { s.markupPct = it; s.save() }
        }
    }
    GlassCard(tinted = true) {
        Text(
            Lang.t("Итог", "Total"),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Aero.accentDeep
        )
        KV(Lang.t("Нужно кожи", "Leather needed"), "${fmt(s.requiredArea, 1)} дм²")
        KV(Lang.t("Кожа", "Leather"), s.amount(s.leatherCost))
        KV(Lang.t("Работа", "Labour"), s.amount(s.labor))
        KV(Lang.t("Себестоимость", "Cost"), s.amount(s.totalCost), bold = true)
        KV(Lang.t("Цена", "Price"), s.amount(s.price), bold = true)
    }
}

@Composable
private fun OutlinedTextFieldLite(value: String, label: String, onValue: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValue,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xCCFFFFFF),
            unfocusedContainerColor = Color(0x99FFFFFF),
            focusedBorderColor = Aero.accent,
            unfocusedBorderColor = Color(0x80B9D2EC),
            focusedTextColor = Aero.ink,
            unfocusedTextColor = Aero.ink
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Простой изометрический эскиз изделия по габаритам — визуальный контроль формы. */
@Composable
private fun IsoBox(w: Int, h: Int, d: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (Aero.dark) Color(0xFF16222F) else Color(0xFFF4F8FF))
    ) {
        Canvas(Modifier.fillMaxSize().padding(12.dp)) {
            val maxDim = maxOf(w, h, d).toFloat().coerceAtLeast(1f)
            val k = min(size.width, size.height) / (maxDim * 1.8f)
            val cx = size.width / 2f
            val cy = size.height / 2f
            fun p(x: Float, y: Float, z: Float): Offset =
                Offset(cx + (x - z) * 0.62f * k, cy + ((x + z) * 0.34f - y) * k)

            val wf = w.toFloat(); val hf = h.toFloat(); val df = d.toFloat()
            val front = listOf(
                p(0f, 0f, 0f), p(wf, 0f, 0f), p(wf, hf, 0f), p(0f, hf, 0f)
            )
            val back = listOf(
                p(0f, 0f, df), p(wf, 0f, df), p(wf, hf, df), p(0f, hf, df)
            )
            val stroke = Color(0xFF2F6BD8)
            fun poly(pts: List<Offset>, fill: Color) {
                val path = androidx.compose.ui.graphics.Path()
                pts.forEachIndexed { i, o ->
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                path.close()
                drawPath(path, fill)
                drawPath(path, stroke, style = Stroke(width = 2f))
            }
            poly(front, Color(0x332F6BD8))
            poly(
                listOf(back[0], back[1], front[1], front[0]),
                Color(0x222F6BD8)
            )
            poly(
                listOf(back[1], back[2], front[2], front[1]),
                Color(0x1A2F6BD8)
            )
            poly(back, Color(0x142F6BD8))
        }
    }
}
