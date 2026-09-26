package xyz.wienerlabs.rasad.ui.islam

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.HijriCalendar
import xyz.wienerlabs.rasad.islam.DayPrayerTimes
import xyz.wienerlabs.rasad.islam.IslamicPreferences
import xyz.wienerlabs.rasad.islam.IslamicTexts
import xyz.wienerlabs.rasad.islam.PrayerCalculator
import xyz.wienerlabs.rasad.ui.components.Pill
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType
import java.time.Instant
import java.time.ZoneId

private const val RAMADAN_INDEX = 8

@Composable
fun PrayerStrip(millis: Long, location: GeoPoint, preferences: IslamicPreferences, onJump: (Long) -> Unit, modifier: Modifier = Modifier) {
    val zone = remember { ZoneId.systemDefault() }
    val date = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
    val settings = preferences.prayer
    val ramadan = HijriCalendar.of(date, preferences.hijriOffset).monthIndex == RAMADAN_INDEX
    val times by produceState<DayPrayerTimes?>(null, date, location, settings, ramadan) {
        value = withContext(Dispatchers.Default) { runCatching { PrayerCalculator.compute(date, location, zone, settings, ramadan) }.getOrNull() }
    }
    val day = times ?: return
    val current = day.currentAtMinute(millis / 60_000L)
    val shown = day.times.filter { it.millis != null }
    val listState = rememberLazyListState()
    LaunchedEffect(current, shown.size) {
        val index = shown.indexOfFirst { it.prayer == current }
        if (index >= 0) listState.animateScrollToItem((index - 1).coerceAtLeast(0))
    }
    LazyRow(
        modifier.widthIn(max = 420.dp).fillMaxWidth(),
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(shown, key = { it.prayer }) { time ->
            val at = time.millis ?: return@items
            Pill("${time.prayer.title} ${Formats.clock(at)}", onClick = { onJump(at) }, inverted = time.prayer == current)
        }
    }
}

@Composable
fun HilalDuaCard(modifier: Modifier = Modifier) {
    val source = IslamicTexts["tirmidhi:3451"] ?: return
    Column(
        modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .panel(RoundedCornerShape(18.dp), strong = true)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("Hilali gördüğünde", style = RasadType.label, color = Palette.TextMuted)
        source.arabic?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                it,
                style = RasadType.arabic.copy(fontSize = RasadType.arabic.fontSize * 0.6f, lineHeight = RasadType.arabic.lineHeight * 0.66f),
                color = Palette.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        source.transliteration?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = RasadType.caption.copy(fontStyle = FontStyle.Italic), color = Palette.Text)
        }
        Spacer(Modifier.height(4.dp))
        Text(source.meaning, style = RasadType.caption, color = Palette.TextMuted)
        Spacer(Modifier.height(4.dp))
        Text(listOfNotNull(source.citation, source.grade, source.translator).joinToString(" · "), style = RasadType.caption, color = Palette.TextFaint)
    }
}
