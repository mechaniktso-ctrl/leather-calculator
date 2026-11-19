package ru.leather.calc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class Lesson(
    val id: Int,
    val module: String,
    val title: String,
    val subtitle: String,
    val minutes: Int,
    val photo: Int? = null,
    val titleEn: String = "",
    val subtitleEn: String = ""
)

val lessons = listOf(
    Lesson(0, "Материал", "Виды кожи и выделка", "Растительная, хромовая, спилок", 4, R.drawable.edu_tannery, "Leather types and tannage", "Veg tan, chrome, split"),
    Lesson(1, "Материал", "Зоны шкуры", "Где хребет, где брюшина", 3, titleEn = "Hide zones", subtitleEn = "Spine vs belly"),
    Lesson(2, "Материал", "Толщина и вес", "Таблица под изделие, oz и мм", 4, titleEn = "Thickness and weight", subtitleEn = "Table by item, oz and mm"),
    Lesson(3, "Инструмент и раскрой", "Инструмент новичка", "Что купить на старте", 4, R.drawable.edu_tools2, "Starter tools", "What to buy first"),
    Lesson(4, "Инструмент и раскрой", "Заточка и уход", "Нож, пробойник, иглы", 3, titleEn = "Sharpening and care", subtitleEn = "Knife, iron, needles"),
    Lesson(5, "Инструмент и раскрой", "Лекала и раскрой", "Масштаб, припуск, рез", 5, R.drawable.edu_cut, "Patterns and cutting", "Scale, allowance, cut"),
    Lesson(6, "Инструмент и раскрой", "Припуски и направление тяги", "Почему детали ведут себя по-разному", 4, titleEn = "Allowance and grain", subtitleEn = "Why parts behave differently"),
    Lesson(7, "Швы и отделка", "Седельный шов", "Две иглы, нить, шаг", 6, R.drawable.edu_stitch, "Saddle stitch", "Two needles, thread, spacing"),
    Lesson(8, "Швы и отделка", "Плетёный шов", "Обработка края косичкой", 4, titleEn = "Buckstitch", subtitleEn = "Braided edge"),
    Lesson(9, "Швы и отделка", "Торцы", "Шлифовка, краска, воск", 4, R.drawable.edu_edge2, "Edges", "Sand, paint, wax"),
    Lesson(10, "Швы и отделка", "Фурнитура", "Кнопки, заклёпки, кольца", 4, R.drawable.edu_belt, "Hardware", "Snaps, rivets, rings"),
    Lesson(11, "Изделие и деньги", "Первый проект и расчёт", "Картхолдер за вечер", 6, R.drawable.edu_wallet, "First project and price", "Card holder in an evening"),
    Lesson(12, "Декор и форма", "Тиснение", "Шеридан, штаммы, кожура", 6, titleEn = "Tooling", subtitleEn = "Sheridan, stamps, casing"),
    Lesson(13, "Декор и форма", "Формовка", "Мокрая кожа держит форму", 5, titleEn = "Molding", subtitleEn = "Wet leather keeps shape"),
    Lesson(14, "Декор и форма", "Плетение", "Косичка, кнут, ручки", 5, titleEn = "Braiding", subtitleEn = "Braid, whip, handles")
)

@Composable
fun EducationTab(s: AppState) {
    var expanded by remember { mutableStateOf(s.lastLesson) }
    var query by remember { mutableStateOf("") }

    val visible = remember(query) {
        if (query.isBlank()) lessons
        else lessons.filter {
            listOf(it.title, it.subtitle, it.module).any { f -> f.contains(query, ignoreCase = true) }
        }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HeroCard(
            title = "Учебник кожевника",
            subtitle = "Короткие уроки с цифрами и схемами — без воды",
            metrics = listOf(
                "Пройдено" to "${s.lessonsRead.size} из ${lessons.size}",
                "Чтения" to "~35 мин"
            )
        )
        AeroProgress(s.lessonsRead.size.toFloat() / lessons.size)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Badge("🌱", Lang.t("Первый урок", "First lesson"), s.lessonsRead.isNotEmpty())
            Badge("🔥", Lang.t("5 уроков", "5 lessons"), s.lessonsRead.size >= 5)
            Badge("🏆", Lang.t("Все уроки", "All lessons"), s.lessonsRead.size >= lessons.size)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text(Lang.t("Поиск по урокам", "Search lessons")) },
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

        if (visible.isEmpty()) {
            Text(Lang.t("Ничего не нашлось", "Nothing found"), color = Aero.muted)
        }

        var lastModule = ""
        visible.forEach { lesson ->
            if (lesson.module != lastModule) {
                lastModule = lesson.module
                Text(
                    when (lesson.module) {
                        "Материал" -> "MATERIAL"
                        "Инструмент и раскрой" -> "TOOLS & CUTTING"
                        "Швы и отделка" -> "STITCHES & FINISH"
                        "Изделие и деньги" -> "ITEM & MONEY"
                        else -> "DECOR & SHAPE"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Aero.accentDeep,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                )
            }
            LessonCard(
                lesson = lesson,
                expanded = expanded == lesson.id,
                read = s.isLessonRead(lesson.id),
                onToggle = {
                    expanded = if (expanded == lesson.id) -1 else lesson.id
                    if (expanded >= 0) { s.lastLesson = expanded; s.save() }
                },
                onRead = { s.toggleLesson(lesson.id) }
            ) { LessonBody(lesson.id) }
        }

        GlassCard {
            SectionTitle("Источники")
            Text(
                "NEBEO — расчёт расхода и таблица толщин; Тавро — маркировка кожи; Паблик Кожевника — " +
                    "стоимость изделия и длина нити; Leather Hide Store — выход с раскладки и 25 % отхода; " +
                    "Wikipedia — техники и инструменты.",
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
            Text(
                "Фотографии: Ivan Radic, Mirøslav Hristøff, Ralf Steinberger — CC BY 2.0 (Openverse/Flickr).",
                style = MaterialTheme.typography.labelSmall, color = Aero.muted
            )
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun Badge(icon: String, title: String, unlocked: Boolean) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (unlocked) Color(0x66DCEBFA) else Color(0x22FFFFFF))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            "$icon $title",
            style = MaterialTheme.typography.labelSmall,
            color = if (unlocked) Aero.accentDeep else Aero.muted
        )
    }
}

