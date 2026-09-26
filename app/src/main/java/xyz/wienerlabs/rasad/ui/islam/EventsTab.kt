package xyz.wienerlabs.rasad.ui.islam

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.Qibla
import xyz.wienerlabs.rasad.astro.SkyBody
import xyz.wienerlabs.rasad.islam.EclipseBody
import xyz.wienerlabs.rasad.islam.EclipseFinder
import xyz.wienerlabs.rasad.islam.LocalEclipse
import xyz.wienerlabs.rasad.islam.QiblaSun
import xyz.wienerlabs.rasad.islam.QiblaSunEvent
import xyz.wienerlabs.rasad.sky.SkyObjectRef
import xyz.wienerlabs.rasad.sky.SkyPresentation
import xyz.wienerlabs.rasad.ui.RasadIcons
import xyz.wienerlabs.rasad.ui.components.Hairline
import xyz.wienerlabs.rasad.ui.components.Pill
import xyz.wienerlabs.rasad.ui.components.SectionLabel
import xyz.wienerlabs.rasad.ui.components.panel
import xyz.wienerlabs.rasad.ui.theme.Palette
import xyz.wienerlabs.rasad.ui.theme.RasadType
import kotlin.math.cos
import kotlin.math.sin

private const val DAY_MILLIS = 86_400_000L

@Composable
fun EventsTab(millis: Long, location: GeoPoint, onShowInSky: (SkyPresentation) -> Unit) {
    val dayKey = millis / DAY_MILLIS
    val qiblaSun by produceState<QiblaSunEvent?>(null, dayKey) {
        value = withContext(Dispatchers.Default) { runCatching { QiblaSun.next(millis) }.getOrNull() }
    }
    val eclipses by produceState<List<LocalEclipse>?>(null, dayKey, location) {
        value = withContext(Dispatchers.Default) { runCatching { EclipseFinder.upcoming(location, millis, 3.0) }.getOrNull() }
    }
    Column {
        SectionLabel("Kıble güneşi")
        Spacer(Modifier.height(8.dp))
        val event = qiblaSun
        if (event == null) {
            Text("Güneş'in Kâbe üzerinden geçişi hesaplanıyor…", style = RasadType.body, color = Palette.TextMuted)
        } else {
            QiblaSunCard(event, millis, location, onShowInSky)
        }
        Spacer(Modifier.height(10.dp))
        SourceChips(listOf("2:144"))

        Spacer(Modifier.height(28.dp))
        SectionLabel("Güneş ve Ay tutulmaları")
        Spacer(Modifier.height(8.dp))
        NoteBlock(
            "Tutulma Allah'ın âyetlerindendir",
            "Güneş ve Ay, Allah'ın âyetlerindendir; kimsenin ölümü ya da hayatı sebebiyle tutulmazlar. Tutulma görüldüğünde dua edilir, tekbir getirilir, namaz kılınır ve sadaka verilir. Hadiste namaz, tutulmanın görülmesine bağlanmıştır; yarıgölge tutulmalar çoğu zaman gözle fark edilmez.",
        )
        Spacer(Modifier.height(10.dp))
        SourceChips(listOf("bukhari:1044"))
        Spacer(Modifier.height(14.dp))
        val list = eclipses
        when {
            list == null -> Text("Önümüzdeki üç yılın tutulmaları hesaplanıyor…", style = RasadType.body, color = Palette.TextMuted)
            list.isEmpty() -> Text("Önümüzdeki üç yılda bu konumdan görülecek bir tutulma yok.", style = RasadType.body, color = Palette.TextMuted)
            else -> list.forEachIndexed { index, eclipse ->
                if (index > 0) Hairline()
                EclipseRow(eclipse, onShowInSky)
            }
        }
    }
}

