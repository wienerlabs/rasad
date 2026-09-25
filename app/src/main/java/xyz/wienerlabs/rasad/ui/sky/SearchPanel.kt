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
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.astro.SkyBody
import xyz.wienerlabs.rasad.astro.TurkishLocale
import xyz.wienerlabs.rasad.sky.Constellations
import xyz.wienerlabs.rasad.sky.SkyCatalog
import xyz.wienerlabs.rasad.sky.SkyObjectRef
import xyz.wienerlabs.rasad.sky.StarLoreBook
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.Hairline
import xyz.wienerlabs.rasad.ui.components.RoundIconButton
import xyz.wienerlabs.rasad.ui.components.SectionLabel
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.components.pressable
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType
import java.text.Normalizer

data class SearchEntry(
    val ref: SkyObjectRef,
    val title: String,
    val subtitle: String,
    val keywords: String,
    val group: String,
)

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
            group = "Güneş sistemi",
        )
    }
    val qibla = SearchEntry(SkyObjectRef.Qibla, "Kıble", "Kâbe yönü", fold("kible kabe mekke qibla"), "Yön")
    val stars = catalog.namedStars
        .sortedBy { catalog.stars.magnitudes[it.index] }
        .map { meta ->
            val lore = StarLoreBook.forStar(meta.properName)
            val constellation = Constellations.turkishName(meta.constellation)
            SearchEntry(
                ref = SkyObjectRef.Star(meta.index),
                title = meta.properName.orEmpty(),
                subtitle = listOfNotNull(lore?.transliteration, constellation).joinToString(" · "),
                keywords = fold("${meta.properName} ${lore?.transliteration.orEmpty()} ${lore?.meaning.orEmpty()} $constellation"),
                group = "Yıldızlar",
            )
        }
    val constellations = catalog.constellations.mapIndexed { index, figure ->
        SearchEntry(
            ref = SkyObjectRef.Constellation(index),
            title = Constellations.turkishName(figure.code),
            subtitle = "Takımyıldız · ${figure.code}",
            keywords = fold("${Constellations.turkishName(figure.code)} ${figure.code}"),
            group = "Takımyıldızlar",
        )
    }.distinctBy { it.title }
    return bodies + qibla + stars + constellations
}

@Composable
fun SearchPanel(index: List<SearchEntry>, onSelect: (SkyObjectRef) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val folded = fold(query.trim())
    val results = remember(folded, index) {
        if (folded.isEmpty()) {
            val brightStars = index.filter { it.group == "Yıldızlar" }.take(40)
            index.filter { it.group != "Yıldızlar" && it.group != "Takımyıldızlar" } + brightStars + index.filter { it.group == "Takımyıldızlar" }
        } else {
            index.filter { it.keywords.contains(folded) }.sortedWith(compareBy({ it.group }, { if (fold(it.title).startsWith(folded)) 0 else 1 }))
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
                item(key = "entry-$position-${entry.title}") {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .pressable(onClick = { onSelect(entry.ref) })
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Text(entry.title, style = RasadType.heading, color = Palette.Text)
                        Text(entry.subtitle, style = RasadType.caption, color = Palette.TextMuted)
                    }
                    Hairline(Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}