@Composable
fun LessonCard(
    lesson: Lesson,
    expanded: Boolean,
    read: Boolean,
    onToggle: () -> Unit,
    onRead: () -> Unit,
    content: @Composable () -> Unit
) {
    GlassCard(tinted = expanded) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(
                        if (read) Brush.verticalGradient(listOf(Aero.accentLight, Aero.accent))
                        else Brush.verticalGradient(listOf(Color(0x55FFFFFF), Color(0x33FFFFFF)))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (read) "✓" else "${lesson.id + 1}",
                    color = if (read) Color.White else Aero.accentDeep,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (Lang.isEn() && lesson.titleEn.isNotBlank()) lesson.titleEn else lesson.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Aero.ink
                )
                Text(
                    (if (Lang.isEn() && lesson.subtitleEn.isNotBlank()) lesson.subtitleEn else lesson.subtitle) +
                        " · ${lesson.minutes} " + Lang.t("мин", "min"),
                    style = MaterialTheme.typography.labelSmall,
                    color = Aero.muted
                )
            }
            AeroChip(if (expanded) "свернуть" else "читать", !expanded) { onToggle() }
        }

        if (expanded) {
            lesson.photo?.let { res ->
                Image(
                    painter = painterResource(res),
                    contentDescription = lesson.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
            content()
            AeroChip(if (read) "✓ пройдено" else "Отметить пройденным", read) { onRead() }
        }
    }
}

