package xyz.wienerlabs.rasad.ui.islam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.HijriCalendar
import xyz.wienerlabs.rasad.astro.SkyBody
import xyz.wienerlabs.rasad.islam.AsrMethod
import xyz.wienerlabs.rasad.islam.DayPrayerTimes
import xyz.wienerlabs.rasad.islam.FalseDawn
import xyz.wienerlabs.rasad.islam.IslamicPreferences
import xyz.wienerlabs.rasad.islam.IslamicTexts
import xyz.wienerlabs.rasad.islam.Prayer
import xyz.wienerlabs.rasad.islam.PrayerCalculator
import xyz.wienerlabs.rasad.islam.TwilightMethod
import xyz.wienerlabs.rasad.sky.SkyObjectRef
import xyz.wienerlabs.rasad.sky.SkyPresentation
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.Hairline
import xyz.wienerlabs.rasad.ui.components.Pill
import xyz.wienerlabs.rasad.ui.components.SectionLabel
import xyz.wienerlabs.rasad.ui.components.pressable
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dayFormatter = DateTimeFormatter.ofPattern("d MMMM EEEE", xyz.wienerlabs.rasad.astro.TurkishLocale)
private val dayMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", xyz.wienerlabs.rasad.astro.TurkishLocale)
private const val RAMADAN_INDEX = 8
private const val TRUE_DAWN_DELAY_MILLIS = 10 * 60_000L

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrayerTab(
    millis: Long,
    location: GeoPoint,
    preferences: IslamicPreferences,
    dayOffset: Int,
    onDayOffsetChange: (Int) -> Unit,
    onShowInSky: (SkyPresentation) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val baseDate = remember(millis) { Instant.ofEpochMilli(millis).atZone(zone).toLocalDate() }
    val date = baseDate.plusDays(dayOffset.toLong())
    val settings = preferences.prayer
    val hijri = HijriCalendar.of(date, preferences.hijriOffset)
    val ramadan = hijri.monthIndex == RAMADAN_INDEX
    val times by produceState<DayPrayerTimes?>(null, date, location, settings, ramadan) {
        value = withContext(Dispatchers.Default) { runCatching { PrayerCalculator.compute(date, location, zone, settings, ramadan) }.getOrNull() }
    }
    val falseDawn by produceState<FalseDawn?>(null, date, location) {
        value = withContext(Dispatchers.Default) { runCatching { PrayerCalculator.darkFalseDawn(date, location, zone) }.getOrNull() }
    }
    val trueDawn = times?.get(Prayer.Fajr)?.plus(TRUE_DAWN_DELAY_MILLIS)
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Pill("Önceki", onClick = { onDayOffsetChange(dayOffset - 1) })
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (dayOffset == 0) "Bugün" else dayFormatter.format(date), style = RasadType.heading, color = Palette.Text, textAlign = TextAlign.Center)
                Text("${if (dayOffset == 0) dayFormatter.format(date) + " · " else ""}${hijri.formatted()} · tahmini", style = RasadType.caption, color = Palette.TextFaint, textAlign = TextAlign.Center)
            }
            Pill("Sonraki", onClick = { onDayOffsetChange(dayOffset + 1) })
        }
        Spacer(Modifier.height(14.dp))
        val day = times
        if (day == null) {
            Text("Vakitler hesaplanıyor…", style = RasadType.body, color = Palette.TextMuted)
        } else {
            val current = if (dayOffset == 0) day.currentAt(millis) else null
            day.times.forEachIndexed { index, time ->
                if (index > 0) Hairline()
                PrayerRow(
                    title = time.prayer.title,
                    value = time.millis?.let { Formats.clock(it) } ?: "Bu tarihte oluşmuyor",
                    active = time.prayer == current,
                    onClick = time.millis?.let { at -> { onShowInSky(presentationFor(time.prayer, at)) } },
                )
            }
        }
        IslamicTexts["4:103"]?.let {
            Spacer(Modifier.height(18.dp))
            SourceCard(it)
        }
        Spacer(Modifier.height(26.dp))
        SectionLabel("Fecr-i sâdık ve fecr-i kâzib")
        Spacer(Modifier.height(8.dp))
        NoteBlock(
            "İki şafak",
            "Sahte şafak (fecr-i kâzib), gün ağarmadan önce doğu ufkundan göğe doğru dikine uzanan soluk bir ışık sütunudur; bugün burçlar ışığı olarak bilinir. Bu sırada yemek serbesttir ve sabah namazının vakti henüz girmemiştir. Gerçek şafak (fecr-i sâdık) ufuk boyunca yatay olarak yayılan beyazlıktır; oruç onunla başlar, sabah namazının vakti onunla girer. Takvimler fecri, Güneş'in ufkun belirli bir açı (18° gibi) altında olduğu anla hesaplar; asıl ölçü gözle görülen yayılan beyazlıktır.",
        )
        Spacer(Modifier.height(10.dp))
        SourceChips(listOf("muslim:1094", "tirmidhi:149:fecr"))
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            falseDawn?.let { moment ->
                Pill("Fecr-i kâzibi göster", icon = RasadIcons.Eye, onClick = { onShowInSky(SkyPresentation(moment.millis, towardSunAltitude = 18.0, fov = 110.0, dawnGuide = true)) })
            }
            trueDawn?.let { at ->
                Pill("Fecr-i sâdıkı göster", icon = RasadIcons.Eye, onClick = { onShowInSky(SkyPresentation(at, towardSunAltitude = 10.0, fov = 100.0, dawnGuide = true)) })
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(dawnCaption(falseDawn, date), style = RasadType.caption, color = Palette.TextFaint)

        Spacer(Modifier.height(26.dp))
        SectionLabel("Hesap yöntemi")
        Spacer(Modifier.height(8.dp))
        Segmented(
            options = listOf("Diyanet açıları", "Ümmü'l-Kurâ"),
            selected = TwilightMethod.entries.indexOf(settings.twilight),
            onSelect = { preferences.updatePrayer(settings.copy(twilight = TwilightMethod.entries[it])) },
        )
        Spacer(Modifier.height(4.dp))
        Text(settings.twilight.title, style = RasadType.caption, color = Palette.TextFaint)
        Spacer(Modifier.height(12.dp))
        Segmented(
            options = listOf("İkindi: cumhur", "İkindi: Hanefî"),
            selected = AsrMethod.entries.indexOf(settings.asr),
            onSelect = { preferences.updatePrayer(settings.copy(asr = AsrMethod.entries[it])) },
        )
        Spacer(Modifier.height(4.dp))
        Text(settings.asr.title, style = RasadType.caption, color = Palette.TextFaint)
        Spacer(Modifier.height(10.dp))
        SourceChips(listOf("tirmidhi:149"))
        Spacer(Modifier.height(16.dp))
        Text(
            "Vakitler Güneş'in bulunduğun yerdeki konumundan hesaplanır. Resmî takvimler birkaç dakikalık temkin payı ekler; öğle vakti Güneş'in tepe noktasını geçmesiyle girer.",
            style = RasadType.caption,
            color = Palette.TextFaint,
        )
    }
}

