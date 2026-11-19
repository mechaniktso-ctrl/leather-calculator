package ru.leather.calc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProjectsTab(s: AppState) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard(tinted = true) {
            Text(
                Lang.t("Текущий проект", "Current project"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Aero.accentDeep
            )
            OutlinedTextField(
                value = s.projName,
                onValueChange = { s.projName = it; s.save() },
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
                modifier = Modifier.fillMaxWidth()
            )
            KV(Lang.t("Деталей", "Pieces"), "${s.pieces.size}")
            KV(Lang.t("Нужно кожи", "Leather needed"), "${fmt(s.requiredArea, 1)} дм²")
            KV(Lang.t("Кожа стоит", "Leather cost"), "${s.amount(s.leatherCost)}")
            KV(Lang.t("Себестоимость", "Cost"), "${s.amount(s.totalCost)}")
            KV(Lang.t("Цена", "Price"), "${s.amount(s.price)}", bold = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("＋ новый", "+ new"), true) {
                    s.newProject()
                    s.wizardStep = 0
                    s.save()
                }
                AeroChip(Lang.t("Копия", "Duplicate"), false) { s.duplicateProject() }
                AeroChip(Lang.t("Удалить", "Delete"), false) { confirmDelete = true }
            }
        }

        SectionTitle(Lang.t("Все проекты", "All projects"), hint = "Переключение не теряет данные: расчёт, склад и настройки сохраняются автоматически.")
        s.projects.sortedByDescending { it.updatedAt }.forEach { p ->
            val active = p.id == s.activeId
            GlassCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            p.name.ifBlank { Lang.t("Без названия", "Untitled") },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) Aero.accentDeep else Aero.ink
                        )
                        Text(
                            (if (active) Lang.t("открыт · ", "open · ") else "") +
                                SimpleDateFormat("dd.MM HH:mm", Locale("ru")).format(Date(p.updatedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = Aero.muted
                        )
                    }
                    if (active) {
                        Text("✓", color = Aero.accent, style = MaterialTheme.typography.titleMedium)
                    } else {
                        AeroChip(Lang.t("Открыть", "Open"), false) { s.switchProject(p.id) }
                    }
                }
            }
        }

        GlassCard {
            SectionTitle(Lang.t("Как это работает", "How it works"))
            Text(
                Lang.t("У каждого проекта свои детали, лист, дефекты, фурнитура, смета и история фактического расхода. Склад остатков общий — он не привязан к изделию.", "Each project has its own pieces, sheet, defects, hardware, quote and usage history. Stock is shared — it belongs to material, not to an item."),
                style = MaterialTheme.typography.bodySmall, color = Aero.ink
            )
        }
        Spacer(Modifier.height(28.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(Lang.t("Удалить проект?", "Delete project?")) },
            text = { Text(Lang.t("Проект и его расчёт будут удалены без возможности отмены.", "The project and its calculation will be deleted. This cannot be undone.")) },
            confirmButton = {
                TextButton(onClick = { s.deleteProject(); confirmDelete = false }) { Text(Lang.t("Удалить", "Delete")) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(Lang.t("Отмена", "Cancel")) }
            }
        )
    }
}
