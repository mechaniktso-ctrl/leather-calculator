package ru.leather.calc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StitchTab(s: AppState) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            SectionTitle(Lang.t("Шов и нить", "Stitch and thread"), hint = "Нить = стежки × (2 × толщина пакета + шаг) + запас. Быстрая проверка: 3,5–4 длины шва.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.seamCm, Lang.t("Длина шва, см", "Seam length, cm"), Modifier.weight(1f)) { s.seamCm = it; s.save() }
                NumField(s.layers, Lang.t("Слоёв", "Layers"), Modifier.weight(1f)) { s.layers = it; s.save() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.layerMm, Lang.t("Слой, мм", "Layer, mm"), Modifier.weight(1f)) { s.layerMm = it; s.save() }
                NumField(s.stepMm, Lang.t("Шаг, мм", "Spacing, mm"), Modifier.weight(1f)) { s.stepMm = it; s.save() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.allowanceCm, Lang.t("Запас, см", "Allowance, cm"), Modifier.weight(1f)) { s.allowanceCm = it; s.save() }
                NumField(s.spoolM, Lang.t("Катушка, м", "Spool, m"), Modifier.weight(1f)) { s.spoolM = it; s.save() }
            }
            Text(
                "Пакет ${fmt(s.pack, 1)} мм = ${fmt(num(s.layers), 0)} × ${fmt(num(s.layerMm), 1)}. " +
                    "Шаг 4 мм ≈ 6 стежков на дюйм.",
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
        }

        GlassCard(tinted = true) {
            Text(
                Lang.t("Результат", "Result"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Aero.accentDeep
            )
            KV(Lang.t("Стежков", "Stitches"), "${s.stitches}")
            KV(Lang.t("Нить на шов", "Thread per seam"), "${fmt(s.threadMm / 1000.0, 2)} м")
            KV(Lang.t("Правило 3,5–4×", "Rule 3.5–4×"), "${fmt(s.ruleMinMm / 1000.0, 2)}–${fmt(s.ruleMaxMm / 1000.0, 2)} м")
            KV(Lang.t("Катушки хватит на", "Spool lasts for"), "${s.spoolSeams} швов")
            KV(Lang.t("На 10 изделий", "For 10 items"), "${fmt(s.threadMm * 10 / 1000.0, 1)} м")
        }

        GlassCard {
            SectionTitle(Lang.t("Толщина кожи", "Leather thickness"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.oz, Lang.t("Унции (oz)", "Ounces (oz)"), Modifier.weight(1f)) {
                    s.oz = it
                    val v = num(it)
                    if (v > 0) s.mm = fmt(v * 0.396875, 2)
                    s.save()
                }
                NumField(s.mm, Lang.t("Миллиметры", "Millimetres"), Modifier.weight(1f)) {
                    s.mm = it
                    val v = num(it)
                    if (v > 0) s.oz = fmt(v / 0.396875, 1)
                    s.save()
                }
            }
            Text(
                "1 oz = 1/64″ = 0,397 мм. Картхолдер 0,8–1,2 мм, сумка 1,2–1,6, ремень 3,2–4,0, ножны 2,4–3,2.",
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}