private fun dawnCaption(falseDawn: FalseDawn?, date: java.time.LocalDate): String {
    val trueDawn = "Gerçek şafak, fecir vaktinden 10 dakika sonrasıyla gösterilir."
    return when {
        falseDawn == null -> trueDawn
        falseDawn.date == date && falseDawn.moonFree -> "Sahte şafak, Güneş ufkun 25° altındayken gösterilir. $trueDawn"
        falseDawn.moonFree -> "Bu sabah Ay ışığı sahte şafağı örter; o yüzden en yakın aysız sabah, ${dayMonthFormatter.format(falseDawn.date)} gösterilir. $trueDawn"
        else -> "Önümüzdeki günlerde Ay ışığı sahte şafağı örtüyor; gösterim yine de Güneş ufkun 25° altındayken yapılır. $trueDawn"
    }
}

private fun presentationFor(prayer: Prayer, at: Long): SkyPresentation = when (prayer) {
    Prayer.Fajr -> SkyPresentation(at, towardSunAltitude = 12.0, fov = 100.0, dawnGuide = true)
    Prayer.Isha -> SkyPresentation(at, towardSunAltitude = 12.0, fov = 100.0)
    else -> SkyPresentation(at, target = SkyObjectRef.Body(SkyBody.Sun), fov = 80.0)
}

@Composable
private fun PrayerRow(title: String, value: String, active: Boolean, onClick: (() -> Unit)?) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (active) Palette.FillStrong else Color.Transparent, shape)
            .border(1.dp, if (active) Palette.HairlineStrong else Color.Transparent, shape)
            .then(if (onClick != null) Modifier.pressable(onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = RasadType.heading, color = Palette.Text, modifier = Modifier.weight(1f))
        Text(value, style = RasadType.value, color = if (onClick != null) Palette.Text else Palette.TextFaint)
        if (onClick != null) {
            Spacer(Modifier.width(10.dp))
            Text("gökte", style = RasadType.caption, color = Palette.TextFaint)
        }
    }
}
