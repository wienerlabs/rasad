package xyz.wienerlabs.rasad.ui.sky

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import xyz.wienerlabs.rasad.islam.IslamicTexts
import xyz.wienerlabs.rasad.sky.ObjectDetails
import xyz.wienerlabs.rasad.sky.SkyObjectRef
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.FactGrid
import xyz.wienerlabs.rasad.ui.components.Hairline
import xyz.wienerlabs.rasad.ui.components.Pill
import xyz.wienerlabs.rasad.ui.components.RoundIconButton
import xyz.wienerlabs.rasad.ui.components.SectionLabel
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.islam.SourceCard
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ObjectSheet(
    details: ObjectDetails?,
    isTarget: Boolean,
    onDismiss: () -> Unit,
    onTarget: () -> Unit,
    onRelated: (SkyObjectRef) -> Unit,
    onJump: (Long) -> Unit,
    onOpenStarNote: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.64f).dp
    Column(
        modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .graphicsLayer { translationY = dragOffset.coerceAtLeast(0f) }
            .panel(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), strong = true)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 140f) onDismiss()
                        dragOffset = 0f
                    },
                    onVerticalDrag = { change, amount ->
                        dragOffset += amount
                        change.consume()
                    },
                )
            }
            .navigationBarsPadding(),
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Palette.HairlineStrong))
        }
        if (details == null) {
            Text("Hesaplanıyor…", style = RasadType.body, color = Palette.TextMuted, modifier = Modifier.padding(24.dp))
            return@Column
        }
        Column(
            Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(top = 14.dp, bottom = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(details.title, style = RasadType.display, color = Palette.Text)
                    Spacer(Modifier.height(4.dp))
                    Text(details.kind, style = RasadType.label, color = Palette.TextMuted)
                }
                RoundIconButton(RasadIcons.Close, "Kapat", onDismiss)
            }
            if (details.original != null) {
                Spacer(Modifier.height(18.dp))
                OriginBlock(details)
            }
            if (details.story != null) {
                Spacer(Modifier.height(16.dp))
                SectionLabel(if (details.original != null) "Adın hikâyesi" else "Not")
                Spacer(Modifier.height(6.dp))
                Text(details.story, style = RasadType.body, color = Palette.TextMuted)
            }
            val texts = details.texts.mapNotNull { IslamicTexts[it] }
            if (texts.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel(details.textsLabel ?: "Kaynak")
                Spacer(Modifier.height(8.dp))
                texts.forEach { source ->
                    SourceCard(source)
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (details.related.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel(details.relatedLabel ?: "İlgili")
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    details.related.forEach { related -> Pill(related.label, onClick = { onRelated(related.ref) }) }
                }
            }
            if (details.facts.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Hairline()
                FactGrid(details.facts)
            }
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Pill(if (isTarget) "Hedef açık" else "Beni oraya yönlendir", icon = RasadIcons.Target, onClick = onTarget, inverted = isTarget)
                val best = details.bestView
                if (details.suggestJump && best != null) Pill(details.jumpLabel ?: "En iyi zamana git", icon = RasadIcons.Clock, onClick = { onJump(best) })
                if (details.starNote) Pill("Yıldızlar ve İslam", onClick = onOpenStarNote)
            }
            details.footnote?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, style = RasadType.caption, color = Palette.TextFaint)
            }
        }
    }
}

@Composable
private fun OriginBlock(details: ObjectDetails) {
    Row(
        Modifier.fillMaxWidth().panel(RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            details.transliteration?.let { Text(it, style = RasadType.heading, color = Palette.Text) }
            details.meaning?.let {
                Spacer(Modifier.height(2.dp))
                Text("“$it”", style = RasadType.body.copy(fontStyle = FontStyle.Italic), color = Palette.TextMuted)
            }
        }
        Spacer(Modifier.width(12.dp))
        val original = details.original ?: return@Row
        if (details.originalScript == ObjectDetails.Script.Arabic) {
            Text(original, style = RasadType.arabic, color = Palette.Text)
        } else {
            Text(original, style = RasadType.title.copy(fontStyle = FontStyle.Italic), color = Palette.TextMuted)
        }
    }
}