@Composable
fun LessonBody(id: Int) {
    if (Lang.isEn()) {
        LessonBodyEn(id)
        return
    }
    when (id) {
        0 -> {
            P("Кожа — это выделанная шкура. Способ выделки определяет, как материал держит форму, " +
                "красится и стареет. Начинающему достаточно различать четыре типа.")
            B(
                listOf(
                    "Растительное дубление (РКП, краст): плотная, жёсткая, форму держит сама. Ремни, ножны, чехлы, тиснение.",
                    "Хромовое (юфть, лофт, наппа): мягкое и эластичное — сумки, одежда, подкладка.",
                    "Крейзи хорс: воскованная, быстро набирает патину от царапин — кошельки и сумки.",
                    "Спилок: нижний слой после двоения. Дешевле, но слабее на разрыв — не на ремни и ручки."
                )
            )
            Example("Первый лист: растишка 1,5–1,8 мм — универсально: ремень, чехол, обложка, проба тиснения.")
        }
        1 -> {
            HideDiagram()
            P("Шкура неоднородна: волокно вдоль хребта плотное, к полам рыхлое, брюшина тянется и быстро теряет форму.")
            B(
                listOf(
                    "Хребет и чепрак — силовые детали: ремень, ручка, дно, клапан.",
                    "Полы и вороток — мелкие детали, подкладка, внутренние карманы.",
                    "Брюшина — только скрытые детали, иначе изделие «поплывёт».",
                    "Кромка листа тоньше середины: не ставьте её на ответственные места."
                )
            )
            Example("Один ремень можно выкроить и из полы, но он вытянется за месяц. Из хребта — будет держать годы.")
        }
        2 -> {
            ThicknessDiagram()
            P("Толщину подбирают по изделию, а не по коже. В карточке товара указывают номинал: реальный разбег 0,2–0,4 мм.")
            TableRow("Изделие", "Толщина")
            TableRow("Подкладка, карманы", "0,8–1,2 мм")
            TableRow("Картхолдер, обложка", "1,1–1,4 мм")
            TableRow("Кошелёк, портмоне", "1,4–1,8 мм")
            TableRow("Сумка, рюкзак", "1,6–2,2 мм")
            TableRow("Ножны, кобура", "2,5–4,0 мм")
            TableRow("Ремень", "3,0–4,5 мм")
            Example("«5–6 oz» = 2,0–2,4 мм. Два слоя по 2 мм жёстче монолитных 4 мм и дают чистую изнанку.")
        }
        3 -> {
            B(
                listOf(
                    "Нож: сапожный или скальпель. Режет по линейке, не тянет кожу.",
                    "Пробойник (pricking iron): шаг 3 / 3,85 / 4 / 5 мм. 4 мм ≈ 6 стежков на дюйм.",
                    "Иглы: две тупоконечные № 0–2, нить 0,6–0,8 мм вощёная.",
                    "Молоток шорный или киянка, шило, клей, краска для торцов, воск.",
                    "Струбцины или шовный конь — держат пакет, пока шьёте."
                )
            )
            Example("Стартовый набор «на попробовать»: нож, пробойник 4 мм, 2 иглы, нить, клей — и один лист А4 растишки.")
        }
        4 -> {
            P("Тупой инструмент портит работу сильнее, чем отсутствие навыка: нож мнёт кромку, пробойник рвёт волокно.")
            B(
                listOf(
                    "Нож: правка на кожаном ремне с пастой — 10–15 движений перед каждым раскроем.",
                    "Пробойник: не бейте по твёрдому, чистите от клея, храните в чехле — зубья гнутся.",
                    "Иглы: меняйте, как только острие цепляет кожу; ржавчина оставляет следы на светлой коже.",
                    "Клей: держите закрытым, наносите тонко — излишек проступает через шов."
                )
            )
        }
        5 -> {
            LayoutDiagram()
            P("Лекала рисуют в масштабе 1:1 и сразу с припуском — иначе детали не сойдутся.")
            B(
                listOf(
                    "Припуск 3–5 мм на шов, 8–10 мм на подгиб.",
                    "Размечайте с изнанки, режьте по линейке одним движением.",
                    "Крупные детали кладите первыми, мелкие — в промежутки.",
                    "Криволинейные детали удобнее резать от себя, поворачивая кожу, а не нож."
                )
            )
            Example("Картхолдер: 2 детали 100×70 мм, припуск уже включён — итог 2,5 дм² с раскроем.")
        }
        6 -> {
            P("Припуск — это запас на шов и подгиб. Направление тяги — то, как деталь поведёт себя в носке.")
            B(
                listOf(
                    "Детали, которые не должны тянуться, кроят вдоль хребта.",
                    "Ручки и клапаны кроят поперёк — они должны тянуться и не ломаться на сгибе.",
                    "На видимых деталях направление должно совпадать, иначе после носки будет разная посадка.",
                    "Обходите шрамы, укусы и растянутые участки: вокруг дефекта теряется больше площади, чем сам дефект."
                )
            )
            Example("Ремень, выкроенный поперёк, растянется под весом сумки. Вдоль хребта — прослужит годами.")
        }
        7 -> {
            StitchDiagram()
            P("Седельный шов шьют двумя иглами навстречу через одно отверстие: получается замок, " +
                "и шов не распускается, если нить оборвётся в одном месте.")
            B(
                listOf(
                    "Разметка: отступ 3 мм от кромки, шаг 4 мм.",
                    "Пробить отверстия, вдеть нить в две иглы, войти в первое отверстие с двух сторон.",
                    "Каждый стежок укладывайте одинаково: одна нить сверху, другая снизу.",
                    "В конце — два обратных стежка для закрепки."
                )
            )
            Example("Шов 30 см, пакет 3,2 мм, шаг 4 мм → 75 стежков и около 1 м нити.")
        }
        8 -> {
            P("Плетёный шов (buckstitch) — косичка по краю изделия. Он не соединяет детали, а закрывает " +
                "кромку и делает её декоративной.")
            B(
                listOf(
                    "Нужны две иглы и нить длиной 6–7 длин края.",
                    "Шьют через край, попеременно перекидывая нить — получается ровная косичка.",
                    "Хорош на ремнях, ножнах и кошельках с открытым торцом.",
                    "Перед плетением торец обязательно выровнять, иначе косичка ляжет волной."
                )
            )
        }
        9 -> {
            EdgeDiagram()
            P("Торец — то, по чему судят о работе. Он делается в три шага: выровнять, увлажнить, запечатать.")
            B(
                listOf(
                    "Шлифовка 400 → 800 → 1200, плоско, без скругления кромки.",
                    "Слегка увлажнить и отшлифовать ещё раз — волокна встанут ровно.",
                    "Краска: 2–3 тонких слоя, сушка 15–20 минут между слоями.",
                    "Воск: растереть до блеска — классика для растишки."
                )
            )
            Example("Кривой или лохматый торец виден на расстоянии вытянутой руки — это первое, что замечают.")
        }
        10 -> {
            P("Фурнитура держит изделие и часто задаёт стиль. Главное правило: отверстие под ножку " +
                "равно диаметру ножки, не больше.")
            B(
                listOf(
                    "Кнопки и заклёпки ставят через подкладку или шайбу, чтобы не порвать кожу.",
                    "Кольца D и полукольца крепят на шлёвке из той же кожи, а не на тонком ремешке.",
                    "Молния требует обтачки: без неё край «гуляет» и молния волнит.",
                    "Магнитную кнопку прячут под подкладку, иначе она цепляет карты и телефон."
                )
            )
            Example("Дешёвая фурнитура ржавеет от рук за месяц — меняется хуже, чем изнашивается кожа.")
        }
        12 -> {
            CarvingDiagram()
            P("Тиснение (карвинг) — это не резьба, а выжимание влажной кожи инструментом. " +
                "Работает только на растительном дублении: хромовая кожа пружинит и не держит отпечаток.")
            B(
                listOf(
                    "Кожу увлажняют («кейсят») и дают воде впитаться — поверхность должна стать прохладной и матовой.",
                    "Свайвел-нож режет контур, но не насквозь: линия задаёт русло для остальных инструментов.",
                    "Бивелер снимает фаску по одну сторону линии — появляется объём и тень.",
                    "Штаммы ставят с наклоном и постукиванием: силуэт, фон, декоративные элементы.",
                    "Готовую работу сушат, затем тонируют и защищают финишем."
                )
            )
            Example("Стиль Шеридан: цветочный узор, где каждый лепесток — контур ножом, затем бивелер и шейдер.")
        }
        13 -> {
            MoldingDiagram()
            P("Формовка — это когда кожа сама принимает форму предмета. Её вымачивают, натягивают на болванку " +
                "и дают высохнуть: волокна сжимаются и запоминают форму.")
            B(
                listOf(
                    "Растишка 2–3 мм: замочить 10–20 минут, промокнуть, дать «отлежаться» 30–60 минут.",
                    "Натянуть на форму, разгладить пальцами, зафиксировать края.",
                    "Сушить при комнатной температуре 12–24 часа, не на батарее.",
                    "После сушки — пропитка воском или финиш, иначе форма поплывёт от влаги."
                )
            )
            Example("Кобура и ножны: мокрая формовка по оружию/ножу, затем сушка и защита.")
        }
        14 -> {
            BraidDiagram()
            P("Плетение — самый декоративный способ обработки края и ручек. Кожа режется на тонкие ленты " +
                "(бобинки) и плетётся в косичку: простая косичка, круглая из четырёх полос, кнутовая.")
            B(
                listOf(
                    "Ленты режут по спирали или по прямой, ширина — 3–5 мм для ручек, 6–10 мм для кнутов.",
                    "Перед плетением ленты увлажняют: сухие ломаются и дают заломы.",
                    "Круглая косичка из 4 лент: каждая лента идёт «над-под» через две другие.",
                    "Натяжение ровное, иначе косичка будет волнистой.",
                    "Готовую косичку проклеивают и прошивают у основания."
                )
            )
            Example("Кнут 1,2 м из 4 лент 6 мм — 4–6 часов работы, расход около 5 дм² кожи.")
        }
        11 -> {
            P("Начинать лучше с двух вещей: картхолдер (мало деталей, короткие швы) и ремень " +
                "(одна деталь, длинный шов, сразу видно качество).")
            B(
                listOf(
                    "Картхолдер: 2 детали 100×70 мм, кожа 1,1–1,4 мм, отступ 3 мм, шаг 4 мм.",
                    "Ремень: полоса 3,5–4,5 мм, длина = обхват + 15–20 см, пряжка на 2–3 см уже.",
                    "Расход: картхолдер 1,4 дм² деталей → 2,5 дм² с раскроем; ремень 125 см — 7 дм².",
                    "Считать материал и цену удобно во вкладке «Кожа» и «Деньги»: там коэффициенты, работа и наценка."
                )
            )
            Example("Картхолдер — 2–3 часа и от 300 ₽ материалов. Ремень — 4–6 часов и от 800 ₽.")
        }
    }
}


