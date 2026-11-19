package ru.leather.calc

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

@Composable
fun InventoryTab(s: AppState) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            SectionTitle(Lang.t("Склад остатков", "Stock"), hint = "Лоты — купленные куски кожи. «Списать под проект» вычитает из них площадь, которая нужна текущему изделию.")
            Text(
                Lang.t("Лоты кожи: сколько куплено и сколько осталось. Площадь — в дм², цена — за весь кусок.", "Leather lots: bought and left. Area in dm², price for the whole piece."),
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AeroChip(Lang.t("＋ лот", "+ lot"), false) {
                    s.stock.add(StockItem(Lang.t("Кусок", "Piece"), "10", "400", ""))
                    s.save()
                }
                AeroChip(Lang.t("Списать под проект", "Use for project"), false) {
                    var need = s.requiredArea
                    for (i in s.stock.indices) {
                        if (need <= 0) break
                        val item = s.stock[i]
                        val have = num(item.area)
                        val take = minOf(have, need)
                        val left = have - take
                        need -= take
                        if (left <= 0.05) s.stock.removeAt(i)
                        else s.stock[i] = item.copy(area = fmt(left, 1))
                        if (left > 0.05) break
                    }
                    s.save()
                }
            }
        }

        s.stock.forEachIndexed { index, item ->
            key(index) {
                StockCard(
                    item = item,
                    cur = s.cur,
                    onChange = { s.stock[index] = it; s.save() },
                    onDelete = { s.stock.removeAt(index); s.save() }
                )
            }
        }

        if (s.stock.isEmpty()) {
            Text(Lang.t("Склад пуст — добавьте купленные куски.", "Stock is empty — add the pieces you bought."), color = Aero.muted)
        }

        GlassCard(tinted = true) {
            Text(
                Lang.t("Итоги склада", "Stock totals"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = Aero.accentDeep
            )
            KV(Lang.t("Всего кожи", "Total leather"), "${fmt(s.stockArea, 1)} дм²")
            KV(Lang.t("Стоимость остатка", "Stock value"), "${s.amount(s.stockValue)}")
            KV(Lang.t("Средняя цена дм²", "Average price per dm²"), "${s.amount(s.stockAvgPrice)}")
            KV(Lang.t("Проекту нужно", "Project needs"), "${fmt(s.requiredArea, 1)} дм²")
            KV(
                if (s.stockEnough) Lang.t("Кожи хватает", "Enough leather") else Lang.t("Не хватает", "Not enough"),
                if (s.stockEnough) Lang.t("да", "yes") else "${fmt(s.requiredArea - s.stockArea, 1)} дм²",
                bold = true
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun StockCard(item: StockItem, cur: String, onChange: (StockItem) -> Unit, onDelete: () -> Unit) {
    val perDm = if (num(item.area) > 0) num(item.price) / num(item.area) else 0.0
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = item.name,
                onValueChange = { onChange(item.copy(name = it)) },
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumField(item.area, Lang.t("Остаток, дм²", "Left, dm²"), Modifier.weight(1f)) { onChange(item.copy(area = it)) }
            NumField(item.price, Lang.t("Цена, ₽", "Price"), Modifier.weight(1f)) { onChange(item.copy(price = it)) }
        }
        Text(
            "≈ ${money(perDm)} $cur/дм²",
            style = MaterialTheme.typography.labelMedium, color = Aero.muted
        )
    }
}
