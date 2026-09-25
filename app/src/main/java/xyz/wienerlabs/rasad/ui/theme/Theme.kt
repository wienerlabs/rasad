package xyz.wienerlabs.rasad.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import xyz.wienerlabs.rasad.R

object Palette {
    val Ink = Color(0xFF05070B)
    val Panel = Color(0xF00A0D13)
    val PanelSoft = Color(0xB80A0D13)
    val Hairline = Color(0x1FF4F2EC)
    val HairlineStrong = Color(0x40F4F2EC)
    val Text = Color(0xFFF4F2EC)
    val TextMuted = Color(0xB8F4F2EC)
    val TextFaint = Color(0x80F4F2EC)
    val Fill = Color(0x14F4F2EC)
    val FillStrong = Color(0x29F4F2EC)
}

val FunnelDisplay = FontFamily(
    Font(R.font.funnel_display, FontWeight.Light),
    Font(R.font.funnel_display, FontWeight.Normal),
    Font(R.font.funnel_display, FontWeight.Medium),
    Font(R.font.funnel_display, FontWeight.SemiBold),
)

val FunnelSans = FontFamily(
    Font(R.font.funnel_sans, FontWeight.Normal),
    Font(R.font.funnel_sans, FontWeight.Medium),
    Font(R.font.funnel_sans, FontWeight.SemiBold),
)

val Amiri = FontFamily(Font(R.font.amiri_regular))

object RasadType {
    val hero = TextStyle(fontFamily = FunnelDisplay, fontWeight = FontWeight.Light, fontSize = 44.sp, lineHeight = 46.sp, letterSpacing = (-0.03).em)
    val display = TextStyle(fontFamily = FunnelDisplay, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.02).em)
    val title = TextStyle(fontFamily = FunnelDisplay, fontWeight = FontWeight.Medium, fontSize = 22.sp, lineHeight = 27.sp, letterSpacing = (-0.01).em)
    val heading = TextStyle(fontFamily = FunnelDisplay, fontWeight = FontWeight.Medium, fontSize = 17.sp, lineHeight = 22.sp)
    val body = TextStyle(fontFamily = FunnelSans, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp)
    val label = TextStyle(fontFamily = FunnelSans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 17.sp, letterSpacing = 0.02.em)
    val caption = TextStyle(fontFamily = FunnelSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.02.em)
    val clock = TextStyle(fontFamily = FunnelDisplay, fontWeight = FontWeight.Light, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em, fontFeatureSettings = "tnum")
    val value = TextStyle(fontFamily = FunnelDisplay, fontWeight = FontWeight.Normal, fontSize = 19.sp, lineHeight = 24.sp, fontFeatureSettings = "tnum")
    val arabic = TextStyle(fontFamily = Amiri, fontSize = 28.sp, lineHeight = 44.sp, textDirection = TextDirection.Rtl)
}

@Composable
fun RasadTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Palette.Text,
            onPrimary = Palette.Ink,
            background = Palette.Ink,
            onBackground = Palette.Text,
            surface = Palette.Panel,
            onSurface = Palette.Text,
        ),
        content = content,
    )
}
