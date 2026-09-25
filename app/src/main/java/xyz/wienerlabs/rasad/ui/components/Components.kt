package xyz.wienerlabs.rasad.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.sky.Fact
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType

fun Modifier.panel(shape: Shape = RoundedCornerShape(24.dp), strong: Boolean = false): Modifier =
    this
        .clip(shape)
        .background(if (strong) Palette.Panel else Palette.PanelSoft, shape)
        .border(1.dp, Palette.Hairline, shape)

@Composable
fun Modifier.pressable(onClick: () -> Unit, bounded: Boolean = true, radius: Dp = Dp.Unspecified, role: Role = Role.Button): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return clickable(
        interactionSource = interaction,
        indication = ripple(bounded = bounded, radius = radius, color = Palette.Text),
        role = role,
        onClick = onClick,
    )
}

@Composable
fun RoundIconButton(icon: ImageVector, description: String, onClick: () -> Unit, modifier: Modifier = Modifier, active: Boolean = false) {
    val background by animateColorAsState(if (active) Palette.Text else Palette.PanelSoft, label = "roundBackground")
    val tint by animateColorAsState(if (active) Palette.Ink else Palette.Text, label = "roundTint")
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(background, CircleShape)
            .border(1.dp, Palette.Hairline, CircleShape)
            .pressable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: (() -> Unit)? = null, inverted: Boolean = false) {
    val shape = RoundedCornerShape(50)
    val base = modifier
        .clip(shape)
        .background(if (inverted) Palette.Text else Palette.PanelSoft, shape)
        .border(1.dp, if (inverted) Palette.Text else Palette.Hairline, shape)
    Row(
        modifier = (if (onClick != null) base.pressable(onClick) else base).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = if (inverted) Palette.Ink else Palette.Text, modifier = Modifier.size(16.dp))
        Text(text, style = RasadType.label, color = if (inverted) Palette.Ink else Palette.Text, maxLines = 1)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = RasadType.caption, color = Palette.TextFaint, modifier = modifier)
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Palette.Hairline))
}

@Composable
fun FactGrid(facts: List<Fact>, modifier: Modifier = Modifier) {
    Column(modifier) {
        facts.chunked(2).forEachIndexed { rowIndex, row ->
            if (rowIndex > 0) Hairline()
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                row.forEachIndexed { index, fact ->
                    if (index > 0) Spacer(Modifier.width(16.dp))
                    FactCell(fact)
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RowScope.FactCell(fact: Fact) {
    Column(Modifier.weight(1f)) {
        Text(fact.label, style = RasadType.caption, color = Palette.TextFaint)
        Spacer(Modifier.height(3.dp))
        Text(fact.value, style = RasadType.value, color = Palette.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressable(onClick = { onCheckedChange(!checked) }, role = Role.Switch)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = RasadType.body, color = Palette.Text, modifier = Modifier.weight(1f))
        MonoSwitch(checked)
    }
}

@Composable
fun MonoSwitch(checked: Boolean) {
    val track by animateColorAsState(if (checked) Palette.Text else Palette.Fill, label = "track")
    val thumb by animateColorAsState(if (checked) Palette.Ink else Palette.TextMuted, label = "thumb")
    val offset by animateDpAsState(if (checked) 18.dp else 2.dp, label = "thumbOffset")
    Box(
        Modifier
            .size(width = 40.dp, height = 24.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .border(1.dp, Palette.HairlineStrong, RoundedCornerShape(50)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset(x = offset)
                .size(20.dp)
                .clip(CircleShape)
                .background(thumb),
        )
    }
}
