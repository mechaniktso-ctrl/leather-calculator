package ru.leather.calc

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun Themed(s: AppState, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val systemEn = java.util.Locale.getDefault().language == "en"
    val code = when (s.langMode) {
        "ru" -> "ru"
        "en" -> "en"
        else -> if (systemEn) "en" else "ru"
    }
    SideEffect {
        Lang.set(code)
        Lang.setDoc(
            when (s.docLang) {
                "ru" -> false
                "en" -> true
                else -> code == "en"
            }
        )
    }
    val dark = when (s.themeMode) {
        1 -> true
        2 -> systemDark
        else -> false
    }
    SideEffect { Aero.dark = dark }
    MaterialTheme(colorScheme = if (dark) Aero.darkScheme() else Aero.lightScheme()) {
        content()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.install(this)
        val prefs = getSharedPreferences("leather_calc", Context.MODE_PRIVATE)
        setContent {
            val state = remember { AppState(prefs).also { it.appContext = applicationContext } }
            Themed(state) {
                CompositionLocalProvider(LocalTips provides state.tips) {
                    AeroBackground {
                        if (state.onboardingDone) CalcApp(state) else OnboardingScreen(state)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalcApp(s: AppState) {
    var moreOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            AeroBottomBar(
                s = s,
                current = s.tab,
                onSelect = { id -> s.tab = id; s.save() },
                onMore = { moreOpen = true }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            GlassHeader(s)
            Box(Modifier.weight(1f)) {
                if (s.wizardStep >= 0) {
                    WizardScreen(s)
                } else when (s.tab) {
                    0 -> ProjectsTab(s)
                    1 -> LeatherTab(s)
                    2 -> LayoutTab(s)
                    3 -> InventoryTab(s)
                    4 -> EducationTab(s)
                    5 -> RefTab(s)
                    6 -> StitchTab(s)
                    7 -> MoneyTab(s)
                    8 -> SettingsTab(s)
                    9 -> HideMeasureScreen(s)
                    else -> SettingsTab(s)
                }
            }
        }
    }

    if (moreOpen) {
        ModalBottomSheet(
            onDismissRequest = { moreOpen = false },
            containerColor = if (Aero.dark) Color(0xFF16283C) else Color(0xFFF2F7FE)
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    Lang.t("Ещё", "More"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Aero.ink
                )
                Spacer(Modifier.height(8.dp))
                s.screens().drop(4).forEach { id ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                s.tab = id
                                s.save()
                                moreOpen = false
                            }
                            .padding(vertical = 14.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            "${AppScreens.icons[id]} ${AppScreens.title(id)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Aero.ink
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun AeroBottomBar(s: AppState, current: Int, onSelect: (Int) -> Unit, onMore: () -> Unit) {
    val items = s.screens().take(4).map { Triple(it, AppScreens.icons[it] ?: "", AppScreens.title(it)) }
    Box(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(Aero.glassTop, Aero.glassBottom)))
            .border(1.dp, Aero.glassEdge, RoundedCornerShape(22.dp))
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            items.forEach { (id, icon, label) ->
                NavCell(icon, label, current == id, Modifier.weight(1f)) { onSelect(id) }
            }
            NavCell("☰", Lang.t("Ещё", "More"), current !in s.screens().take(4), Modifier.weight(1f)) { onMore() }
        }
    }
}

@Composable
private fun NavCell(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (selected) Brush.verticalGradient(listOf(Aero.accentLight, Aero.accent))
                    else Brush.verticalGradient(listOf(Aero.glassTop, Aero.glassBottom))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Aero.accentDeep else Aero.muted,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
fun GlassHeader(s: AppState) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                Lang.t("Калькулятор кожи", "Leather Calculator"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Aero.ink
            )
            Text(
                "${s.projName} · ${fmt(s.requiredArea, 1)} дм²",
                style = MaterialTheme.typography.labelMedium,
                color = Aero.muted
            )
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Brush.verticalGradient(listOf(Aero.accentLight, Aero.accentDeep)))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("v1.19", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}
