package xyz.wienerlabs.rasad.ui.hilal

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.VisibilityGrid
import xyz.wienerlabs.rasad.astro.YallopCategory
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType
import kotlin.math.roundToInt

private const val MOON_FIRST_PIXEL = 0x66000000
private const val LAT_TOP = 60.0
private const val LAT_BOTTOM = -60.0

fun categoryAlpha(category: YallopCategory): Float = when (category) {
    YallopCategory.A -> 0.50f
    YallopCategory.B -> 0.34f
    YallopCategory.C -> 0.21f
    YallopCategory.D -> 0.11f
    YallopCategory.E -> 0.05f
    YallopCategory.F -> 0f
}

private fun zoneBitmap(grid: VisibilityGrid): Bitmap {
    val bitmap = Bitmap.createBitmap(grid.columns, grid.rows, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(grid.columns * grid.rows)
    for (row in 0 until grid.rows) {
        val y = grid.rows - 1 - row
        for (column in 0 until grid.columns) {
            val category = grid.categoryAt(column, row)
            pixels[y * grid.columns + column] = if (grid.moonSetsFirstAt(column, row)) {
                MOON_FIRST_PIXEL
            } else {
                val alpha = if (category == null) 0f else categoryAlpha(category)
                val a = (alpha * 255).roundToInt().coerceIn(0, 255)
                (a shl 24) or (a shl 16) or (a shl 8) or a
            }
        }
    }
    bitmap.setPixels(pixels, 0, grid.columns, 0, 0, grid.columns, grid.rows)
    return bitmap
}

@Composable
fun VisibilityMap(grid: VisibilityGrid?, land: List<FloatArray>, location: GeoPoint, modifier: Modifier = Modifier) {
    val zones = remember(grid) { grid?.let { zoneBitmap(it).asImageBitmap() } }
    val landPaths = remember(land) { land }
    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF080A0F))
                .border(1.dp, Palette.Hairline, RoundedCornerShape(16.dp)),
        ) {
            val width = size.width
            val height = size.height
            fun x(lon: Double) = ((lon + 180.0) / 360.0 * width).toFloat()
            fun y(lat: Double) = ((LAT_TOP - lat) / (LAT_TOP - LAT_BOTTOM) * height).toFloat()

            clipRect {
                val path = Path()
                val landOutlines = ArrayList<Path>(landPaths.size)
                for (ring in landPaths) {
                    path.reset()
                    var first = true
                    var previousLon = 0.0
                    var i = 0
                    while (i < ring.size) {
                        val lon = ring[i].toDouble()
                        val lat = ring[i + 1].toDouble().coerceIn(LAT_BOTTOM - 5, LAT_TOP + 5)
                        if (first || kotlin.math.abs(lon - previousLon) > 180) {
                            if (!first) path.close()
                            path.moveTo(x(lon), y(lat))
                            first = false
                        } else {
                            path.lineTo(x(lon), y(lat))
                        }
                        previousLon = lon
                        i += 2
                    }
                    path.close()
                    drawPath(path, Color(0xFF1A1E26))
                    landOutlines += Path().apply { addPath(path) }
                }

                if (zones != null && grid != null) {
                    val cellWidth = width * grid.step / 360.0
                    val cellHeight = height * grid.step / (LAT_TOP - LAT_BOTTOM)
                    drawImage(
                        image = zones,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(grid.columns, grid.rows),
                        dstOffset = IntOffset((-cellWidth / 2).roundToInt(), (-cellHeight / 2).roundToInt()),
                        dstSize = IntSize((grid.columns * cellWidth).roundToInt(), (grid.rows * cellHeight).roundToInt()),
                        filterQuality = FilterQuality.High,
                    )
                }

                landOutlines.forEach { outline ->
                    drawPath(outline, Color(0x66000000), style = Stroke(width = 2.2.dp.toPx()))
                    drawPath(outline, Color(0x8CF4F2EC), style = Stroke(width = 0.7.dp.toPx()))
                }

                var lon = -150.0
                while (lon <= 150.0) {
                    drawLine(Color(0x14F4F2EC), Offset(x(lon), 0f), Offset(x(lon), height), strokeWidth = 1f)
                    lon += 30.0
                }
                var lat = -30.0
                while (lat <= 30.0) {
                    drawLine(Color(if (lat == 0.0) 0x24F4F2EC else 0x14F4F2EC), Offset(0f, y(lat)), Offset(width, y(lat)), strokeWidth = 1f)
                    lat += 30.0
                }

                val here = Offset(x(location.longitude), y(location.latitude.coerceIn(LAT_BOTTOM, LAT_TOP)))
                drawCircle(Palette.Ink, radius = 5.dp.toPx(), center = here)
                drawCircle(Palette.Text, radius = 3.2.dp.toPx(), center = here)
                drawCircle(Palette.Text, radius = 8.dp.toPx(), center = here, style = Stroke(width = 1.dp.toPx()))
            }
        }
        Spacer(Modifier.height(10.dp))
        Legend()
    }
}

@Composable
private fun Legend() {
    val entries = listOf(
        YallopCategory.A to "A · gözle kolay",
        YallopCategory.B to "B · gözle, ideal şartta",
        YallopCategory.C to "C · önce dürbünle",
        YallopCategory.D to "D · yalnız dürbünle",
        YallopCategory.E to "E · teleskopla bile zor",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        entries.chunked(2).forEachIndexed { index, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                row.forEach { (category, label) -> LegendItem(category, label) }
                if (index == 2) MoonFirstLegendItem()
            }
        }
    }
}

@Composable
private fun LegendItem(category: YallopCategory, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = categoryAlpha(category).coerceAtLeast(0.06f)))
                .border(1.dp, Palette.HairlineStrong, RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(5.dp))
        Text(label, style = RasadType.caption, color = Palette.TextMuted, maxLines = 1)
    }
}

@Composable
private fun MoonFirstLegendItem() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Palette.HairlineStrong, RoundedCornerShape(3.dp)),
        )
        Spacer(Modifier.width(5.dp))
        Text("Ay, Güneş'ten önce batar", style = RasadType.caption, color = Palette.TextMuted, maxLines = 1)
    }
}
