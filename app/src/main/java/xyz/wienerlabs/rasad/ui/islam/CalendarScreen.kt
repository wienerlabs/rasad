package xyz.wienerlabs.rasad.ui.islam

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.islam.IslamicPreferences
import xyz.wienerlabs.rasad.sky.SkyCatalog
import xyz.wienerlabs.rasad.sky.SkyPresentation
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.Hairline
import xyz.wienerlabs.rasad.ui.components.RoundIconButton
import xyz.wienerlabs.rasad.ui.hilal.HilalTab
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType

enum class CalendarTab(val title: String, val key: String) {
    Hilal("Hilal", "hilal"),
    Prayer("Vakitler", "vakitler"),
    Days("Günler", "gunler"),
    Events("Olaylar", "olaylar"),
    ;

    companion object {
        fun fromKey(key: String?): CalendarTab? = entries.firstOrNull { it.key == key?.lowercase() }
    }
}

@Composable
fun CalendarScreen(
    millis: Long,
    location: GeoPoint,
    placeName: String?,
    catalog: SkyCatalog,
    preferences: IslamicPreferences,
    tab: CalendarTab,
    onTabChange: (CalendarTab) -> Unit,
    initialEvening: Int?,
    onBack: () -> Unit,
    onShowInSky: (SkyPresentation) -> Unit,
) {
    BackHandler(onBack = onBack)
    var prayerDayOffset by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().background(Palette.Ink).statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(RasadIcons.Back, "Geri", onBack)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Takvim", style = RasadType.title, color = Palette.Text)
                    Text(placeName ?: location.formatted(), style = RasadType.caption, color = Palette.TextFaint)
                }
            }
            Spacer(Modifier.height(16.dp))
            Segmented(
                options = CalendarTab.entries.map { it.title },
                selected = CalendarTab.entries.indexOf(tab),
                onSelect = { onTabChange(CalendarTab.entries[it]) },
            )
            Spacer(Modifier.height(12.dp))
        }
        Hairline()
        key(tab) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 36.dp),
            ) {
                when (tab) {
                    CalendarTab.Hilal -> HilalTab(millis, location, catalog, preferences.hijriOffset, initialEvening)
                    CalendarTab.Prayer -> PrayerTab(millis, location, preferences, prayerDayOffset, { prayerDayOffset = it }, onShowInSky)
                    CalendarTab.Days -> DaysTab(millis, preferences)
                    CalendarTab.Events -> EventsTab(millis, location, onShowInSky)
                }
            }
        }
    }
}
