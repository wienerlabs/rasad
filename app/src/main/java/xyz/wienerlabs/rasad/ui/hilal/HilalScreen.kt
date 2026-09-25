package xyz.wienerlabs.rasad.ui.hilal

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.wienerlabs.rasad.astro.CrescentEvening
import xyz.wienerlabs.rasad.astro.CrescentStatus
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.Hilal
import xyz.wienerlabs.rasad.astro.HilalMap
import xyz.wienerlabs.rasad.astro.VisibilityGrid
import xyz.wienerlabs.rasad.astro.YallopCategory
import xyz.wienerlabs.rasad.sky.SkyCatalog
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.Hairline
import xyz.wienerlabs.rasad.ui.components.RoundIconButton
import xyz.wienerlabs.rasad.ui.components.SectionLabel
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.components.pressable
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val eveningFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", xyz.wienerlabs.rasad.astro.TurkishLocale)

@Composable
fun HilalScreen(
    millis: Long,
    location: GeoPoint,
    placeName: String?,
    catalog: SkyCatalog,
    onBack: () -> Unit,
    initialEvening: Int? = null,
) {
    BackHandler(onBack = onBack)
    val hourKey = millis / 3_600_000L
    val overview by produceState<HilalOverview?>(null, hourKey, location) {
        value = withContext(Dispatchers.Default) { runCatching { HilalModel.overview(millis, location) }.getOrNull() }
    }
    var selected by remember(overview) { mutableIntStateOf(initialEvening ?: overview?.suggestedEvening ?: 0) }
    val current = overview
    val evening = current?.evenings?.getOrNull(selected)
    val grid by produceState<VisibilityGrid?>(null, evening?.date, current?.conjunction) {
        value = null
        val target = evening ?: return@produceState
        value = withContext(Dispatchers.Default) { runCatching { HilalMap.compute(target.date, target.conjunctionMillis) }.getOrNull() }
    }
    val months by produceState<List<UpcomingMonth>?>(null, current?.conjunction, location) {
        val start = current?.conjunction ?: return@produceState
        value = withContext(Dispatchers.Default) { runCatching { HilalModel.upcomingMonths(start, location, 12) }.getOrNull() }
    }

    Box(Modifier.fillMaxSize().background(Palette.Ink)) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundIconButton(RasadIcons.Back, "Geri", onBack)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Hilal", style = RasadType.title, color = Palette.Text)
                Text(placeName ?: location.formatted(), style = RasadType.caption, color = Palette.TextFaint)
            }
        }

        if (current == null) {
            Box(Modifier.fillMaxWidth().height(420.dp), contentAlignment = Alignment.Center) {
                Text("Ay'ın yolu hesaplanıyor…", style = RasadType.body, color = Palette.TextMuted)
            }
        } else {
            HilalBody(current, evening, selected, { selected = it }, grid, months, catalog, location)
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(Palette.Ink.copy(alpha = 0.92f)),
    )
    }
}

