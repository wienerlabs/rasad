package xyz.wienerlabs.rasad.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object RasadIcons {
    private fun icon(name: String, filled: Boolean = false, block: PathBuilder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            if (filled) {
                path(fill = SolidColor(Color.White), pathBuilder = block)
            } else {
                path(
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.6f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathBuilder = block,
                )
            }
        }.build()

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx - r, cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx + r, y1 = cy)
        arcTo(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, x1 = cx - r, y1 = cy)
        close()
    }

    val Sensor = icon("sensor") {
        moveTo(8f, 3f); lineTo(16f, 3f)
        arcTo(1.5f, 1.5f, 0f, false, true, 17.5f, 4.5f)
        lineTo(17.5f, 19.5f)
        arcTo(1.5f, 1.5f, 0f, false, true, 16f, 21f)
        lineTo(8f, 21f)
        arcTo(1.5f, 1.5f, 0f, false, true, 6.5f, 19.5f)
        lineTo(6.5f, 4.5f)
        arcTo(1.5f, 1.5f, 0f, false, true, 8f, 3f)
        close()
        moveTo(12f, 8f); lineTo(14f, 12f); lineTo(12f, 16f); lineTo(10f, 12f); close()
    }

    val Hand = icon("hand") {
        moveTo(12f, 3f); lineTo(12f, 21f)
        moveTo(3f, 12f); lineTo(21f, 12f)
        moveTo(9.5f, 5.5f); lineTo(12f, 3f); lineTo(14.5f, 5.5f)
        moveTo(9.5f, 18.5f); lineTo(12f, 21f); lineTo(14.5f, 18.5f)
        moveTo(5.5f, 9.5f); lineTo(3f, 12f); lineTo(5.5f, 14.5f)
        moveTo(18.5f, 9.5f); lineTo(21f, 12f); lineTo(18.5f, 14.5f)
    }

    val Clock = icon("clock") {
        circle(12f, 12f, 8.5f)
        moveTo(12f, 7.5f); lineTo(12f, 12f); lineTo(15f, 14f)
    }

    val Search = icon("search") {
        circle(10.5f, 10.5f, 6f)
        moveTo(15f, 15f); lineTo(20f, 20f)
    }

    val Moon = icon("moon") {
        moveTo(15.5f, 3.8f)
        arcTo(8.5f, 8.5f, 0f, isMoreThanHalf = true, isPositiveArc = false, x1 = 20.2f, y1 = 15.5f)
        arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 15.5f, y1 = 3.8f)
        close()
    }

    val Layers = icon("layers") {
        moveTo(12f, 4f); lineTo(20f, 8.5f); lineTo(12f, 13f); lineTo(4f, 8.5f); close()
        moveTo(4f, 12.5f); lineTo(12f, 17f); lineTo(20f, 12.5f)
        moveTo(4f, 16.5f); lineTo(12f, 21f); lineTo(20f, 16.5f)
    }

    val Close = icon("close") {
        moveTo(6f, 6f); lineTo(18f, 18f)
        moveTo(18f, 6f); lineTo(6f, 18f)
    }

    val Back = icon("back") {
        moveTo(14.5f, 5f); lineTo(7.5f, 12f); lineTo(14.5f, 19f)
    }

    val Target = icon("target") {
        circle(12f, 12f, 7f)
        moveTo(12f, 2.5f); lineTo(12f, 6.5f)
        moveTo(12f, 17.5f); lineTo(12f, 21.5f)
        moveTo(2.5f, 12f); lineTo(6.5f, 12f)
        moveTo(17.5f, 12f); lineTo(21.5f, 12f)
    }

    val Eye = icon("eye") {
        moveTo(2.5f, 12f)
        quadTo(12f, 3.5f, 21.5f, 12f)
        quadTo(12f, 20.5f, 2.5f, 12f)
        close()
        circle(12f, 12f, 3f)
    }

    val Play = icon("play", filled = true) {
        moveTo(8f, 5f); lineTo(19f, 12f); lineTo(8f, 19f); close()
    }

    val Pause = icon("pause", filled = true) {
        moveTo(7f, 5f); lineTo(10.5f, 5f); lineTo(10.5f, 19f); lineTo(7f, 19f); close()
        moveTo(13.5f, 5f); lineTo(17f, 5f); lineTo(17f, 19f); lineTo(13.5f, 19f); close()
    }

    val Pin = icon("pin") {
        moveTo(12f, 21f)
        quadTo(5f, 13.5f, 5f, 9.5f)
        arcTo(7f, 7f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 19f, y1 = 9.5f)
        quadTo(19f, 13.5f, 12f, 21f)
        close()
        circle(12f, 9.5f, 2.5f)
    }

    val Kaaba = icon("kaaba") {
        moveTo(5f, 7f); lineTo(19f, 7f); lineTo(19f, 20f); lineTo(5f, 20f); close()
        moveTo(5f, 10.5f); lineTo(19f, 10.5f)
    }
}
