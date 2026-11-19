package ru.leather.calc

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.Checkbox
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.mutableStateListOf
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Box
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlin.math.max
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color

@Composable
fun LeatherTab(s: AppState) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var found by remember { mutableStateOf<List<SheetParser.Found>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scanning = true
            try {
                val image = InputImage.fromFilePath(context, uri)
                TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(image)
                    .addOnSuccessListener { result ->
                        val parsed = SheetParser.parse(result.text)
                        scanning = false
                        if (parsed.isEmpty()) {
                            scope.launch {
                                snackbar.showSnackbar(
                                    "На фото не нашлись размеры вида «120×80». Попробуйте ближе и ровнее."
                                )
                            }
                        } else {
                            found = parsed
                        }
                    }
                    .addOnFailureListener {
                        scanning = false
                        scope.launch { snackbar.showSnackbar("Не удалось распознать фото") }
                    }
            } catch (e: Exception) {
                scanning = false
                scope.launch { snackbar.showSnackbar("Не открыть изображение: ${e.message}") }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LeatherContent(s, snackbar, scope, scanning = scanning, onScan = {
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        })
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).padding(12.dp)
        )
    }

    if (found.isNotEmpty()) {
        val picked = remember(found) { mutableStateListOf<Int>().apply { addAll(found.indices) } }
        var working by remember(found) { mutableStateOf(found) }
        AlertDialog(
            onDismissRequest = { found = emptyList() },
            title = { Text(Lang.t("Найдено деталей: ", "Pieces found: ") + found.size) },
            text = {
                Column {
                    Text(
                        Lang.t("Отметьте, что добавить. Размеры взяты из текста на фото.", "Pick what to add. Sizes come from the photo text."),
                        style = MaterialTheme.typography.bodySmall
                    )
                    working.forEachIndexed { i, f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = i in picked,
                                onCheckedChange = { on ->
                                    if (on) picked.add(i) else picked.remove(i)
                                }
                            )
                            NumField(
                                f.w.toString(),
                                Lang.t("Ш, мм", "W, mm"),
                                Modifier.width(78.dp)
                            ) { v ->
                                val n = num(v).toInt()
                                val copy = working.toMutableList()
                                copy[i] = f.copy(w = n)
                                working = copy
                            }
                            Spacer(Modifier.width(4.dp))
                            NumField(
                                f.h.toString(),
                                Lang.t("В, мм", "H, mm"),
                                Modifier.width(78.dp)
                            ) { v ->
                                val n = num(v).toInt()
                                val copy = working.toMutableList()
                                copy[i] = f.copy(h = n)
                                working = copy
                            }
                            Spacer(Modifier.width(4.dp))
                            NumField(
                                f.qty.toString(),
                                Lang.t("шт", "pcs"),
                                Modifier.width(58.dp)
                            ) { v ->
                                val n = (v.toIntOrNull() ?: 1).coerceIn(1, 50)
                                val copy = working.toMutableList()
                                copy[i] = f.copy(qty = n)
                                working = copy
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    picked.sorted().forEach { i ->
                        val f = working[i]
                        s.pieces.add(
                            Piece(
                                "Деталь с фото ${s.pieces.size + 1}",
                                PieceKind.MID,
                                f.w.toString(),
                                f.h.toString(),
                                f.qty.toString()
                            )
                        )
                    }
                    s.save()
                    found = emptyList()
                }) { Text(Lang.t("Добавить", "Add")) }
            },
            dismissButton = {
                TextButton(onClick = { found = emptyList() }) { Text(Lang.t("Отмена", "Cancel")) }
            }
        )
    }
}