@Composable
private fun HilalBody(
    current: HilalOverview,
    evening: CrescentEvening?,
    selected: Int,
    onSelect: (Int) -> Unit,
    grid: VisibilityGrid?,
    months: List<UpcomingMonth>?,
    catalog: SkyCatalog,
    location: GeoPoint,
) {
    Column {
        Hero(current, catalog)
        Spacer(Modifier.height(28.dp))
        Hairline()
        Spacer(Modifier.height(22.dp))

        SectionLabel("Sıradaki hilal")
        Spacer(Modifier.height(6.dp))
        Text("${current.month.monthName} ${current.month.year}", style = RasadType.display, color = Palette.Text)
        Spacer(Modifier.height(6.dp))
        val untilConjunction = current.conjunction - current.millis
        Text(
            "Kavuşum ${Formats.dayMonthWeekday(current.conjunction)} ${Formats.clock(current.conjunction)} · " +
                if (untilConjunction >= 0) "${Formats.duration(untilConjunction)} sonra" else "${Formats.duration(untilConjunction)} önce",
            style = RasadType.body,
            color = Palette.TextMuted,
        )
        Spacer(Modifier.height(16.dp))
        current.evenings.forEachIndexed { index, item ->
            EveningRow(item, selected = index == selected, onClick = { onSelect(index) })
            Spacer(Modifier.height(8.dp))
        }

        if (evening != null) {
            Spacer(Modifier.height(10.dp))
            val pose = current.eveningPoses.getOrNull(selected)
            if (evening.status == CrescentStatus.Computed && evening.moonAltitude != null) {
                SectionLabel("${eveningFormatter.format(evening.date)} akşamı batı ufku")
                Spacer(Modifier.height(8.dp))
                CrescentDiagram(evening, catalog.moonTexture, pose)
                Spacer(Modifier.height(10.dp))
                EveningNumbers(evening)
            }
            Spacer(Modifier.height(24.dp))
            SectionLabel("${eveningFormatter.format(evening.date)} akşamı hilal nereden görülür")
            Spacer(Modifier.height(8.dp))
            if (grid == null) {
                Box(Modifier.fillMaxWidth().height(130.dp).panel(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text("Dünya haritası hesaplanıyor…", style = RasadType.caption, color = Palette.TextMuted)
                }
            } else {
                VisibilityMap(grid, catalog.land, location)
            }
        }

        Spacer(Modifier.height(28.dp))
        Hairline()
        Spacer(Modifier.height(22.dp))
        SectionLabel("Önümüzdeki aylar")
        Spacer(Modifier.height(8.dp))
        val list = months
        if (list == null) {
            Text("On iki ayın hilali hesaplanıyor…", style = RasadType.caption, color = Palette.TextMuted)
        } else {
            list.forEachIndexed { index, month ->
                if (index > 0) Hairline()
                MonthRow(month)
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Görünürlük, Yallop (1997) q ölçütüyle ve gün batımı ile ay batımı arasındaki sürenin 4/9'u kadar sonrasındaki en iyi gözlem anı için hesaplanır. Hicrî tarihler Ümmü'l-Kurâ takvimine göredir. Resmî ay başları için Diyanet İşleri Başkanlığı'nın ilanlarını esas alın.",
            style = RasadType.caption,
            color = Palette.TextFaint,
        )
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun Hero(overview: HilalOverview, catalog: SkyCatalog) {
    val lunation = overview.lunation
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        MoonGlobe(catalog.moonTexture, overview.heroPose, Modifier.size(260.dp))
        Text(lunation.phaseName, style = RasadType.display, color = Palette.Text)
        Spacer(Modifier.height(4.dp))
        Text(
            "${Formats.percent(lunation.illumination)} aydınlık · ${Formats.decimal(lunation.ageDays, 1)} günlük",
            style = RasadType.label,
            color = Palette.TextMuted,
        )
        Spacer(Modifier.height(2.dp))
        Text(overview.hijriToday.formatted(), style = RasadType.caption, color = Palette.TextFaint)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            lunation.nextQuarters.forEach { (quarter, time) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    QuarterGlyph(quarter)
                    Spacer(Modifier.height(6.dp))
                    Text(Hilal.quarterName(quarter), style = RasadType.caption, color = Palette.TextMuted)
                    Text(Formats.shortDayMonth(time), style = RasadType.label, color = Palette.Text)
                    Text(Formats.clock(time), style = RasadType.caption, color = Palette.TextFaint)
                }
            }
        }
    }
}

@Composable
private fun QuarterGlyph(quarter: Int) {
    Box(Modifier.size(18.dp).clip(CircleShape).border(1.dp, Palette.HairlineStrong, CircleShape)) {
        when (quarter) {
            0 -> Unit
            2 -> Box(Modifier.fillMaxSize().background(Palette.Text))
            1 -> Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f))
                Box(Modifier.weight(1f).fillMaxSize().background(Palette.Text))
            }
            else -> Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxSize().background(Palette.Text))
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EveningRow(evening: CrescentEvening, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Palette.FillStrong else Color.Transparent, shape)
            .border(1.dp, if (selected) Palette.HairlineStrong else Palette.Hairline, shape)
            .pressable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryBadge(evening.category)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("${eveningFormatter.format(evening.date)} akşamı", style = RasadType.label, color = Palette.TextMuted)
            Spacer(Modifier.height(2.dp))
            Text(eveningTitle(evening), style = RasadType.heading, color = Palette.Text)
            eveningSubtitle(evening)?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = RasadType.caption, color = Palette.TextFaint)
            }
        }
    }
}

