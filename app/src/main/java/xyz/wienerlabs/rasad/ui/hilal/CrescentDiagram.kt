package xyz.wienerlabs.rasad.ui.hilal

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import xyz.wienerlabs.rasad.astro.CrescentEvening
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun CrescentDiagram(evening: CrescentEvening, moonTexture: Bitmap, pose: MoonPose?, modifier: Modifier = Modifier) {
    val sunAzimuth = evening.sunAzimuth ?: return
    val sunAltitude = evening.sunAltitude ?: return
    val moonAzimuth = evening.moonAzimuth ?: return
    val moonAltitude = evening.moonAltitude ?: return
    var deltaAzimuth = moonAzimuth - sunAzimuth
    if (deltaAzimuth > 180) deltaAzimuth -= 360
    if (deltaAzimuth < -180) deltaAzimuth += 360
    val halfSpan = max(16.0, abs(deltaAzimuth) + 7.0)
    val bottom = -9.0
    val top = max(16.0, moonAltitude + 6.0)
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Palette.Hairline, RoundedCornerShape(18.dp)),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val scale = minOf(widthPx / (2 * halfSpan), heightPx / (top - bottom)).toFloat()
        val originX = widthPx / 2f
        val horizonY = (heightPx - (0.0 - bottom).toFloat() * scale)
        fun px(deltaDegrees: Double) = originX + deltaDegrees.toFloat() * scale
        fun py(altitude: Double) = horizonY - altitude.toFloat() * scale

        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                Brush.verticalGradient(
                    0f to Color(0xFF0B1330),
                    0.55f to Color(0xFF1F2440),
                    1f to Color(0xFF6A4A43),
                    startY = 0f,
                    endY = horizonY,
                ),
                size = size.copy(height = horizonY),
            )
            drawRect(Color(0xFF07080B), topLeft = Offset(0f, horizonY), size = size.copy(height = size.height - horizonY))
            var altitude = 5.0
            while (altitude < top) {
                val y = py(altitude)
                drawLine(Color(0x1FF4F2EC), Offset(0f, y), Offset(size.width, y), 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f)))
                altitude += 5.0
            }
            drawLine(Color(0x99F4F2EC), Offset(0f, horizonY), Offset(size.width, horizonY), 1.4.dp.toPx())
            val sun = Offset(px(0.0), py(sunAltitude))
            drawCircle(
                Brush.radialGradient(listOf(Color(0x88FFC98A), Color(0x00FFC98A)), center = sun, radius = 46.dp.toPx()),
                radius = 46.dp.toPx(),
                center = sun,
            )
            drawCircle(Color(0xFFFFE7C2), radius = 7.dp.toPx(), center = sun)
            val moon = Offset(px(deltaAzimuth), py(moonAltitude))
            drawLine(Color(0x33F4F2EC), sun, moon, 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
            drawCircle(Color(0x40F4F2EC), radius = 24.dp.toPx(), center = moon, style = Stroke(1f))
        }

        val moonSize = 34.dp
        val moonSizePx = with(density) { moonSize.toPx() }
        if (pose != null) {
            MoonGlobe(
                texture = moonTexture,
                pose = pose,
                halo = false,
                gain = 2.1f,
                shadowAlpha = 0.06f,
                modifier = Modifier
                    .size(moonSize)
                    .offset { IntOffset((px(deltaAzimuth) - moonSizePx / 2).roundToInt(), (py(moonAltitude) - moonSizePx / 2).roundToInt()) },
            )
        }

        Label("Ay ${Formats.degrees(moonAltitude, 1)}", px(deltaAzimuth) + moonSizePx * 0.75f, py(moonAltitude) - with(density) { 10.dp.toPx() })
        Label("Güneş ${Formats.degrees(sunAltitude, 1).replace('-', '−')}", px(0.0) + with(density) { 12.dp.toPx() }, py(sunAltitude) - with(density) { 8.dp.toPx() })
        Label("Ufuk · ${Formats.degrees(sunAzimuth)} ${Formats.compassPoint(sunAzimuth)}", with(density) { 12.dp.toPx() }, horizonY + with(density) { 8.dp.toPx() })
        evening.bestTimeMillis?.let {
            Label("En iyi gözlem ${Formats.clock(it)}", with(density) { 12.dp.toPx() }, with(density) { 10.dp.toPx() })
        }
    }
}

@Composable
private fun Label(text: String, x: Float, y: Float) {
    Box(Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) }) {
        Text(text, style = RasadType.caption, color = Palette.Text)
    }
}