@Composable
fun LessonBodyEn(id: Int) {
    when (id) {
        0 -> {
            P("Leather is tanned hide. The tannage decides how it holds shape, takes dye and ages.")
            B(
                listOf(
                    "Vegetable-tanned (veg tan): firm, holds shape, darkens with age — belts, sheaths, tooling.",
                    "Chrome-tanned: soft and stretchy — bags, garments, lining.",
                    "Crazy horse: waxed, shows patina — wallets and bags.",
                    "Split: lower layer after splitting, weaker — never for straps or handles."
                )
            )
            Example("First sheet: veg tan 1.5–1.8 mm covers belts, sleeves, covers and tooling practice.")
        }
        1 -> {
            HideDiagram()
            P("A hide is not uniform: fibre is dense along the spine, loose at the flanks, belly stretches.")
            B(
                listOf(
                    "Back and bend — load-bearing parts: straps, handles, bottom, flap.",
                    "Flanks and shoulder — small parts, lining, inner pockets.",
                    "Belly — hidden parts only.",
                    "The edge of a sheet is thinner: keep it away from stressed areas."
                )
            )
        }
        2 -> {
            ThicknessDiagram()
            TableRow("Item", "Thickness")
            TableRow("Lining, pockets", "0.8–1.2 mm")
            TableRow("Card holder, cover", "1.1–1.4 mm")
            TableRow("Wallet", "1.4–1.8 mm")
            TableRow("Bag, backpack", "1.6–2.2 mm")
            TableRow("Sheath, holster", "2.5–4.0 mm")
            TableRow("Belt", "3.0–4.5 mm")
            Example("“5–6 oz” is 2.0–2.4 mm. Two 2 mm layers are stiffer than one 4 mm and give a clean back.")
        }
        3 -> {
            B(
                listOf(
                    "Knife: shoemaker or scalpel, cuts along a ruler without stretching.",
                    "Pricking iron: 3 / 3.85 / 4 / 5 mm; 4 mm ≈ 6 stitches per inch.",
                    "Two blunt needles No. 0–2, waxed thread 0.6–0.8 mm.",
                    "Mallet, awl, glue, edge paint, wax.",
                    "Clamps or a stitching pony to hold the stack."
                )
            )
        }
        4 -> {
            P("A dull tool hurts more than inexperience: the knife crushes the edge, the iron tears fibres.")
            B(
                listOf(
                    "Knife: strop on leather with compound, 10–15 passes before cutting.",
                    "Pricking iron: never strike hard surfaces, clean glue, keep the teeth straight.",
                    "Needles: replace when they drag; rust stains light leather.",
                    "Glue: thin layers, keep the cap on — squeeze-out ruins seams."
                )
            )
        }
        5 -> {
            LayoutDiagram()
            P("Draw patterns at 1:1 and add seam allowance right away — otherwise parts will not match.")
            B(
                listOf(
                    "3–5 mm seam allowance, 8–10 mm for a folded edge.",
                    "Mark on the flesh side, cut with one firm pass along a ruler.",
                    "Place large parts first, small ones into the gaps.",
                    "For curves, turn the leather, not the knife."
                )
            )
        }
        6 -> {
            P("Allowance is the reserve for seams and folds. Grain direction is how the part behaves in use.")
            B(
                listOf(
                    "Parts that must not stretch are cut along the spine.",
                    "Straps and flaps are cut across — they must flex without cracking.",
                    "Keep grain direction consistent on visible parts, or the item will sag unevenly.",
                    "Route around scars and bites: the loss is bigger than the mark itself."
                )
            )
        }
        7 -> {
            StitchDiagram()
            P("Saddle stitch uses two needles passing one hole from both sides: the seam locks and does not unravel.")
            B(
                listOf(
                    "Mark 3 mm from the edge, spacing 4 mm.",
                    "Punch, thread both needles, enter the first hole from both sides.",
                    "Keep the same over-under order on every stitch.",
                    "Finish with two back stitches."
                )
            )
            Example("30 cm seam, 3.2 mm stack, 4 mm spacing → 75 stitches, about 1 m of thread.")
        }
        8 -> {
            P("Buckstitch is a braid along the edge. It does not join parts — it covers and decorates the edge.")
            B(
                listOf(
                    "Two needles, thread 6–7 edge lengths.",
                    "Work over the edge, crossing the thread alternately.",
                    "Great for belts, sheaths and open-edge wallets.",
                    "Level the edge first, otherwise the braid waves."
                )
            )
        }
        9 -> {
            EdgeDiagram()
            P("The edge is what people judge. Three steps: level, dampen, seal.")
            B(
                listOf(
                    "Sand 400 → 800 → 1200, flat, without rounding the corner.",
                    "Dampen slightly and sand again — fibres stand up evenly.",
                    "Edge paint: 2–3 thin coats, 15–20 minutes apart.",
                    "Or wax, buffed to a shine."
                )
            )
        }
        10 -> {
            P("Hardware holds the item together. Rule one: the hole equals the post diameter, never bigger.")
            B(
                listOf(
                    "Snaps and rivets go through lining or a washer.",
                    "D-rings sit on a leather loop, not on a thin strap.",
                    "A zipper needs a facing, otherwise the edge ripples.",
                    "Hide a magnetic snap under the lining so it does not grab cards."
                )
            )
        }
        11 -> {
            P("Start with a card holder (few parts, short seams) and a belt (one part, long seam, visible quality).")
            B(
                listOf(
                    "Card holder: 2 pieces 100×70 mm, 1.1–1.4 mm, 3 mm inset, 4 mm spacing.",
                    "Belt: 3.5–4.5 mm, length = waist + 15–20 cm.",
                    "Consumption: card holder 1.4 dm² → 2.5 dm² with cutting; 125 cm belt 7 dm².",
                    "Use the Leather and Costs tabs to price material and labour."
                )
            )
            Example("Card holder: 2–3 hours, from 300 ₽ of material. Belt: 4–6 hours, from 800 ₽.")
        }
        12 -> {
            CarvingDiagram()
            P("Tooling is pressing damp leather, not cutting it. It only works on vegetable-tanned leather.")
            B(
                listOf(
                    "Case the leather with water until the surface is cool and matte.",
                    "Swivel knife cuts the outline — not through, just a channel.",
                    "Beveler lifts one side of the line to create depth.",
                    "Stamps add texture, then the piece is dried, dyed and finished."
                )
            )
        }
        13 -> {
            MoldingDiagram()
            P("Wet molding: the leather takes the shape of an object and remembers it after drying.")
            B(
                listOf(
                    "Veg tan 2–3 mm: soak 10–20 minutes, blot, rest 30–60 minutes.",
                    "Stretch over the form, smooth with fingers, fix the edges.",
                    "Dry 12–24 hours at room temperature.",
                    "Seal with wax or finish, or the shape will soften with moisture."
                )
            )
        }
        14 -> {
            BraidDiagram()
            P("Braiding is the most decorative way to finish an edge or a handle: cut lace and plait it.")
            B(
                listOf(
                    "Lace 3–5 mm wide for handles, 6–10 mm for whips.",
                    "Dampen the lace: dry strands crack.",
                    "Four-strand round braid: each strand goes over two and under two.",
                    "Keep tension even, or the braid will wave.",
                    "Glue and stitch the ends."
                )
            )
        }
    }
}

