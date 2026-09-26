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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.islam.IslamicTexts
import xyz.wienerlabs.rasad.islam.SourceText
import xyz.wienerlabs.rasad.ui.components.Pill
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.components.pressable
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType

private const val LONG_ARABIC = 160
private const val QURAN_LINE_FACTOR = 2.3f
private const val HADITH_LINE_FACTOR = 1.7f
private val pauseMarkAfterSpace = Regex(" ([\u06D6-\u06DC])")

fun arabicForDisplay(text: String): String = text.replace(pauseMarkAfterSpace, "\$1")

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Palette.Hairline, shape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEachIndexed { index, option ->
            val active = index == selected
            Text(
                option,
                style = RasadType.label,
                color = if (active) Palette.Ink else Palette.TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(if (active) Palette.Text else Color.Transparent, shape)
                    .pressable(onClick = { onSelect(index) })
                    .padding(vertical = 9.dp),
            )
        }
    }
}

@Composable
fun SourceCard(source: SourceText, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .panel(RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        source.arabic?.let {
            val size = RasadType.arabic.fontSize * if (it.length > LONG_ARABIC) 0.68f else 0.82f
            val lineFactor = if (source.key.first().isDigit()) QURAN_LINE_FACTOR else HADITH_LINE_FACTOR
            Text(arabicForDisplay(it), style = RasadType.arabic.copy(fontSize = size, lineHeight = size * lineFactor), color = Palette.Text, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
        source.transliteration?.let {
            Text(it, style = RasadType.body.copy(fontStyle = FontStyle.Italic), color = Palette.Text)
            Spacer(Modifier.height(6.dp))
        }
        Text("“${source.meaning}”", style = RasadType.body, color = Palette.TextMuted)
        Spacer(Modifier.height(8.dp))
        Text(listOfNotNull(source.citation, source.grade, source.translator).joinToString(" · "), style = RasadType.caption, color = Palette.TextFaint)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SourceChips(keys: List<String>, modifier: Modifier = Modifier) {
    var open by remember(keys) { mutableStateOf<String?>(null) }
    Column(modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            keys.mapNotNull { IslamicTexts[it] }.forEach { source ->
                Pill(source.shortCitation, onClick = { open = if (open == source.key) null else source.key }, inverted = open == source.key)
            }
        }
        open?.let { key ->
            IslamicTexts[key]?.let {
                Spacer(Modifier.height(10.dp))
                SourceCard(it)
            }
        }
    }
}

@Composable
fun NoteBlock(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().panel(RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(title, style = RasadType.heading, color = Palette.Text)
        Spacer(Modifier.height(6.dp))
        Text(body, style = RasadType.body, color = Palette.TextMuted)
    }
}
