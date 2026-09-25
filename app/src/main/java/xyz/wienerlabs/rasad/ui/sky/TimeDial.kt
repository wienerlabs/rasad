package xyz.wienerlabs.rasad.ui.sky

import android.graphics.Paint
import android.graphics.Typeface
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.sky.SkyClock
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.Pill
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.theme.Palette
import java.time.Instant
import java.time.ZoneId
import kotlin.math.floor

private const val MINUTES_PER_TICK = 10
private const val TICK_MILLIS = MINUTES_PER_TICK * 60_000L
private const val HOUR_MILLIS = 3_600_000L
private const val DAY_MILLIS = 86_400_000L

@Composable
fun TimeDial(clock: SkyClock, frameTick: () -> Long, typeface: Typeface, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val density = LocalDensity.current
    val spacing = with(density) { 13.dp.toPx() }
    val scope = rememberCoroutineScope()
    val fling = remember { Animatable(0f) }
    val tracker = remember { VelocityTracker() }
    val lastHour = remember { longArrayOf(Long.MIN_VALUE) }
    val paints = remember(typeface) {
        Triple(
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4F2EC.toInt(); strokeWidth = with(density) { 1.dp.toPx() }; strokeCap = Paint.Cap.ROUND },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0xFFF4F2EC.toInt()
                this.typeface = typeface
                textSize = with(density) { 11.dp.toPx() }
                textAlign = Paint.Align.CENTER
                fontFeatureSettings = "tnum"
            },
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF4F2EC.toInt(); strokeWidth = with(density) { 2.dp.toPx() }; strokeCap = Paint.Cap.ROUND },
        )
    }

    fun shiftBy(pixels: Float) {
        clock.shift((-pixels / spacing * TICK_MILLIS).toLong())
        val hour = floor(clock.now().toDouble() / HOUR_MILLIS).toLong()
        if (lastHour[0] != Long.MIN_VALUE && hour != lastHour[0]) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        lastHour[0] = hour
    }

    Column(modifier.panel(strong = true).padding(vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Pill("−1 gün", onClick = { clock.shift(-DAY_MILLIS) })
            Pill("−1 sa", onClick = { clock.shift(-HOUR_MILLIS) })
            Pill("Şimdi", onClick = { clock.reset() }, inverted = clock.isLive)
            Pill("+1 sa", onClick = { clock.shift(HOUR_MILLIS) })
            Pill("+1 gün", onClick = { clock.shift(DAY_MILLIS) })
            Pill(
                if (clock.playing) "Durdur" else "Hızlandır",
                icon = if (clock.playing) RasadIcons.Pause else RasadIcons.Play,
                onClick = { clock.playing = !clock.playing },
                inverted = clock.playing,
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(top = 8.dp)
                .pointerInput(clock) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            clock.playing = false
                            scope.launch { fling.stop() }
                            tracker.resetTracking()
                        },
                        onDragEnd = {
                            val velocity = tracker.calculateVelocity().x
                            scope.launch {
                                var previous = 0f
                                fling.snapTo(0f)
                                fling.animateDecay(velocity, exponentialDecay(frictionMultiplier = 1.6f)) {
                                    shiftBy(value - previous)
                                    previous = value
                                }
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            tracker.addPosition(change.uptimeMillis, change.position)
                            shiftBy(dragAmount)
                            change.consume()
                        },
                    )
                },
        ) {
            frameTick()
            val now = clock.now()
            val zone = ZoneId.systemDefault()
            val centerX = size.width / 2f
            val (tickPaint, labelPaint, needlePaint) = paints
            val firstTick = (floor(now.toDouble() / TICK_MILLIS).toLong() - (centerX / spacing).toLong() - 2) * TICK_MILLIS
            val lastTick = now + ((centerX / spacing).toLong() + 2) * TICK_MILLIS
            val baseline = size.height * 0.62f
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                var tick = firstTick
                while (tick <= lastTick) {
                    val x = centerX + (tick - now).toFloat() / TICK_MILLIS * spacing
                    val local = Instant.ofEpochMilli(tick).atZone(zone)
                    val isHour = local.minute == 0
                    val isDay = isHour && local.hour == 0
                    val height = when {
                        isDay -> size.height * 0.5f
                        isHour -> size.height * 0.34f
                        local.minute == 30 -> size.height * 0.2f
                        else -> size.height * 0.12f
                    }
                    val fade = 1f - (kotlin.math.abs(x - centerX) / centerX).coerceIn(0f, 1f)
                    tickPaint.alpha = (40 + 170 * fade).toInt()
                    native.drawLine(x, baseline, x, baseline - height, tickPaint)
                    if (isHour) {
                        labelPaint.alpha = (60 + 180 * fade).toInt()
                        val label = if (isDay) Formats.shortDayMonth(tick) else "%02d".format(local.hour)
                        native.drawText(label, x, baseline + labelPaint.textSize + 6f, labelPaint)
                    }
                    tick += TICK_MILLIS
                }
                native.drawLine(centerX, 0f, centerX, baseline + 4f, needlePaint)
            }
            drawCircle(Palette.Text, radius = 3.dp.toPx(), center = Offset(centerX, 0f))
        }
    }
}
