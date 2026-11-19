package ru.leather.calc

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MoneyTab(s: AppState) {
    val context = LocalContext.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        GlassCard {
            SectionTitle(Lang.t("Изделие", "Item"))
            OutlinedTextField(
                value = s.product,
                onValueChange = { s.product = it; s.save() },
                label = { Text(Lang.t("Название для сметы", "Name for the quote"), style = MaterialTheme.typography.labelSmall) },
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
        }

        GlassCard {
            SectionTitle(Lang.t("Фурнитура", "Hardware"), hint = "Добавьте позиции: кнопки, заклёпки, кольца, молнии. Сумма автоматически попадёт в смету и PDF.")
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HardwareCatalog.items.forEach { item ->
                    AeroChip("${HardwareCatalog.title(item)} · ${item.price}₽", false) {
                        s.hw.add(HwItem(HardwareCatalog.title(item), item.price.toString(), "1"))
                        s.save()
                    }
                }
            }
            s.hw.forEachIndexed { index, item ->
                key(index) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = {
                                s.hw[index] = item.copy(name = it); s.save()
                            },
                            label = { Text(Lang.t("Позиция", "Item"), style = MaterialTheme.typography.labelSmall) },
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
                        NumField(item.price, "₽", Modifier.width(76.dp)) {
                            s.hw[index] = item.copy(price = it); s.save()
                        }
                        Spacer(Modifier.width(6.dp))
                        NumField(item.qty, Lang.t("шт", "pcs"), Modifier.width(58.dp)) {
                            s.hw[index] = item.copy(qty = it.filter { c -> c.isDigit() }); s.save()
                        }
                        Spacer(Modifier.width(6.dp))
                        IconGlass("✕", { s.hw.removeAt(index); s.save() })
                    }
                }
            }
            if (s.hw.isEmpty()) {
                Text(Lang.t("Добавьте позиции — сумма попадёт в смету.", "Add items — the sum goes to the quote."), style = MaterialTheme.typography.labelSmall, color = Aero.muted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.hardware, Lang.t("Прочее, ₽", "Other, ₽"), Modifier.weight(1f)) { s.hardware = it; s.save() }
                Spacer(Modifier.weight(1f))
            }
            KV(Lang.t("Фурнитура всего", "Hardware total"), "${s.amount(s.hardwareTotal)}", bold = true)
        }

        GlassCard {
            SectionTitle(
                Lang.t("Работа", "Work"),
                hint = Lang.t(
                    Lang.t("Засеките время работы: по «Стоп» часы добавятся к затратам и попадут в смету.", "Start the timer: on stop the hours go into costs and the quote."),
                    "Start the timer: on stop the hours go into costs and the quote."
                )
            )
            if (s.timerRunning()) {
                var now by remember { mutableStateOf(System.currentTimeMillis()) }
                LaunchedEffect(s.timerStart) {
                    while (true) {
                        now = System.currentTimeMillis()
                        delay(1000)
                    }
                }
                val elapsed = ((now - s.timerStart) / 1000L)
                val hh = elapsed / 3600
                val mm = (elapsed % 3600) / 60
                val ss = elapsed % 60
                Text(
                    "%02d:%02d:%02d".format(hh, mm, ss),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Aero.accentDeep
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AeroChip(Lang.t("Стоп и в смету", "Stop and add"), true) {
                        s.stopTimer()
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AeroChip(Lang.t("Засечь время", "Start timer"), false) { s.startTimer() }
                }
            }
        }

        GlassCard {
            SectionTitle(Lang.t("Затраты", "Costs"), hint = "Расходники — нить, клей, краска, воск (5–10 % от кожи). Брак — запас на переделку. Накладные — аренда, инструмент, свет.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.consumablesPct, Lang.t("Расходники, %", "Supplies, %"), Modifier.weight(1f)) { s.consumablesPct = it; s.save() }
                NumField(s.hours, Lang.t("Часов", "Hours"), Modifier.weight(1f)) { s.hours = it; s.save() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.rate, Lang.t("Ставка, ₽/ч", "Rate, ₽/h"), Modifier.weight(1f)) { s.rate = it; s.save() }
                NumField(s.defectPct, Lang.t("Брак, %", "Defects, %"), Modifier.weight(1f)) { s.defectPct = it; s.save() }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumField(s.overheadPct, Lang.t("Накладные, %", "Overhead, %"), Modifier.weight(1f)) { s.overheadPct = it; s.save() }
                NumField(s.markupPct, Lang.t("Наценка, %", "Markup, %"), Modifier.weight(1f)) { s.markupPct = it; s.save() }
            }
            Text(
                Lang.t("Расходники — нить, клей, краска, воск: 5–10 % от кожи. Брак 5–10 %. Накладные — аренда, инструмент, свет.", "Supplies — thread, glue, paint, wax: 5–10 % of leather. Defects 5–10 %. Overhead — rent, tools, power."),
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
        }

        GlassCard(tinted = true) {
            Text(
                Lang.t("Смета", "Quote"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Aero.accentDeep
            )
            KV(Lang.t("Кожа", "Leather"), "${s.amount(s.leatherCost)}")
            KV(Lang.t("Фурнитура", "Hardware"), "${s.amount(s.hardwareTotal)}")
            KV(Lang.t("Расходники", "Supplies"), "${s.amount(s.consumables)}")
            KV(Lang.t("Работа (", "Labor (") + "${fmt(num(s.hours), 1)}" + Lang.t(" ч)", " h)"), "${s.amount(s.labor)}")
            KV(Lang.t("Брак", "Defects"), "${s.amount(s.defectsCost)}")
            KV(Lang.t("Накладные", "Overhead"), "${s.amount(s.overhead)}")
            HorizontalDivider(color = Color(0x33A9C6E4))
            KV(Lang.t("Себестоимость", "Cost"), "${s.amount(s.totalCost)}", bold = true)
            KV(Lang.t("Цена (+", "Price (+") + "${fmt(num(s.markupPct), 0)} %)", "${s.amount(s.price)}", bold = true)
            KV(Lang.t("Прибыль", "Profit"), "${s.amount(s.profit)}")
            KV(Lang.t("Прибыль в час", "Profit per hour"), "${s.amount(s.profitPerHour)}/ч")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AeroButton(Lang.t("PDF-смета", "PDF quote")) {
                try {
                    val file = PdfEstimate.build(context, s, s.product)
                    val saved = PdfEstimate.saveToDownloads(context, file)
                    Toast.makeText(
                        context,
                        if (saved != null) Lang.t("Сохранено: ", "Saved: ") + saved else Lang.t("PDF готов, отправляем", "PDF ready, sending"),
                        Toast.LENGTH_LONG
                    ).show()
                    PdfEstimate.share(context, file)
                } catch (e: Exception) {
                    Toast.makeText(context, Lang.t("Не удалось создать PDF: ", "Could not create PDF: ") + e.message, Toast.LENGTH_LONG).show()
                }
            }
            AeroChip(Lang.t("Отправить", "Send"), false) {
                try {
                    val file = PdfEstimate.build(context, s, s.product)
                    PdfEstimate.share(context, file)
                } catch (e: Exception) {
                    Toast.makeText(context, Lang.t("Не удалось: ", "Failed: ") + e.message, Toast.LENGTH_LONG).show()
                }
            }
        }
        Text(
            Lang.t("PDF сохраняется в Download/DSH-Go и сразу открывается окно «Отправить».", "The PDF is saved to Download/DSH-Go and the share sheet opens."),
            style = MaterialTheme.typography.labelSmall, color = Aero.muted
        )
        Spacer(Modifier.height(28.dp))
    }
}