@Composable
private fun LeatherContent(
    s: AppState,
    snackbar: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    scanning: Boolean,
    onScan: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroCard(
            title = s.projName,
            subtitle = "Расчёт кожи, стоимости и работы",
            metrics = listOf(
                "Деталей" to "${s.pieces.size}",
                "Нужно" to "${fmt(s.requiredArea, 1)} дм²",
                "Кожа" to "${money(s.leatherCost)} ${s.cur}"
            )
        )

        GlassCard {
            SectionTitle(
                Lang.t("Покупка", "Purchase"),
                hint = "Укажите, как покупаете кожу: целым листом/шкурой или платите за дм². " +
                    "Полезный выход — сколько от площади реально идёт в дело (шкура 75–85 %, лист около 95 %)."
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("Целая шкура/лист", "Whole hide/sheet"), s.mode == PurchaseMode.PIECE) {
                    s.mode = PurchaseMode.PIECE; s.save()
                }
                AeroChip(Lang.t("За дм²", "Per dm²"), s.mode == PurchaseMode.AREA) {
                    s.mode = PurchaseMode.AREA; s.save()
                }
            }
            if (s.mode == PurchaseMode.PIECE) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField(s.hideArea, Lang.t("Площадь, дм²", "Area, dm²"), Modifier.weight(1f)) { s.hideArea = it; s.save() }
                    NumField(s.hidePrice, Lang.t("Цена, ₽", "Price, ₽"), Modifier.weight(1f)) { s.hidePrice = it; s.save() }
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
                Text(
                    "Лист А3 = 420×297 мм ≈ 12,5 дм². Полезный выход шкуры 75–85 %.",
                    style = MaterialTheme.typography.labelSmall, color = Aero.muted
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumField(s.areaPrice, Lang.t("Цена дм², ₽", "Price per dm²"), Modifier.weight(1f)) { s.areaPrice = it; s.save() }
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(Lang.t("Детали кроя", "Pattern pieces"), hint = "Площадь считается по габаритному прямоугольнику детали (Д × Ш), затем умножается на коэффициент типа детали: крупная 1,18, средняя 1,25, мелочь 1,35, лоскут 1,5.")
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AeroChip(if (scanning) "…" else Lang.t("из фото", "from photo"), false) { onScan() }
                AeroChip(Lang.t("+ деталь", "+ piece"), false) {
                    s.pieces.add(Piece("Новая деталь", PieceKind.MID, "", "", "1"))
                    s.save()
                }
            }
        }

        Text(
            Lang.t("Шаблоны изделий", "Item templates"),
            style = MaterialTheme.typography.labelMedium,
            color = Aero.muted
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Templates.builtIn.forEach { t ->
                AeroChip(if (Lang.isEn()) t.titleEn else t.titleRu, false) { s.applyTemplate(t) }
            }
        }

        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AeroChip(Lang.t("＋ сохранить шаблон", "+ save template"), false) { s.saveAsTemplate() }
            s.customTemplates.forEach { t ->
                AeroChip(if (Lang.isEn()) t.titleEn else t.titleRu, true) { s.applyTemplate(t) }
                IconGlass("✕", { s.removeTemplate(t.id) })
            }
        }
        if (s.customTemplates.isNotEmpty()) {
            Text(
                Lang.t("Свои шаблоны сохраняются в приложении.", "Your templates are stored on the device."),
                style = MaterialTheme.typography.labelSmall,
                color = Aero.muted
            )
        }

        s.pieces.forEachIndexed { index, p ->
            key(index) {
                PieceCard(p, onChange = { s.pieces[index] = it; s.save() }, onDelete = {
                    val removed = p
                    val at = index
                    s.pieces.removeAt(index)
                    s.save()
                    scope.launch {
                        val result = snackbar.showSnackbar(
                            "Деталь «${removed.name.ifBlank { "без названия" }}» удалена",
                            actionLabel = "Вернуть",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            s.pieces.add(at.coerceAtMost(s.pieces.size), removed)
                            s.save()
                        }
                    }
                })
            }
        }

        if (s.pieces.isEmpty()) Text(Lang.t("Деталей нет — добавьте первую.", "No pieces yet — add the first one."), color = Aero.muted)

        GlassCard(tinted = true) {
            Text(
                Lang.t("Итого по коже", "Leather total"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Aero.accentDeep
            )
            KV(Lang.t("Площадь деталей", "Pieces area"), "${fmt(s.piecesArea, 2)} дм²")
            KV(Lang.t("Коэффициент раскроя", "Cutting factor"), "×${fmt(s.effectiveK, 2)}")
            KV(Lang.t("Нужно кожи", "Leather needed"), "${fmt(s.requiredArea, 1)} дм²")
            if (s.mode == PurchaseMode.PIECE) {
                KV(Lang.t("Шкур нужно", "Hides needed"), "${s.unitsNeeded} × ${fmt(num(s.hideArea), 0)} дм²")
                KV(Lang.t("Остаток", "Leftover"), "${fmt(s.leftover, 1)} дм²")
            }
            KV(Lang.t("Цена полезного дм²", "Price per usable dm²"), "${s.amount(s.pricePerUseful)}")
            KV(Lang.t("Кожа стоит", "Leather cost"), "${s.amount(s.leatherCost)}", bold = true)
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun KindChip(current: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        AeroChip(current, true, Modifier.fillMaxWidth()) { expanded = true }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            kindPresets.keys.forEach { o ->
                DropdownMenuItem(text = { Text(o) }, onClick = { onChange(o); expanded = false })
            }
        }
    }
}

@Composable
fun PieceCard(p: Piece, onChange: (Piece) -> Unit, onDelete: () -> Unit) {
    val area = num(p.w) * num(p.h) / 10000.0 * max(0, p.qty.toIntOrNull() ?: 0)
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = p.name,
                onValueChange = { onChange(p.copy(name = it)) },
                label = { Text(Lang.t("Название", "Name"), style = MaterialTheme.typography.labelSmall) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xCCFFFFFF),
                    unfocusedContainerColor = Color(0x99FFFFFF),
                    focusedBorderColor = Aero.accent,
                    unfocusedBorderColor = Color(0x80B9D2EC),
                    focusedTextColor = Aero.ink,
                    unfocusedTextColor = Aero.ink
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            IconGlass("✕", onDelete)
        }
        PieceKind.entries.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pair.forEach { k ->
                    AeroChip("${k.label} ×${fmt(k.k, 2)}", p.kind == k) { onChange(p.copy(kind = k)) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(p.w, Lang.t("Длина, мм", "Length, mm"), Modifier.weight(1f)) { onChange(p.copy(w = it)) }
            NumField(p.h, Lang.t("Ширина, мм", "Width, mm"), Modifier.weight(1f)) { onChange(p.copy(h = it)) }
            NumField(p.qty, Lang.t("шт", "pcs"), Modifier.width(66.dp)) {
                onChange(p.copy(qty = it.filter { c -> c.isDigit() }.take(3)))
            }
        }
        Text(
            "${fmt(area, 2)} дм² · с раскроем ${fmt(area * p.kind.k, 2)} дм²",
            style = MaterialTheme.typography.labelMedium,
            color = Aero.muted
        )
    }
}