@Composable
private fun QiblaSunCard(event: QiblaSunEvent, now: Long, location: GeoPoint, onShowInSky: (SkyPresentation) -> Unit) {
    val sun = remember(event, location) { QiblaSun.sunAt(event.millis, location) }
    val qibla = remember(location) { Qibla.bearingDegrees(location) }
    Column(Modifier.fillMaxWidth().panel(RoundedCornerShape(18.dp)).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(Formats.dayMonthYear(event.millis), style = RasadType.heading, color = Palette.Text)
                Text("${Formats.weekday(event.millis)} · ${Formats.clock(event.millis)}", style = RasadType.value, color = Palette.Text)
                Text(untilText(event.millis - now), style = RasadType.caption, color = Palette.TextFaint)
            }
            Spacer(Modifier.width(12.dp))
            ShadowDial(qibla, Modifier.size(96.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Bu anda Güneş, Kâbe'nin tam üstündedir. Güneş'e dönen kişi kıbleye dönmüş olur; dik duran bir cismin gölgesi de kıbleye tam ters yönü gösterir. Pusulaya ihtiyaç duymadan kıbleyi doğrulamanın en basit yollarından biridir.",
            style = RasadType.body,
            color = Palette.TextMuted,
        )
        Spacer(Modifier.height(8.dp))
        val usable = sun.altitude > 5.0
        Text(
            if (usable) {
                "Bulunduğun yerde Güneş o anda ${Formats.degrees(sun.azimuth, 1)} yönünde, ${Formats.degrees(sun.altitude)} yükseklikte olacak; kıble açın ${Formats.degrees(qibla, 1)}."
            } else {
                "Bu anda Güneş senin ufkunun altında olacağı için bu yöntem bulunduğun yerde kullanılamaz."
            },
            style = RasadType.caption,
            color = Palette.TextFaint,
        )
        Spacer(Modifier.height(12.dp))
        Pill("Gökte göster", icon = RasadIcons.Eye, onClick = { onShowInSky(SkyPresentation(event.millis, target = SkyObjectRef.Body(SkyBody.Sun), fov = 90.0)) })
    }
}

private fun untilText(delta: Long): String {
    val days = delta / DAY_MILLIS
    return if (days >= 1) "$days gün sonra" else "${Formats.duration(delta)} sonra"
}

@Composable
private fun ShadowDial(qiblaBearing: Double, modifier: Modifier = Modifier) {
    val ink = Palette.Text
    val faint = Palette.HairlineStrong
    Canvas(modifier) {
        val radius = size.minDimension / 2f - 6.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(faint, radius, center, style = Stroke(1.dp.toPx()))
        drawLine(faint, Offset(center.x, center.y - radius), Offset(center.x, center.y - radius + 7.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        val angle = Math.toRadians(qiblaBearing)
        val qibla = Offset(center.x + (sin(angle) * radius * 0.86).toFloat(), center.y - (cos(angle) * radius * 0.86).toFloat())
        val shadow = Offset(center.x - (sin(angle) * radius * 0.7).toFloat(), center.y + (cos(angle) * radius * 0.7).toFloat())
        drawLine(ink, center, qibla, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawRect(ink, topLeft = Offset(qibla.x - 4.dp.toPx(), qibla.y - 4.dp.toPx()), size = androidx.compose.ui.geometry.Size(8.dp.toPx(), 8.dp.toPx()))
        drawLine(ink.copy(alpha = 0.55f), center, shadow, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
        drawCircle(ink, 3.dp.toPx(), center)
    }
}

@Composable
private fun EclipseRow(eclipse: LocalEclipse, onShowInSky: (SkyPresentation) -> Unit) {
    val body = if (eclipse.body == EclipseBody.Sun) SkyBody.Sun else SkyBody.Moon
    Column(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
        Text("${eclipse.body.title} · ${eclipse.type.title}", style = RasadType.heading, color = Palette.Text)
        Text("${Formats.dayMonthYear(eclipse.peak)} ${Formats.weekday(eclipse.peak)}", style = RasadType.label, color = Palette.Text)
        Spacer(Modifier.height(2.dp))
        Text(
            "başlangıç ${Formats.clock(eclipse.begin)} · en büyük ${Formats.clock(eclipse.peak)} · bitiş ${Formats.clock(eclipse.end)}",
            style = RasadType.caption,
            color = Palette.TextMuted,
        )
        Text(
            listOfNotNull(
                "en büyük anda ${if (eclipse.body == EclipseBody.Sun) "Güneş" else "Ay"} ${Formats.degrees(eclipse.peakAltitude)} yükseklikte",
                if (eclipse.obscuration > 0.0) "örtülme ${Formats.percent(eclipse.obscuration)}" else null,
            ).joinToString(" · "),
            style = RasadType.caption,
            color = Palette.TextFaint,
        )
        if (!eclipse.noticeable) {
            Text("Yarıgölge tutulma: gözle fark edilmesi zordur.", style = RasadType.caption, color = Palette.TextFaint)
        }
        Spacer(Modifier.height(8.dp))
        Pill("Gökte göster", icon = RasadIcons.Eye, onClick = { onShowInSky(SkyPresentation(eclipse.peak, target = SkyObjectRef.Body(body), fov = 40.0)) })
    }
}
