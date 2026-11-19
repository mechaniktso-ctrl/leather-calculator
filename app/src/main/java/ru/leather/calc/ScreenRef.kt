package ru.leather.calc

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun RefTab(s: AppState) {
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val ok = Backup.importFrom(context, uri, s.prefs)
            Toast.makeText(
                context,
                if (ok) "Данные загружены, перезапускаю" else "Файл не подошёл",
                Toast.LENGTH_LONG
            ).show()
            if (ok) context.findActivity()?.recreate()
        }
    }

    val presets = listOf(
        Triple(Lang.t("Картхолдер", "Card holder"), "1,5–2 дм²", "2,5 дм²"),
        Triple(Lang.t("Паспорт", "Passport"), "2,5–3 дм²", "4 дм²"),
        Triple(Lang.t("Бифолд", "Bifold"), "4–6 дм²", "7 дм²"),
        Triple(Lang.t("Портмоне", "Long wallet"), "7–9 дм²", "11 дм²"),
        Triple(Lang.t("Ремень 125 см", "Belt 125 cm"), "5–6 дм²", "7 дм²"),
        Triple(Lang.t("Ножны", "Sheath"), "8–12 дм²", "14 дм²"),
        Triple(Lang.t("Поясная сумка", "Waist bag"), "18–25 дм²", "28 дм²"),
        Triple(Lang.t("Шопер", "Shopper"), "45–60 дм²", "70 дм²"),
        Triple(Lang.t("Рюкзак", "Backpack"), "70–90 дм²", "105 дм²")
    )

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        GlassCard {
            SectionTitle(Lang.t("Сколько кожи уходит", "Leather per item"), hint = "Ориентиры: площадь деталей и с учётом раскроя. Второе число — то, что стоит покупать.")
            Text(
                Lang.t("Площадь деталей и с учётом раскроя", "Pieces area, and with cutting factor"),
                style = MaterialTheme.typography.labelSmall,
                color = Aero.muted
            )
        }
        presets.forEach { (name, area, withCut) ->
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.bodyLarge, color = Aero.ink)
                        Text(area, style = MaterialTheme.typography.labelMedium, color = Aero.muted)
                    }
                    Text(
                        withCut,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = Aero.accentDeep
                    )
                }
            }
        }

        GlassCard {
            SectionTitle(Lang.t("Коэффициенты раскроя", "Cutting factors"))
            KV(Lang.t("Крупные прямые", "Large straight"), "×1,15–1,20")
            KV(Lang.t("Средние со скруглениями", "Medium, rounded"), "×1,20–1,30")
            KV(Lang.t("Мелочёвка", "Small parts"), "×1,30–1,40")
            KV(Lang.t("Лоскут, дефекты", "Scraps, defects"), "×1,50 и выше")
            KV(Lang.t("Полезный выход шкуры", "Usable hide yield"), "75–85 %")
            KV("1 ft²", "9,29 дм²")
            KV(Lang.t("Итальянский ft²", "Italian ft²"), "≈ 9 дм²")
            KV("1 oz", "0,397 мм")
        }

        GlassCard(tinted = true) {
            Text(
                Lang.t("Фактический расход", "Actual usage"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Aero.accentDeep
            )
            KV(Lang.t("Расчёт по проекту", "Project estimate"), "${fmt(s.requiredArea, 1)} дм²")
            KV(Lang.t("Личный коэффициент", "Personal factor"), "×${fmt(num(s.myK), 2)}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.actualDm2, Lang.t("Ушло фактически, дм²", "Actual, dm²"), Modifier.weight(1f)) {
                    s.actualDm2 = it; s.save()
                }
                AeroChip(Lang.t("Записать", "Record"), false) {
                    val actual = num(s.actualDm2)
                    if (actual > 0 && s.requiredArea > 0) {
                        s.fitCalc.add(s.requiredArea)
                        s.fitActual.add(actual)
                        s.actualDm2 = ""
                        s.save()
                    }
                }
            }
            if (s.fitCount > 0) {
                KV(Lang.t("Последнее отклонение", "Last deviation"), "${fmt(s.lastDeviationPct, 0)} %")
                KV(Lang.t("В среднем", "Average"), "×${fmt(s.fitAvgRatio, 2)}")
                KV(Lang.t("Записей", "Entries"), "${s.fitCount}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AeroChip(Lang.t("Подстроить под факт", "Fit to actual"), true) {
                        s.myK = fmt(s.fitAvgRatio, 2)
                        s.save()
                    }
                    AeroChip(Lang.t("Сбросить", "Reset"), false) {
                        s.myK = "1"
                        s.save()
                    }
                }
                Text(Lang.t("История: расчёт → факт", "History: estimate → actual"), style = MaterialTheme.typography.labelSmall, color = Aero.muted)
                s.fitCalc.indices.reversed().take(5).forEach { i ->
                    val dev = if (s.fitCalc[i] > 0) (s.fitActual[i] / s.fitCalc[i] - 1) * 100 else 0.0
                    Text(
                        "${fmt(s.fitCalc[i], 1)} → ${fmt(s.fitActual[i], 1)} дм² (${if (dev >= 0) "+" else ""}${fmt(dev, 0)} %)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Aero.ink
                    )
                }
            } else {
                Text(
                    "Сшейте изделие и впишите, сколько кожи ушло на самом деле — приложение подстроит коэффициент под ваши раскладки.",
                    style = MaterialTheme.typography.labelSmall, color = Aero.muted
                )
            }
        }

        GlassCard {
            SectionTitle(Lang.t("Данные", "Data"))
            Text(
                Lang.t("Проекты, склад, фурнитура и настройки сохраняются в JSON.", "Projects, stock, hardware and settings are saved as JSON."),
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("Экспорт", "Export"), true) {
                    val path = Backup.export(context, s.prefs)
                    Toast.makeText(
                        context,
                        path ?: Lang.t("Не удалось сохранить", "Could not save"),
                        Toast.LENGTH_LONG
                    ).show()
                }
                AeroChip(Lang.t("Импорт", "Import"), false) {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}
