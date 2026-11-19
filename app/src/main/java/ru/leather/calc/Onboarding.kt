package ru.leather.calc

import androidx.compose.foundation.background
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

@Composable
fun OnboardingScreen(s: AppState) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            Lang.t("Калькулятор кожи", "Leather Calculator"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Aero.ink
        )
        Text(
            Lang.t(
                "Считает кожу, работу и цену изделия. Учит кроить и шить. Работает офлайн.",
                "Counts leather, labour and price. Teaches cutting and stitching. Works offline."
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = Aero.muted
        )

        StepCard(
            number = "1",
            title = Lang.t("Добавьте детали", "Add your pieces"),
            text = Lang.t(
                "Вручную, из шаблона или с фото выкройки — размеры распознаются автоматически.",
                "By hand, from a template, or from a pattern photo — sizes are read automatically."
            )
        )
        StepCard(
            number = "2",
            title = Lang.t("Узнайте расход", "Get the leather needed"),
            text = Lang.t(
                "Коэффициенты раскроя, полезный выход шкуры и раскладка на листе.",
                "Cutting factors, usable hide yield and layout on a sheet."
            )
        )
        StepCard(
            number = "3",
            title = Lang.t("Посчитайте цену", "Price the item"),
            text = Lang.t(
                "Фурнитура, расходники, часы работы, брак и наценка — смета и PDF для клиента.",
                "Hardware, supplies, hours, defects and markup — quote and PDF for the client."
            )
        )
        StepCard(
            number = "4",
            title = Lang.t("Учитесь по ходу", "Learn as you go"),
            text = Lang.t(
                "15 уроков с фотографиями и схемами: от выбора кожи до торцов и тиснения.",
                "15 lessons with photos and diagrams: from picking leather to edges and tooling."
            )
        )

        Spacer(Modifier.height(8.dp))
        AeroButton(
            Lang.t("Начать", "Start"),
            Modifier.fillMaxWidth()
        ) {
            s.onboardingDone = true
            s.save()
        }
        Text(
            Lang.t(
                "Всё хранится на телефоне. Экспорт и бэкап — в настройках.",
                "Everything stays on the phone. Export and backup are in Settings."
            ),
            style = MaterialTheme.typography.labelSmall,
            color = Aero.muted
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepCard(number: String, title: String, text: String) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Aero.accentLight, Aero.accent))),
                contentAlignment = Alignment.Center
            ) {
                Text(number, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Aero.ink)
                Text(text, style = MaterialTheme.typography.bodySmall, color = Aero.ink)
            }
        }
    }
}