@Composable
fun P(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Aero.ink)
}

@Composable
fun B(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall, color = Aero.ink) }
    }
}

@Composable
fun Example(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (Aero.dark) Color(0x331B3350) else Color(0x66DCEBFA))
            .padding(10.dp)
    ) {
        Text("Пример", style = MaterialTheme.typography.labelSmall, color = Aero.accentDeep)
        Text(text, style = MaterialTheme.typography.bodySmall, color = Aero.ink)
    }
}

@Composable
fun TableRow(left: String, right: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(left, style = MaterialTheme.typography.bodySmall, color = Aero.ink)
        Text(right, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Aero.accentDeep)
    }
}

// ---------- схемы ----------

@Composable
fun HideDiagram() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(170.dp)
            .semantics { contentDescription = "Схема зон шкуры: хребет, полы, вороток, брюшина" }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x55FFFFFF))
    ) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.width
            val h = size.height
            val outline = Path().apply {
                moveTo(w * 0.12f, h * 0.24f)
                cubicTo(w * 0.20f, h * 0.04f, w * 0.80f, h * 0.04f, w * 0.88f, h * 0.24f)
                cubicTo(w * 0.96f, h * 0.46f, w * 0.94f, h * 0.78f, w * 0.80f, h * 0.90f)
                cubicTo(w * 0.60f, h * 1.00f, w * 0.40f, h * 1.00f, w * 0.20f, h * 0.90f)
                cubicTo(w * 0.06f, h * 0.78f, w * 0.04f, h * 0.46f, w * 0.12f, h * 0.24f)
                close()
            }
            clipPath(outline) {
                drawRect(color = Color(0xFFE8D5BC))
                drawRect(color = Color(0xFFB98F63), topLeft = Offset(w * 0.30f, h * 0.22f), size = Size(w * 0.40f, h * 0.55f))
                drawRect(color = Color(0xFFD8BE9C), topLeft = Offset(0f, h * 0.22f), size = Size(w * 0.30f, h * 0.60f))
                drawRect(color = Color(0xFFD8BE9C), topLeft = Offset(w * 0.70f, h * 0.22f), size = Size(w * 0.30f, h * 0.60f))
                drawRect(color = Color(0xFFC9A883), topLeft = Offset(0f, h * 0.78f), size = Size(w, h * 0.25f))
                drawRect(color = Color(0xFFC9A883), topLeft = Offset(0f, 0f), size = Size(w, h * 0.18f))
            }
            drawPath(outline, color = Color(0xFF8A6A4A), style = Stroke(width = 3f))
        }
    }
    Text(
        Lang.t("Тёмная полоса в середине — хребет (плотный, силовой). По бокам полы, сверху вороток, внизу брюшина.",
            "Dark band in the middle is the spine (dense, load-bearing). Flanks at the sides, shoulder on top, belly at the bottom."),
        style = MaterialTheme.typography.labelSmall, color = Aero.muted
    )
}

