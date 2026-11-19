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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private fun findAct(c: Context): Activity? {
    var cur: Context = c
    while (cur is ContextWrapper) {
        if (cur is Activity) return cur
        cur = cur.baseContext
    }
    return null
}

private fun compose_icon(id: Int): String = AppScreens.icons[id] ?: ""

@Composable
fun SettingsTab(s: AppState) {
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val ok = Backup.importFrom(context, uri, s.prefs)
            Toast.makeText(context, if (ok) "Загружено, перезапускаю" else "Файл не подошёл", Toast.LENGTH_LONG).show()
            if (ok) findAct(context)?.recreate()
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            SectionTitle(Lang.t("Вид", "Look"), hint = Lang.t("Валюта влияет на все суммы в приложении и в PDF-смете.", "Currency affects every amount in the app and in the PDF quote."))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("₽", "$", "€", "₸").forEach { c ->
                    AeroChip(c, s.cur == c) { s.cur = c; s.save() }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(Lang.t("Язык", "Language"), style = MaterialTheme.typography.labelMedium, color = Aero.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("Как в системе", "System"), s.langMode == "system") { s.langMode = "system"; s.save() }
                AeroChip("Русский", s.langMode == "ru") { s.langMode = "ru"; s.save() }
                AeroChip("English", s.langMode == "en") { s.langMode = "en"; s.save() }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                Lang.t("Документы (PDF, CSV)", "Documents (PDF, CSV)"),
                style = MaterialTheme.typography.labelMedium,
                color = Aero.muted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("Как в приложении", "App language"), s.docLang == "auto") { s.docLang = "auto"; s.save() }
                AeroChip("Русский", s.docLang == "ru") { s.docLang = "ru"; s.save() }
                AeroChip("English", s.docLang == "en") { s.docLang = "en"; s.save() }
            }
            Spacer(Modifier.height(6.dp))
            Text(Lang.t("Тема", "Theme"), style = MaterialTheme.typography.labelMedium, color = Aero.muted)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("Светлая", "Light"), s.themeMode == 0) { s.themeMode = 0; s.save() }
                AeroChip(Lang.t("Тёмная", "Dark"), s.themeMode == 1) { s.themeMode = 1; s.save() }
                AeroChip(Lang.t("Системная", "System"), s.themeMode == 2) { s.themeMode = 2; s.save() }
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(
                    Lang.t("Всплывающие подсказки", "Tooltips"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Aero.ink,
                    modifier = Modifier.weight(1f)
                )
                AeroChip(if (s.tips) Lang.t("вкл", "on") else Lang.t("выкл", "off"), s.tips) { s.tips = !s.tips; s.save() }
            }
            Text(
                Lang.t("Подсказки — маленькие «?» рядом с заголовками: нажмите, чтобы прочитать пояснение.", "Tooltips are small “?” next to headings: tap to read the hint."),
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
        }

        GlassCard {
            SectionTitle(
                Lang.t("Меню", "Menu"),
                hint = Lang.t("Первые четыре экрана попадают в нижнюю панель, остальные — в «Ещё». Стрелками меняйте порядок: что выше, то и на главном экране.", "The first four screens go to the bottom bar, the rest to More. Use arrows to reorder: higher means on the main bar.")
            )
            s.screens().forEachIndexed { index, id ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        "${index + 1}. ${compose_icon(id)} ${AppScreens.title(id)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Aero.ink,
                        modifier = Modifier.weight(1f)
                    )
                    AeroChip("↑", false) { s.moveScreen(id, -1) }
                    Spacer(Modifier.width(6.dp))
                    AeroChip("↓", false) { s.moveScreen(id, 1) }
                }
            }
        }

        GlassCard {
            SectionTitle(Lang.t("Экспорт расчётов", "Export calculations"))
            AeroChip(Lang.t("CSV: детали и смета", "CSV: pieces and quote"), true) {
                val path = Export.exportCsv(context, s)
                Toast.makeText(context, path ?: "Не удалось сохранить", Toast.LENGTH_LONG).show()
            }
            Text(
                Lang.t("Файл с параметрами, деталями и итогами текущего проекта плюс сводка по всем проектам. Открывается в Excel и Google Таблицах.", "File with settings, pieces and totals of the current project plus a summary of all projects. Opens in Excel and Google Sheets."),
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
        }

        GlassCard {
            SectionTitle(Lang.t("Данные", "Data"))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("Бэкап", "Backup"), true) {
                    val path = Backup.export(context, s.prefs)
                    Toast.makeText(context, path ?: "Не удалось сохранить", Toast.LENGTH_LONG).show()
                }
                AeroChip(Lang.t("Импорт", "Import"), false) {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                }
            }
            AeroChip(Lang.t("Сбросить всё", "Reset all"), false) { confirmReset = true }
            Text(
                Lang.t("Сброс удалит проекты, склад, фурнитуру и настройки на этом телефоне.", "Reset deletes projects, stock, hardware and settings on this phone."),
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
        }

        val crash = remember { CrashLog.read(context) }
        if (crash != null) {
            GlassCard(tinted = true) {
                SectionTitle("Отчёт о сбое", hint = "Приложение сохранило последнюю ошибку локально. Отправьте её разработчику — данные никуда не уходят без вашего действия.")
                Text(
                    crash.take(400),
                    style = MaterialTheme.typography.bodySmall,
                    color = Aero.ink
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AeroChip("Отправить", true) { CrashLog.share(context) }
                    AeroChip("Удалить", false) { CrashLog.clear(context) }
                }
            }
        }

        GlassCard {
            SectionTitle(Lang.t("О приложении", "About"))
            KV(Lang.t("Версия", "Version"), "1.9")
            Text(
                "Обучающий калькулятор кожи: расход материала, стоимость кожи и работы, " +
                    "раскладка, склад и уроки.",
                style = MaterialTheme.typography.bodySmall, color = Aero.ink
            )
        }

        GlassCard {
            SectionTitle("Фотографии в учебнике")
            Text(
                "Ivan Radic — «Shoemaker stitching leather by hand», «Shoe upper leather being cut», " +
                    "«Leather wallet»; Mirøslav Hristøff — «Leather-worker»; Ralf Steinberger — " +
                    "«Dyeing leather, Fez». Лицензия CC BY 2.0, источник — Openverse / Flickr.",
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
        }
        Spacer(Modifier.height(28.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить всё?") },
            text = { Text("Проекты, склад и настройки будут удалены без возможности отмены.") },
            confirmButton = {
                TextButton(onClick = {
                    s.prefs.edit().clear().apply()
                    confirmReset = false
                    findAct(context)?.recreate()
                }) { Text("Сбросить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Отмена") }
            }
        )
    }
}
