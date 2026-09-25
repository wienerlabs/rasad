package xyz.wienerlabs.rasad.ui.sky

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.sky.Guidance
import xyz.wienerlabs.rasad.sky.GuidanceReadout
import xyz.wienerlabs.rasad.sky.ViewingPlan
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.Pill
import xyz.wienerlabs.rasad.ui.components.RoundIconButton
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType
import kotlin.math.abs

@Composable
fun GuidanceCard(
    readout: GuidanceReadout,
    plan: ViewingPlan?,
    nowMillis: Long,
    onJump: (Long) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .panel(RoundedCornerShape(24.dp), strong = true)
            .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GuidanceDial(readout, Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    readout.name,
                    style = RasadType.heading,
                    color = Palette.Text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!readout.centered && !readout.onScreen) {
                    Spacer(Modifier.width(8.dp))
                    Text("${readout.separation}°", style = RasadType.label, color = Palette.TextMuted)
                }
            }
            Text(Guidance.instruction(readout), style = RasadType.label, color = Palette.Text)
            Text(statusLine(readout, plan, nowMillis), style = RasadType.caption, color = Palette.TextFaint, maxLines = 2)
            val jump = plan?.bestTime?.takeIf { (readout.belowHorizon || readout.altitude < 10) && it > nowMillis }
            if (jump != null) {
                Spacer(Modifier.height(8.dp))
                Pill("O saate git", icon = RasadIcons.Clock, onClick = { onJump(jump) })
            }
        }
        Spacer(Modifier.width(6.dp))
        RoundIconButton(RasadIcons.Close, "Hedefi kapat", onClose)
    }
}

fun describeTime(millis: Long, nowMillis: Long): String = Formats.relativeTime(millis, nowMillis)

private fun statusLine(readout: GuidanceReadout, plan: ViewingPlan?, nowMillis: Long): String {
    if (readout.belowHorizon) {
        return when {
            plan?.neverRises == true -> "Bu konumdan hiç doğmaz"
            plan?.nextRise != null -> "Ufkun ${abs(readout.altitude)}° altında · doğuş ${describeTime(plan.nextRise, nowMillis)}"
            else -> "Ufkun ${abs(readout.altitude)}° altında"
        }
    }
    val position = "${readout.altitude}° yükseklik · ${Formats.compassPoint(readout.azimuth.toDouble())}"
    val best = plan?.bestTime?.takeIf { it - nowMillis > 20 * 60_000L && plan.bestAltitude - readout.altitude >= 12.0 } ?: return position
    return "$position · en iyi ${describeTime(best, nowMillis)}, ${plan.bestAltitude.toInt()}°"
}

@Composable
private fun GuidanceDial(readout: GuidanceReadout, modifier: Modifier = Modifier) {
    var unwrapped by remember { mutableFloatStateOf(readout.screenAngleDegrees.toFloat()) }
    LaunchedEffect(readout.screenAngleDegrees) {
        val target = readout.screenAngleDegrees.toFloat()
        val delta = ((target - unwrapped) % 360f + 540f) % 360f - 180f
        unwrapped += delta
    }
    val angle by animateFloatAsState(unwrapped, spring(dampingRatio = 0.8f, stiffness = 220f), label = "dialAngle")
    val found = readout.centered
    val inView = readout.onScreen
    val ringColor = Palette.HairlineStrong
    val ink = Palette.Text
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(ringColor, radius - 1.dp.toPx(), center, style = Stroke(1.dp.toPx()))
        when {
            found -> {
                drawCircle(ink, radius * 0.24f, center)
                drawCircle(ink, radius * 0.52f, center, style = Stroke(1.4.dp.toPx()))
            }
            inView -> {
                drawCircle(ink, radius * 0.14f, center)
                drawCircle(ink, radius * 0.46f, center, style = Stroke(1.2.dp.toPx()))
                rotate(-angle, center) {
                    drawCircle(ink, 2.6.dp.toPx(), Offset(center.x + radius * 0.46f, center.y))
                }
            }
            else -> rotate(-angle, center) {
                val tip = Offset(center.x + radius * 0.68f, center.y)
                val tail = Offset(center.x - radius * 0.36f, center.y)
                drawLine(ink, tail, tip, strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
                val wing = radius * 0.26f
                val arrow = Path().apply {
                    moveTo(tip.x - wing, tip.y - wing * 0.8f)
                    lineTo(tip.x, tip.y)
                    lineTo(tip.x - wing, tip.y + wing * 0.8f)
                }
                drawPath(arrow, ink, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
}
