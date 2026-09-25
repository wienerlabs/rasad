package xyz.wienerlabs.rasad.ui.sky

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.astro.SkyBody
import xyz.wienerlabs.rasad.astro.SkySnapshot
import xyz.wienerlabs.rasad.astro.TurkishLocale
import xyz.wienerlabs.rasad.sky.ConstellationFigure
import xyz.wienerlabs.rasad.sky.Constellations
import xyz.wienerlabs.rasad.sky.SkyCatalog
import xyz.wienerlabs.rasad.sky.SkyObjectRef
import xyz.wienerlabs.rasad.sky.StarLoreBook
import xyz.wienerlabs.rasad.sky.direction
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.Hairline
import xyz.wienerlabs.rasad.ui.components.RoundIconButton
import xyz.wienerlabs.rasad.ui.components.SectionLabel
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.components.pressable
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType
import java.text.Collator
import java.text.Normalizer

data class SearchEntry(
    val ref: SkyObjectRef,
    val title: String,
    val subtitle: String,
    val keywords: String,
    val group: String,
)

data class SkyStatus(val altitude: Double, val azimuth: Double) {
    val visible: Boolean get() = altitude > 0.0

    val label: String
        get() = if (visible) "${Formats.degrees(altitude)} · ${Formats.compassPoint(azimuth)}" else "ufkun altında"
}

private const val GROUP_NOW = "Şu an gökyüzünde"
private const val NAKED_EYE_LIMIT = 6.5
private const val GROUP_SOLAR = "Güneş sistemi"
private const val GROUP_DIRECTION = "Yön"
private const val GROUP_STARS = "Yıldızlar"
private const val GROUP_CONSTELLATIONS = "Takımyıldızlar"

private val combiningMarks = Regex("\\p{Mn}+")