private fun eveningTitle(evening: CrescentEvening): String = when (evening.status) {
    CrescentStatus.MoonSetsFirst -> "Ay, Güneş'ten önce batıyor"
    CrescentStatus.BeforeConjunction -> "Henüz kavuşum olmadı"
    CrescentStatus.NoSunset -> "Güneş batmıyor"
    CrescentStatus.Undetermined -> "Bu enlemde hesaplanamıyor"
    CrescentStatus.Computed -> evening.category.title
}

private fun eveningSubtitle(evening: CrescentEvening): String? = when (evening.status) {
    CrescentStatus.Computed -> listOfNotNull(
        evening.moonAgeHours?.let { "Ay ${Formats.decimal(it, 0)} saatlik" },
        evening.lagMinutes?.let { "gecikme ${Formats.decimal(it, 0)} dk" },
        evening.arcl?.let { "uzanım ${Formats.degrees(it, 1)}" },
    ).joinToString(" · ")
    CrescentStatus.MoonSetsFirst -> evening.sunsetMillis?.let { "Gün batımı ${Formats.clock(it)}" }
    else -> null
}

@Composable
private fun CategoryBadge(category: YallopCategory) {
    val strength = category.strength
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Palette.Text.copy(alpha = 0.08f + 0.86f * strength), CircleShape)
            .border(1.dp, Palette.HairlineStrong, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(category.code, style = RasadType.heading, color = if (strength > 0.5f) Palette.Ink else Palette.Text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EveningNumbers(evening: CrescentEvening) {
    val facts = listOfNotNull(
        evening.sunsetMillis?.let { "Gün batımı" to Formats.clock(it) },
        evening.moonsetMillis?.let { "Ay batımı" to Formats.clock(it) },
        evening.arcv?.let { "Yay farkı (ARCV)" to Formats.degrees(it, 2) },
        evening.widthArcMinutes?.let { "Hilal kalınlığı" to Hilal.widthLabel(it) },
        evening.q?.let { "q değeri" to Formats.decimal(it, 3).replace('-', '−') },
        evening.illumination?.let { "Aydınlık" to Formats.percent(it) },
    )
    Column {
        facts.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                row.forEach { (label, value) ->
                    Column(Modifier.weight(1f)) {
                        Text(label, style = RasadType.caption, color = Palette.TextFaint)
                        Text(value, style = RasadType.value, color = Palette.Text)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MonthRow(month: UpcomingMonth) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${month.month.monthName} ${month.month.year}", style = RasadType.heading, color = Palette.Text)
            Text("Kavuşum ${Formats.shortDayMonth(month.conjunction)} ${Formats.clock(month.conjunction)}", style = RasadType.caption, color = Palette.TextFaint)
        }
        val naked = month.firstNakedEye
        val optical = month.firstOptical
        Column(horizontalAlignment = Alignment.End) {
            when {
                naked != null -> {
                    Text("${shortDate(naked)} akşamı", style = RasadType.label, color = Palette.Text)
                    Text("Çıplak gözle · ${naked.category.code}", style = RasadType.caption, color = Palette.TextMuted)
                }
                optical != null -> {
                    Text("${shortDate(optical)} akşamı", style = RasadType.label, color = Palette.Text)
                    Text("Dürbünle · ${optical.category.code}", style = RasadType.caption, color = Palette.TextMuted)
                }
                else -> Text("İlk üç akşam zor", style = RasadType.caption, color = Palette.TextMuted)
            }
        }
    }
}

private fun shortDate(evening: CrescentEvening): String =
    Formats.shortDayMonth(evening.date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli())