@Composable
fun ThicknessDiagram() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .semantics { contentDescription = "Схема пакета слоёв кожи и толщины" }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x55FFFFFF))
    ) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val w = size.width
            val layerH = size.height / 5f
            val colors = listOf(Color(0xFFB98F63), Color(0xFFD8BE9C))
            colors.forEachIndexed { i, col ->
                val y = size.height - layerH * (i + 1)
                drawRoundRect(
                    color = col,
                    topLeft = Offset(w * 0.12f, y),
                    size = Size(w * 0.55f, layerH * 0.94f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }
            drawRoundRect(
                color = Color(0xFF8A6A4A),
                topLeft = Offset(w * 0.12f, size.height - layerH * 2),
                size = Size(w * 0.55f, layerH * 2),
                cornerRadius = CornerRadius(4f, 4f),
                style = Stroke(width = 2f)
            )
            val x = w * 0.78f
            drawLine(Color(0xFF2C6BD6), Offset(x, size.height - layerH * 2), Offset(x, size.height), strokeWidth = 3f)
            drawLine(Color(0xFF2C6BD6), Offset(x - 8f, size.height - layerH * 2), Offset(x + 8f, size.height - layerH * 2), strokeWidth = 3f)
            drawLine(Color(0xFF2C6BD6), Offset(x - 8f, size.height), Offset(x + 8f, size.height), strokeWidth = 3f)
        }
    }
    Text(
        Lang.t("Пакет из двух слоёв 1,6 мм = 3,2 мм. Толщину набирают склейкой.", "Two 1.6 mm layers = 3.2 mm. Thickness is built by gluing."),
        style = MaterialTheme.typography.labelSmall, color = Aero.muted
    )
}