private fun fold(text: String): String =
    Normalizer.normalize(text.lowercase(TurkishLocale), Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .replace('ı', 'i')

fun buildSearchIndex(catalog: SkyCatalog): List<SearchEntry> {
    val bodies = SkyBody.entries.map { body ->
        SearchEntry(
            ref = SkyObjectRef.Body(body),
            title = body.displayName,
            subtitle = listOfNotNull(if (body == SkyBody.Sun) "Yıldızımız" else if (body == SkyBody.Moon) "Uydu" else "Gezegen", body.classicalName).joinToString(" · "),
            keywords = fold("${body.displayName} ${body.classicalName.orEmpty()} ${body.name}"),
            group = GROUP_SOLAR,
        )
    }
    val qibla = SearchEntry(SkyObjectRef.Qibla, "Kıble", "Kâbe yönü", fold("kible kabe mekke qibla"), GROUP_DIRECTION)
    val stars = catalog.namedStars
        .sortedBy { catalog.stars.magnitudes[it.index] }
        .map { meta ->
            val lore = StarLoreBook.forStar(meta.properName)
            val constellation = Constellations.turkishName(meta.constellation)
            SearchEntry(
                ref = SkyObjectRef.Star(meta.index),
                title = meta.properName.orEmpty(),
                subtitle = listOfNotNull(lore?.transliteration, constellation).joinToString(" · "),
                keywords = fold("${meta.properName} ${lore?.transliteration.orEmpty()} ${lore?.meaning.orEmpty()} $constellation ${Constellations.latinName(meta.constellation)}"),
                group = GROUP_STARS,
            )
        }
    val collator = Collator.getInstance(TurkishLocale)
    val constellations = catalog.constellations.mapIndexed { index, figure -> index to figure }
        .distinctBy { (_, figure) -> figure.code }
        .sortedWith(compareBy<Pair<Int, ConstellationFigure>> { it.second.rank }.thenComparator { a, b ->
            collator.compare(Constellations.turkishName(a.second.code), Constellations.turkishName(b.second.code))
        })
        .map { (index, figure) ->
            val code = figure.code
            SearchEntry(
                ref = SkyObjectRef.Constellation(index),
                title = Constellations.turkishName(code),
                subtitle = listOfNotNull(Constellations.latinName(code), Constellations.classicalName(code)?.transliteration, Constellations.folkName(code)).joinToString(" · "),
                keywords = fold(Constellations.searchText(code)),
                group = GROUP_CONSTELLATIONS,
            )
        }
    return bodies + qibla + stars + constellations
}

fun skyStatuses(index: List<SearchEntry>, catalog: SkyCatalog, snapshot: SkySnapshot, qiblaAzimuth: Double): Map<SkyObjectRef, SkyStatus> =
    index.associate { entry ->
        val direction = entry.ref.direction(catalog, snapshot, qiblaAzimuth)
        entry.ref to SkyStatus(direction.altitudeDegrees, direction.azimuthDegrees)
    }

fun skyHighlights(index: List<SearchEntry>, catalog: SkyCatalog, snapshot: SkySnapshot, statuses: Map<SkyObjectRef, SkyStatus>): List<SearchEntry> {
    fun altitudeOf(entry: SearchEntry) = statuses[entry.ref]?.altitude ?: -90.0
    val brightness = snapshot.bodies.associate { it.body to it.magnitude }
    val bodies = index
        .filter { it.ref is SkyObjectRef.Body && altitudeOf(it) > 3.0 && (brightness[(it.ref as SkyObjectRef.Body).body] ?: 99.0) <= NAKED_EYE_LIMIT }
        .sortedBy { brightness[(it.ref as SkyObjectRef.Body).body] ?: 99.0 }
    val constellations = index
        .filter { entry ->
            val ref = entry.ref as? SkyObjectRef.Constellation ?: return@filter false
            catalog.constellations[ref.index].rank == 1 && altitudeOf(entry) > 20.0
        }
        .sortedByDescending(::altitudeOf)
        .take(6)
    val stars = index
        .filter { entry ->
            val ref = entry.ref as? SkyObjectRef.Star ?: return@filter false
            catalog.stars.magnitudes[ref.index] < 1.6f && altitudeOf(entry) > 15.0
        }
        .take(5)
    return (bodies + constellations + stars).map { it.copy(group = GROUP_NOW) }
}

@Composable
fun SearchPanel(
    index: List<SearchEntry>,
    statuses: Map<SkyObjectRef, SkyStatus>,
    highlights: List<SearchEntry>,
    onSelect: (SkyObjectRef) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val folded = fold(query.trim())
    val results = remember(folded, index, highlights, statuses) {
        if (folded.isEmpty()) {
            val shown = highlights.mapTo(HashSet()) { it.ref }
            val rest = index.filter { it.ref !in shown }
            val brightStars = rest.filter { it.group == GROUP_STARS }.take(40)
            highlights + rest.filter { it.group == GROUP_SOLAR || it.group == GROUP_DIRECTION } + rest.filter { it.group == GROUP_CONSTELLATIONS } + brightStars
        } else {
            index.filter { it.keywords.contains(folded) }.sortedWith(
                compareBy(
                    { it.group },
                    { if (fold(it.title).startsWith(folded)) 0 else 1 },
                    { if (statuses[it.ref]?.visible == true) 0 else 1 },
                ),
            )
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .background(Palette.Ink)
            .statusBarsPadding()
            .imePadding(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).panel(RoundedCornerShape(22.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(RasadIcons.Search, contentDescription = null, tint = Palette.TextMuted, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Yıldız, gezegen, takımyıldız…", style = RasadType.body, color = Palette.TextFaint)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = RasadType.body.copy(color = Palette.Text),
                        cursorBrush = SolidColor(Palette.Text),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            RoundIconButton(RasadIcons.Close, "Kapat", onDismiss)
        }
        if (folded.isNotEmpty() && results.isEmpty()) {
            Text(
                "“${query.trim()}” için sonuç yok. Türkçe, Latince ya da Arapça adı deneyebilirsin.",
                style = RasadType.body,
                color = Palette.TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            )
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
            var lastGroup: String? = null
            results.forEachIndexed { position, entry ->
                if (entry.group != lastGroup) {
                    val group = entry.group
                    item(key = "group-$group") {
                        SectionLabel(group, Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp))
                    }
                    lastGroup = group
                }
                item(key = "entry-$position-${entry.group}-${entry.title}") {
                    val status = statuses[entry.ref]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .pressable(onClick = { onSelect(entry.ref) })
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, style = RasadType.heading, color = Palette.Text)
                            if (entry.subtitle.isNotEmpty()) {
                                Text(entry.subtitle, style = RasadType.caption, color = Palette.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (status != null && entry.ref != SkyObjectRef.Qibla) {
                            Spacer(Modifier.width(12.dp))
                            Text(status.label, style = RasadType.caption, color = if (status.visible) Palette.Text else Palette.TextFaint)
                        }
                    }
                    Hairline(Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}
