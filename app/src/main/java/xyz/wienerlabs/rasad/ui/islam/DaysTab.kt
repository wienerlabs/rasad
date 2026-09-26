package xyz.wienerlabs.rasad.ui.islam

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.astro.HijriCalendar
import xyz.wienerlabs.rasad.astro.TurkishLocale
import xyz.wienerlabs.rasad.islam.IslamicPreferences
import xyz.wienerlabs.rasad.islam.SacredDays
import xyz.wienerlabs.rasad.islam.SacredOccurrence
import xyz.wienerlabs.rasad.ui.components.Hairline
import xyz.wienerlabs.rasad.ui.components.SectionLabel
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val fullDate = DateTimeFormatter.ofPattern("d MMMM yyyy EEEE", TurkishLocale)
private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", TurkishLocale)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMMM yyyy", TurkishLocale)
private const val WHITE_DAYS_SHOWN = 2

@Composable
fun DaysTab(millis: Long, preferences: IslamicPreferences) {
    val today = remember(millis) { Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate() }
    val offset = preferences.hijriOffset
    val occurrences = remember(today, offset) {
        var whiteShown = 0
        SacredDays.upcoming(today, offset).filter { occurrence ->
            if (occurrence.day.key != "white") return@filter true
            whiteShown++ < WHITE_DAYS_SHOWN
        }
    }
    Column {
        NoteBlock(
            "Tarihler tahminidir",
            "Hicrî ay, hilalin görülmesiyle başlar. Aşağıdaki tarihler Ümmü'l-Kurâ takvimine göre hesaplanmıştır; bulunduğun yerde hilal farklı bir akşam görüldüyse buradan düzelt, bütün tarihler ona göre kayar.",
        )
        Spacer(Modifier.height(10.dp))
        SourceChips(listOf("bukhari:1909"))
        Spacer(Modifier.height(18.dp))
        SectionLabel("Bu ay başı bizde")
        Spacer(Modifier.height(8.dp))
        Segmented(
            options = listOf("Bir gün önce", "Takvimle aynı", "Bir gün sonra"),
            selected = when (offset) {
                1 -> 0
                0 -> 1
                else -> 2
            },
            onSelect = { preferences.updateHijriOffset(1 - it) },
        )
        Spacer(Modifier.height(6.dp))
        Text("Bugün: ${HijriCalendar.of(today, offset).formatted()} · tahmini", style = RasadType.caption, color = Palette.TextFaint)
        Spacer(Modifier.height(20.dp))
        occurrences.forEachIndexed { index, occurrence ->
            if (index > 0) Hairline()
            OccurrenceRow(occurrence, today)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Yalnızca Kur'an'da ya da sahih sünnette dayanağı bulunan günler listelenir.",
            style = RasadType.caption,
            color = Palette.TextFaint,
        )
    }
}

@Composable
private fun OccurrenceRow(occurrence: SacredOccurrence, today: LocalDate) {
    val day = occurrence.day
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Text(day.kind.title, style = RasadType.caption, color = Palette.TextFaint)
        Text(day.title, style = RasadType.heading, color = Palette.Text)
        Spacer(Modifier.height(2.dp))
        Text(dateText(occurrence, today), style = RasadType.label, color = Palette.Text)
        Text(hijriText(occurrence), style = RasadType.caption, color = Palette.TextMuted)
        Spacer(Modifier.height(6.dp))
        Text(day.summary, style = RasadType.body, color = Palette.TextMuted)
        occurrence.note?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = RasadType.caption, color = Palette.TextFaint)
        }
        Spacer(Modifier.height(8.dp))
        SourceChips(day.sources)
    }
}

private fun dateText(occurrence: SacredOccurrence, today: LocalDate): String {
    val prefix = if (!today.isBefore(occurrence.start) && !today.isAfter(occurrence.end)) "Takvime göre şu an · " else ""
    return prefix + if (occurrence.start == occurrence.end) {
        fullDate.format(occurrence.start)
    } else if (occurrence.start.year == occurrence.end.year) {
        "${dayMonth.format(occurrence.start)} ile ${dayMonthYear.format(occurrence.end)} arası"
    } else {
        "${dayMonthYear.format(occurrence.start)} ile ${dayMonthYear.format(occurrence.end)} arası"
    }
}

private fun hijriText(occurrence: SacredOccurrence): String {
    val month = HijriCalendar.monthNames[occurrence.monthIndex]
    val length = HijriCalendar.monthLength(occurrence.hijriYear, occurrence.monthIndex)
    val first = occurrence.day.firstDay.coerceAtMost(length)
    val last = occurrence.day.lastDay.coerceAtMost(length)
    return if (first == last) "$first $month ${occurrence.hijriYear}" else "$first ile $last $month ${occurrence.hijriYear}"
}