@Composable
fun StitchDiagram() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(120.dp)
            .semantics { contentDescription = "Схема седельного шва: отверстия и перекрест нити" }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x55FFFFFF))
    ) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val w = size.width
            val h = size.height
            val rows = 8
            val dx = w / (rows + 1)
            val y = h / 2f
            val r = 5f
            for (i in 1..rows) {
                val cx = dx * i
                drawCircle(Color(0xFF8A6A4A), radius = r, center = Offset(cx, y))
                if (i < rows) {
                    val nx = dx * (i + 1)
                    drawLine(Color(0xFF2C6BD6), Offset(cx, y - r), Offset(nx, y - 24f), strokeWidth = 3f)
                    drawLine(Color(0xFF2C6BD6), Offset(cx, y + r), Offset(nx, y + 24f), strokeWidth = 3f)
                }
            }
            drawLine(Color(0x558A6A4A), Offset(0f, y), Offset(w, y), strokeWidth = 2f)
        }
    }
    Text(
        Lang.t("Нить ложится крестом: две иглы проходят одно отверстие навстречу.", "Thread crosses: two needles pass one hole from opposite sides."),
        style = MaterialTheme.typography.labelSmall, color = Aero.muted
    )
}

@Composable
fun EdgeDiagram() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(110.dp)
            .semantics { contentDescription = "Схема обработки торца в три стадии" }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x55FFFFFF))
    ) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val gap = size.width / 4f
            val top = size.height * 0.25f
            val layerH = size.height * 0.22f

            fun edge(x: Float, painted: Boolean, sanded: Boolean) {
                val radius = if (sanded) 10f else 2f
                drawRoundRect(
                    color = Color(0xFFD8BE9C),
                    topLeft = Offset(x, top),
                    size = Size(gap * 0.6f, layerH),
                    cornerRadius = CornerRadius(radius, radius)
                )
                drawRoundRect(
                    color = Color(0xFFB98F63),
                    topLeft = Offset(x, top + layerH),
                    size = Size(gap * 0.6f, layerH),
                    cornerRadius = CornerRadius(radius, radius)
                )
                if (painted) {
                    drawRoundRect(
                        color = Color(0xFF3B2A1A),
                        topLeft = Offset(x, top),
                        size = Size(gap * 0.6f, layerH * 2),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 5f)
                    )
                }
            }

            edge(gap * 0.2f, painted = false, sanded = false)
            edge(gap * 1.3f, painted = false, sanded = true)
            edge(gap * 2.4f, painted = true, sanded = true)
        }
    }
    Text(
        Lang.t("1 — сырой срез; 2 — отшлифован; 3 — краска или воск в 2–3 слоя.", "1 — raw edge; 2 — sanded; 3 — paint or wax, 2–3 coats."),
        style = MaterialTheme.typography.labelSmall, color = Aero.muted
    )
}

@Composable
fun LayoutDiagram() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp)
            .semantics { contentDescription = "Схема раскладки деталей на листе" }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x55FFFFFF))
    ) {
        Canvas(Modifier.fillMaxSize().padding(8.dp)) {
            val w = size.width
            val h = size.height
            drawRoundRect(
                color = Color(0x66FFFFFF),
                topLeft = Offset(w * 0.05f, h * 0.08f),
                size = Size(w * 0.9f, h * 0.84f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = Color(0x99A9C6E4),
                topLeft = Offset(w * 0.05f, h * 0.08f),
                size = Size(w * 0.9f, h * 0.84f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 2f)
            )
            val pieces = listOf(
                floatArrayOf(0.08f, 0.12f, 0.36f, 0.32f),
                floatArrayOf(0.46f, 0.12f, 0.30f, 0.32f),
                floatArrayOf(0.08f, 0.48f, 0.46f, 0.24f),
                floatArrayOf(0.56f, 0.48f, 0.38f, 0.24f),
                floatArrayOf(0.08f, 0.76f, 0.22f, 0.14f),
                floatArrayOf(0.32f, 0.76f, 0.18f, 0.14f)
            )
            pieces.forEach { p ->
                drawRoundRect(
                    color = Color(0xCC6FA3E0),
                    topLeft = Offset(w * p[0], h * p[1]),
                    size = Size(w * p[2], h * p[3]),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = Color(0xAAFFFFFF),
                    topLeft = Offset(w * p[0], h * p[1]),
                    size = Size(w * p[2], h * p[3]),
                    cornerRadius = CornerRadius(4f, 4f),
                    style = Stroke(width = 1.5f)
                )
            }
        }
    }
    Text(
        Lang.t("Крупные детали первыми, мелкие в промежутки. На шкуре реальное заполнение 60–75 %.", "Large parts first, small ones into the gaps. Real hide fill is 60–75 %."),
        style = MaterialTheme.typography.labelSmall, color = Aero.muted
    )
}

