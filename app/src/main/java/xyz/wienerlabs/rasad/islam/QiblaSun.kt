package xyz.wienerlabs.rasad.islam

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.searchHourAngle
import xyz.wienerlabs.rasad.astro.GeoPoint
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap
import java.time.ZoneOffset

data class QiblaSunEvent(val millis: Long, val zenithDistanceDegrees: Double)

data class SunPosition(val azimuth: Double, val altitude: Double)

object QiblaSun {
    private const val REFINE_WINDOW_MILLIS = 30 * 60_000L
    private const val REFINE_STEP_MILLIS = 5_000L
    private val windows = listOf(5 to 17, 7 to 5)
    private const val WINDOW_DAYS = 22

    private val cache = ConcurrentHashMap<Int, List<QiblaSunEvent>>()

    fun eventsIn(year: Int): List<QiblaSunEvent> = cache.getOrPut(year) { windows.map { (month, day) -> search(LocalDate.of(year, month, day)) } }

    fun next(fromMillis: Long): QiblaSunEvent {
        val year = LocalDate.ofEpochDay(fromMillis / 86_400_000L).year
        return (eventsIn(year) + eventsIn(year + 1)).first { it.millis > fromMillis }
    }

    fun sunAt(millis: Long, location: GeoPoint): SunPosition = sunAt(millis, location.toObserver())

    private fun search(firstDay: LocalDate): QiblaSunEvent {
        val kaaba = GeoPoint.Kaaba.toObserver()
        var bestTransit = 0L
        var bestAltitude = -90.0
        for (offset in 0 until WINDOW_DAYS) {
            val start = Time.fromMillisecondsSince1970(firstDay.plusDays(offset.toLong()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
            val transit = searchHourAngle(Body.Sun, kaaba, 0.0, start)
            if (transit.hor.altitude > bestAltitude) {
                bestAltitude = transit.hor.altitude
                bestTransit = transit.time.toMillisecondsSince1970()
            }
        }
        var bestMillis = bestTransit
        var bestRefined = -90.0
        var millis = bestTransit - REFINE_WINDOW_MILLIS
        while (millis <= bestTransit + REFINE_WINDOW_MILLIS) {
            val altitude = sunAt(millis, kaaba).altitude
            if (altitude > bestRefined) {
                bestRefined = altitude
                bestMillis = millis
            }
            millis += REFINE_STEP_MILLIS
        }
        return QiblaSunEvent(bestMillis, 90.0 - bestRefined)
    }

    private fun sunAt(millis: Long, observer: Observer): SunPosition {
        val time = Time.fromMillisecondsSince1970(millis)
        val position = equator(Body.Sun, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val local = horizon(time, observer, position.ra, position.dec, Refraction.None)
        return SunPosition(local.azimuth, local.altitude)
    }
}
