package ru.leather.calc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

object Aero {
    private val darkState = mutableStateOf(false)
    var dark: Boolean
        get() = darkState.value
        set(v) { darkState.value = v }

    // акценты
    val accent: Color get() = if (dark) Color(0xFF5B9BF5) else Color(0xFF2F6BD8)
    val accentLight: Color get() = if (dark) Color(0xFF7FB2FF) else Color(0xFF4C86E8)
    val accentDeep: Color get() = if (dark) Color(0xFFCCE0FF) else Color(0xFF1B4FA0)
    val ink: Color get() = if (dark) Color(0xFFE9EFF7) else Color(0xFF14243A)
    val muted: Color get() = if (dark) Color(0xFF93A6BC) else Color(0xFF6B7F94)

    // стекло
    val glassTop: Color get() = if (dark) Color(0xFF18232F) else Color(0xFFFFFFFF)
    val glassBottom: Color get() = if (dark) Color(0xFF141E29) else Color(0xFFFFFFFF)
    val glassEdge: Color get() = if (dark) Color(0xFF243342) else Color(0xFFE4EAF2)
    val glassShadow: Color get() = if (dark) Color(0x33000000) else Color(0x0F1B2B44)

    // фон
    val skyTop: Color get() = if (dark) Color(0xFF0D151E) else Color(0xFFF2F5FA)
    val skyBottom: Color get() = if (dark) Color(0xFF111A24) else Color(0xFFFBFCFE)

    // тонированные карточки
    val tintTop: Color get() = if (dark) Color(0xFF16273A) else Color(0xFFF4F8FF)
    val tintBottom: Color get() = if (dark) Color(0xFF13202F) else Color(0xFFEEF4FE)
    val tintEdge: Color get() = if (dark) Color(0xFF253A52) else Color(0xFFD8E6FA)

    val warn: Color get() = if (dark) Color(0xFFE38B6B) else Color(0xFFB4543B)

    fun lightScheme() = lightColorScheme(
        primary = accent, onPrimary = Color.White, secondary = accentLight,
        background = skyBottom, onBackground = ink, surface = glassTop, onSurface = ink,
        surfaceVariant = Color(0x66FFFFFF), onSurfaceVariant = muted, outline = Color(0x66A9C6E4)
    )

    fun darkScheme() = darkColorScheme(
        primary = accent, onPrimary = Color(0xFF06131F), secondary = accentLight,
        background = skyBottom, onBackground = ink, surface = glassTop, onSurface = ink,
        surfaceVariant = Color(0x33FFFFFF), onSurfaceVariant = muted, outline = Color(0x44A9C6E4)
    )
}

@Composable
fun AeroBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Aero.skyTop)
    ) {
        content()
    }
}

@Composable
fun AeroPill(title: String, selected: Boolean, onClick: () -> Unit) {
    val fill = if (selected)
        Brush.verticalGradient(listOf(Aero.accentLight, Aero.accent))
    else
        Brush.verticalGradient(listOf(Aero.glassTop, Aero.glassTop))
    Box(
        Modifier
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, if (selected) Color(0x66FFFFFF) else Aero.glassEdge, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        Text(
            title,
            color = if (selected) Color.White else Aero.ink,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
        )
    }
}

@Composable
fun AeroChip(
    title: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val fill = if (selected)
        Brush.verticalGradient(listOf(Aero.accentLight, Aero.accent))
    else
        Brush.verticalGradient(listOf(if (Aero.dark) Color(0xFF1B2836) else Color(0xFFF1F5FA), if (Aero.dark) Color(0xFF1B2836) else Color(0xFFF1F5FA)))
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .border(1.dp, if (selected) Color(0x55FFFFFF) else Aero.glassEdge, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(
            title,
            color = if (selected) Color.White else Aero.ink,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1
        )
    }
}

@Composable
fun AeroButton(title: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(Aero.accentLight, Aero.accent)))
            .border(1.dp, Color(0x80FFFFFF), RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .semantics { contentDescription = title }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(title, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    tinted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val fill = if (tinted) Aero.tintTop else Aero.glassTop
    val edge = if (tinted) Aero.tintEdge else Aero.glassEdge
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

val LocalTips = compositionLocalOf { true }

/** Маленький «?» с всплывающей подсказкой. */
@Composable
fun HintDot(text: String) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(40.dp)
                .clickable { open = true }
                .semantics { contentDescription = "Подсказка" },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(0x40FFFFFF))
                    .border(1.dp, Color(0x66FFFFFF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("?", style = MaterialTheme.typography.labelSmall, color = Aero.accentDeep)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Text(
                text,
                modifier = Modifier.widthIn(max = 260.dp).padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = Aero.ink
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, hint: String? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Aero.ink)
        if (hint != null && LocalTips.current) HintDot(hint)
    }
}

/** Крупная «витрина» с ключевыми числами. */
@Composable
fun HeroCard(title: String, subtitle: String, metrics: List<Pair<String, String>>) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(Color(0xFF4E93EE), Color(0xFF1F58B4))))
            .border(1.dp, Color(0x66FFFFFF), shape)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = Color(0xCCFFFFFF))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                metrics.forEach { (label, value) ->
                    Column {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xBBFFFFFF))
                        Text(
                            value,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x33FFFFFF), Color(0x00FFFFFF)),
                        startY = 0f,
                        endY = 110f
                    )
                )
        )
    }
}

@Composable
fun AeroProgress(fraction: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(Aero.accentLight, Aero.accent)))
        )
    }
}

@Composable
fun NumField(value: String, label: String, modifier: Modifier = Modifier, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 2) },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xCCFFFFFF),
            unfocusedContainerColor = Color(0x99FFFFFF),
            focusedBorderColor = Aero.accent,
            unfocusedBorderColor = Color(0x80B9D2EC),
            focusedLabelColor = Aero.accent,
            unfocusedLabelColor = Aero.muted,
            cursorColor = Aero.accent,
            focusedTextColor = Aero.ink,
            unfocusedTextColor = Aero.ink
        ),
        modifier = modifier
    )
}

@Composable
fun KV(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Aero.ink)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = if (bold) Aero.accentDeep else Aero.ink
        )
    }
}

@Composable
fun IconGlass(text: String, onClick: () -> Unit, tint: Color = Aero.ink) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Aero.glassTop, Aero.glassBottom)))
            .border(1.dp, Aero.glassEdge, CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = 8.dp)
    ) { Text(text, color = tint) }
}