@Composable
fun CarvingDiagram() {
    Box(
        Modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = "Схема тиснения: контур ножом и фаска бивелером" }.clip(RoundedCornerShape(12.dp)).background(Color(0x55FFFFFF))
    ) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val w = size.width; val h = size.height
            drawRoundRect(color = Color(0xFFE2C39A), topLeft = Offset(0f, h * 0.15f),
                size = Size(w, h * 0.7f), cornerRadius = CornerRadius(6f, 6f))
            // контурная линия (свайвел-нож)
            val path = Path().apply {
                moveTo(w * 0.10f, h * 0.62f)
                cubicTo(w * 0.30f, h * 0.25f, w * 0.52f, h * 0.80f, w * 0.72f, h * 0.42f)
                cubicTo(w * 0.80f, h * 0.28f, w * 0.88f, h * 0.36f, w * 0.92f, h * 0.30f)
            }
            drawPath(path, color = Color(0xFF5B3A22), style = Stroke(width = 4f))
            // фаска с одной стороны
            val bevel = Path().apply {
                moveTo(w * 0.10f, h * 0.62f)
                cubicTo(w * 0.30f, h * 0.30f, w * 0.52f, h * 0.74f, w * 0.72f, h * 0.46f)
                cubicTo(w * 0.80f, h * 0.33f, w * 0.88f, h * 0.40f, w * 0.92f, h * 0.34f)
            }
            drawPath(bevel, color = Color(0x662C6BD6), style = Stroke(width = 9f))
            // «свет» от штампа
            drawCircle(Color(0x33FFFFFF), radius = h * 0.06f, center = Offset(w * 0.80f, h * 0.70f))
        }
    }
    Text(Lang.t("Линия ножом → фаска бивелером → штампы для объёма. Работает только на растишке.", "Knife line → bevel → stamps for volume. Veg tan only."),
        style = MaterialTheme.typography.labelSmall, color = Aero.muted)
}

@Composable
fun MoldingDiagram() {
    Box(
        Modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = "Схема формовки: кожа на болванке" }.clip(RoundedCornerShape(12.dp)).background(Color(0x55FFFFFF))
    ) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val w = size.width; val h = size.height
            // форма-болванка
            drawRoundRect(color = Color(0x33A9C6E4), topLeft = Offset(w * 0.28f, h * 0.35f),
                size = Size(w * 0.44f, h * 0.55f), cornerRadius = CornerRadius(10f, 10f))
            // кожа поверх
            drawRoundRect(color = Color(0xFFD8B98F), topLeft = Offset(w * 0.24f, h * 0.28f),
                size = Size(w * 0.52f, h * 0.66f), cornerRadius = CornerRadius(14f, 14f),
                style = Stroke(width = 8f))
            // капли
            for (i in 0 until 5) {
                drawCircle(Color(0x882C6BD6), radius = h * 0.022f,
                    center = Offset(w * (0.12f + i * 0.05f), h * 0.20f))
            }
            // стрелки натяжения
            drawLine(Color(0x662C6BD6), Offset(w * 0.12f, h * 0.55f), Offset(w * 0.22f, h * 0.55f), strokeWidth = 3f)
            drawLine(Color(0x662C6BD6), Offset(w * 0.88f, h * 0.55f), Offset(w * 0.78f, h * 0.55f), strokeWidth = 3f)
        }
    }
    Text(Lang.t("Замочить, натянуть на болванку, высушить 12–24 часа, пропитать воском.", "Soak, stretch over a form, dry 12–24 hours, seal with wax."),
        style = MaterialTheme.typography.labelSmall, color = Aero.muted)
}

@Composable
fun BraidDiagram() {
    Box(
        Modifier.fillMaxWidth().height(130.dp).semantics { contentDescription = "Схема плетения из четырёх лент" }.clip(RoundedCornerShape(12.dp)).background(Color(0x55FFFFFF))
    ) {
        Canvas(Modifier.fillMaxSize().padding(10.dp)) {
            val w = size.width; val h = size.height
            val colors = listOf(Color(0xFFB98F63), Color(0xFFD8BE9C), Color(0xFF9A7346), Color(0xFFE8D5BC))
            // четыре ленты «над-под»
            for (i in 0 until 4) {
                val x = w * (0.18f + i * 0.18f)
                val path = Path().apply {
                    moveTo(x, h * 0.12f)
                    cubicTo(x + w * 0.10f, h * 0.35f, x - w * 0.10f, h * 0.65f, x, h * 0.88f)
                }
                drawPath(path, colors[i], style = Stroke(width = h * 0.09f))
            }
            // поперечные перекрытия
            for (i in 0 until 3) {
                val y = h * (0.32f + i * 0.18f)
                drawLine(Color(0x66FFFFFF), Offset(w * 0.12f, y), Offset(w * 0.88f, y), strokeWidth = 3f)
            }
        }
    }
    Text(Lang.t("Четыре ленты идут «над-под» через две: ровное натяжение — ровная косичка.", "Four strands go over-under across two: even tension gives an even braid."),
        style = MaterialTheme.typography.labelSmall, color = Aero.muted)
}
