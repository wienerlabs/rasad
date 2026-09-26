package xyz.wienerlabs.rasad.islam

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EclipseKind
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.nextLocalSolarEclipse
import io.github.cosinekitty.astronomy.nextLunarEclipse
import io.github.cosinekitty.astronomy.searchLocalSolarEclipse
import io.github.cosinekitty.astronomy.searchLunarEclipse
import xyz.wienerlabs.rasad.astro.GeoPoint

enum class EclipseBody(val title: String) {
    Sun("Güneş tutulması"),
    Moon("Ay tutulması"),
}

enum class EclipseType(val title: String) {
    Penumbral("Yarıgölge"),
    Partial("Parçalı"),
    Annular("Halkalı"),
    Total("Tam"),
}

data class LocalEclipse(
    val body: EclipseBody,
    val type: EclipseType,
    val begin: Long,
    val peak: Long,
    val end: Long,
    val peakAltitude: Double,
    val obscuration: Double,
) {
    val noticeable: Boolean get() = type != EclipseType.Penumbral
}

object EclipseFinder {
    private const val MINUTE_MILLIS = 60_000L
    private const val YEAR_MILLIS = 365.25 * 86_400_000.0
    private const val SAMPLES = 12

    fun upcoming(location: GeoPoint, fromMillis: Long, years: Double = 3.0): List<LocalEclipse> {
        val observer = location.toObserver()
        val until = fromMillis + (years * YEAR_MILLIS).toLong()
        return (lunar(observer, fromMillis, until) + solar(observer, fromMillis, until)).sortedBy { it.peak }
    }

    private fun lunar(observer: Observer, fromMillis: Long, until: Long): List<LocalEclipse> {
        val found = ArrayList<LocalEclipse>()
        var eclipse = searchLunarEclipse(Time.fromMillisecondsSince1970(fromMillis))
        while (eclipse.peak.toMillisecondsSince1970() < until) {
            val peak = eclipse.peak.toMillisecondsSince1970()
            val halfSpan = (if (eclipse.sdPartial > 0.0) eclipse.sdPartial else eclipse.sdPenum) * MINUTE_MILLIS
            val begin = peak - halfSpan.toLong()
            val end = peak + halfSpan.toLong()
            val visible = (0..SAMPLES).any { step -> moonAltitude(observer, begin + (end - begin) * step / SAMPLES) > 0.0 }
            if (visible) {
                found += LocalEclipse(
                    body = EclipseBody.Moon,
                    type = type(eclipse.kind),
                    begin = begin,
                    peak = peak,
                    end = end,
                    peakAltitude = moonAltitude(observer, peak),
                    obscuration = eclipse.obscuration,
                )
            }
            eclipse = nextLunarEclipse(eclipse.peak)
        }
        return found
    }

    private fun solar(observer: Observer, fromMillis: Long, until: Long): List<LocalEclipse> {
        val found = ArrayList<LocalEclipse>()
        var eclipse = searchLocalSolarEclipse(Time.fromMillisecondsSince1970(fromMillis), observer)
        while (eclipse.peak.time.toMillisecondsSince1970() < until) {
            val visible = eclipse.partialBegin.altitude > 0.0 || eclipse.peak.altitude > 0.0 || eclipse.partialEnd.altitude > 0.0
            if (visible) {
                found += LocalEclipse(
                    body = EclipseBody.Sun,
                    type = type(eclipse.kind),
                    begin = eclipse.partialBegin.time.toMillisecondsSince1970(),
                    peak = eclipse.peak.time.toMillisecondsSince1970(),
                    end = eclipse.partialEnd.time.toMillisecondsSince1970(),
                    peakAltitude = eclipse.peak.altitude,
                    obscuration = eclipse.obscuration,
                )
            }
            eclipse = nextLocalSolarEclipse(eclipse.peak.time, observer)
        }
        return found
    }

    private fun moonAltitude(observer: Observer, millis: Long): Double {
        val time = Time.fromMillisecondsSince1970(millis)
        val position = equator(Body.Moon, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        return horizon(time, observer, position.ra, position.dec, Refraction.Normal).altitude
    }

    private fun type(kind: EclipseKind): EclipseType = when (kind) {
        EclipseKind.Penumbral -> EclipseType.Penumbral
        EclipseKind.Partial -> EclipseType.Partial
        EclipseKind.Annular -> EclipseType.Annular
        EclipseKind.Total -> EclipseType.Total
    }
}
