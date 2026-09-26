package xyz.wienerlabs.rasad.astro

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.KM_PER_AU
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.Vector
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.moonQuartersAfter
import io.github.cosinekitty.astronomy.rotationEqjHor
import io.github.cosinekitty.astronomy.searchMoonPhase
import io.github.cosinekitty.astronomy.searchRiseSet
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

enum class YallopCategory(val code: String, val title: String, val detail: String, val strength: Float) {
    A("A", "Çıplak gözle kolayca görülmesi beklenir", "Hilalin gün batımından sonra rahatça seçilmesi beklenir.", 1.0f),
    B("B", "İdeal şartlarda çıplak gözle görülmesi beklenir", "Temiz ve açık bir batı ufku gerekir.", 0.72f),
    C("C", "Önce dürbünle aranmalı", "Yeri bulunduktan sonra gözle de seçilebilir.", 0.48f),
    D("D", "Dürbün ya da teleskopla görülmesi beklenir", "Çıplak gözle görülmesi beklenmez.", 0.28f),
    E("E", "Teleskopla bile görülmesi beklenmez", "Hilal Danjon sınırına çok yakın.", 0.12f),
    F("F", "Görülmesi beklenmez", "Ay, hesaplanan görünürlük sınırının altında kalıyor.", 0.0f);

    val isNakedEye: Boolean get() = this == A || this == B

    companion object {
        fun fromQ(q: Double): YallopCategory = when {
            q > 0.216 -> A
            q > -0.014 -> B
            q > -0.160 -> C
            q > -0.232 -> D
            q > -0.293 -> E
            else -> F
        }
    }
}

enum class CrescentStatus { Computed, MoonSetsFirst, BeforeConjunction, NoSunset, Undetermined }

data class CrescentEvening(
    val date: LocalDate,
    val conjunctionMillis: Long,
    val status: CrescentStatus,
    val category: YallopCategory,
    val sunsetMillis: Long? = null,
    val moonsetMillis: Long? = null,
    val bestTimeMillis: Long? = null,
    val q: Double? = null,
    val arcv: Double? = null,
    val arcl: Double? = null,
    val daz: Double? = null,
    val widthArcMinutes: Double? = null,
    val lagMinutes: Double? = null,
    val moonAgeHours: Double? = null,
    val sunAzimuth: Double? = null,
    val sunAltitude: Double? = null,
    val moonAzimuth: Double? = null,
    val moonAltitude: Double? = null,
    val illumination: Double? = null,
)

data class LunationInfo(
    val millis: Long,
    val phaseAngle: Double,
    val phaseName: String,
    val illumination: Double,
    val ageDays: Double,
    val previousNewMoon: Long,
    val nextNewMoon: Long,
    val nextQuarters: List<Pair<Int, Long>>,
)

object Yallop {
    fun q(arcv: Double, widthArcMinutes: Double): Double {
        val w = widthArcMinutes
        val threshold = 11.8371 - 6.3226 * w + 0.7319 * w * w - 0.1018 * w * w * w
        return (arcv - threshold) / 10.0
    }

    fun topocentricWidth(moonDistanceKm: Double, geocentricAltitude: Double, elongation: Double): Double {
        val parallax = asin(EARTH_EQUATORIAL_RADIUS_KM / moonDistanceKm)
        val semiDiameterArcMin = Math.toDegrees(0.27245 * parallax) * 60.0
        val topocentricSemiDiameter = semiDiameterArcMin * (1.0 + sin(Math.toRadians(geocentricAltitude)) * sin(parallax))
        return topocentricSemiDiameter * (1.0 - cos(Math.toRadians(elongation)))
    }

    const val EARTH_EQUATORIAL_RADIUS_KM = 6378.1366
}

object Hilal {
    private const val DAY_MILLIS = 86_400_000L
    private const val MAX_LAG_MILLIS = 12L * 3_600_000L
    private const val SYNODIC_MONTH_DAYS = 29.530588853

    fun nextConjunction(fromMillis: Long): Long {
        val found = searchMoonPhase(0.0, Time.fromMillisecondsSince1970(fromMillis), 40.0)
            ?: error("Kavuşum bulunamadı")
        return found.toMillisecondsSince1970()
    }

    fun previousConjunction(beforeMillis: Long): Long {
        val phase = moonPhase(Time.fromMillisecondsSince1970(beforeMillis))
        val approximateAgeDays = phase / 360.0 * SYNODIC_MONTH_DAYS
        val searchStart = beforeMillis - ((approximateAgeDays + 2.0) * DAY_MILLIS).toLong()
        val candidate = nextConjunction(searchStart)
        return if (candidate <= beforeMillis) candidate else nextConjunction(searchStart - 30L * DAY_MILLIS)
    }

    fun lunation(millis: Long): LunationInfo {
        val time = Time.fromMillisecondsSince1970(millis)
        val phase = moonPhase(time)
        val previous = previousConjunction(millis)
        val next = nextConjunction(millis)
        val quarters = moonQuartersAfter(time).take(4).map { it.quarter to it.time.toMillisecondsSince1970() }.toList()
        return LunationInfo(
            millis = millis,
            phaseAngle = phase,
            phaseName = phaseName(phase),
            illumination = illumination(Body.Moon, time).phaseFraction,
            ageDays = (millis - previous) / DAY_MILLIS.toDouble(),
            previousNewMoon = previous,
            nextNewMoon = next,
            nextQuarters = quarters,
        )
    }

    fun phaseName(phaseAngle: Double): String = when {
        phaseAngle < 6.0 || phaseAngle >= 354.0 -> "Yeni ay"
        phaseAngle < 84.0 -> "Büyüyen hilal"
        phaseAngle < 96.0 -> "İlk dördün"
        phaseAngle < 174.0 -> "Büyüyen şişkin ay"
        phaseAngle < 186.0 -> "Dolunay"
        phaseAngle < 264.0 -> "Küçülen şişkin ay"
        phaseAngle < 276.0 -> "Son dördün"
        else -> "Küçülen hilal"
    }

    fun quarterName(quarter: Int): String = when (quarter) {
        0 -> "Yeni ay"
        1 -> "İlk dördün"
        2 -> "Dolunay"
        else -> "Son dördün"
    }

    fun approximateLocalNoon(date: LocalDate, location: GeoPoint): Long {
        val utcNoon = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        return utcNoon - (location.longitude / 15.0 * 3_600_000.0).toLong()
    }

    fun eveningsAfter(conjunctionMillis: Long, location: GeoPoint, count: Int = 3): List<CrescentEvening> {
        val firstDate = localDateAt(conjunctionMillis, location)
        return (0 until count).map { offset -> assessEvening(firstDate.plusDays(offset.toLong()), location, conjunctionMillis) }
    }

    fun localDateAt(millis: Long, location: GeoPoint): LocalDate {
        val shifted = millis + (location.longitude / 15.0 * 3_600_000.0).toLong()
        return java.time.Instant.ofEpochMilli(shifted).atOffset(ZoneOffset.UTC).toLocalDate()
    }

    fun assessEvening(date: LocalDate, location: GeoPoint, conjunctionMillis: Long): CrescentEvening {
        val observer = location.toObserver()
        val noon = Time.fromMillisecondsSince1970(approximateLocalNoon(date, location))
        val sunset = searchRiseSet(Body.Sun, observer, Direction.Set, noon, 1.0)
            ?: return CrescentEvening(date, conjunctionMillis, CrescentStatus.NoSunset, YallopCategory.F)
        val moonset = searchRiseSet(Body.Moon, observer, Direction.Set, noon, 1.2)
        val sunsetMillis = sunset.toMillisecondsSince1970()
        val moonsetMillis = moonset?.toMillisecondsSince1970()
        if (moonsetMillis == null || moonsetMillis - sunsetMillis > MAX_LAG_MILLIS) {
            return CrescentEvening(date, conjunctionMillis, CrescentStatus.Undetermined, YallopCategory.F, sunsetMillis = sunsetMillis)
        }
        if (moonsetMillis <= sunsetMillis) {
            return CrescentEvening(
                date = date,
                conjunctionMillis = conjunctionMillis,
                status = CrescentStatus.MoonSetsFirst,
                category = YallopCategory.F,
                sunsetMillis = sunsetMillis,
                moonsetMillis = moonsetMillis,
                lagMinutes = (moonsetMillis - sunsetMillis) / 60_000.0,
            )
        }
        val lagMillis = moonsetMillis - sunsetMillis
        val bestMillis = sunsetMillis + lagMillis * 4 / 9
        val best = Time.fromMillisecondsSince1970(bestMillis)
        val rotation = rotationEqjHor(best, observer)
        val sunGeo = geoVector(Body.Sun, best, Aberration.Corrected)
        val moonGeo = geoVector(Body.Moon, best, Aberration.Corrected)
        val sunHor = rotation.rotate(sunGeo)
        val moonHor = rotation.rotate(moonGeo)
        val arcv = altitudeOf(moonHor) - altitudeOf(sunHor)
        val daz = normalizeSigned(azimuthOf(sunHor) - azimuthOf(moonHor))
        val arcl = Math.toDegrees(atan2(crossLength(sunGeo, moonGeo), dot(sunGeo, moonGeo)))
        val moonEquatorial = equator(Body.Moon, best, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val sunEquatorial = equator(Body.Sun, best, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val moonApparent = horizon(best, observer, moonEquatorial.ra, moonEquatorial.dec, Refraction.Normal)
        val sunApparent = horizon(best, observer, sunEquatorial.ra, sunEquatorial.dec, Refraction.Normal)
        val moonDistanceKm = moonGeo.length() * KM_PER_AU
        val width = Yallop.topocentricWidth(moonDistanceKm, altitudeOf(moonHor), arcl)
        val q = Yallop.q(arcv, width)
        val beforeConjunction = bestMillis < conjunctionMillis
        return CrescentEvening(
            date = date,
            conjunctionMillis = conjunctionMillis,
            status = if (beforeConjunction) CrescentStatus.BeforeConjunction else CrescentStatus.Computed,
            category = if (beforeConjunction) YallopCategory.F else YallopCategory.fromQ(q),
            sunsetMillis = sunsetMillis,
            moonsetMillis = moonsetMillis,
            bestTimeMillis = bestMillis,
            q = q,
            arcv = arcv,
            arcl = arcl,
            daz = daz,
            widthArcMinutes = width,
            lagMinutes = lagMillis / 60_000.0,
            moonAgeHours = (bestMillis - conjunctionMillis) / 3_600_000.0,
            sunAzimuth = sunApparent.azimuth,
            sunAltitude = sunApparent.altitude,
            moonAzimuth = moonApparent.azimuth,
            moonAltitude = moonApparent.altitude,
            illumination = illumination(Body.Moon, best).phaseFraction,
        )
    }

    fun firstVisibleEvening(conjunctionMillis: Long, location: GeoPoint, nakedEyeOnly: Boolean = true): CrescentEvening? =
        eveningsAfter(conjunctionMillis, location, 3).firstOrNull { evening ->
            if (nakedEyeOnly) evening.category.isNakedEye else evening.category <= YallopCategory.C
        }

    private fun altitudeOf(v: Vector): Double = Math.toDegrees(asin((v.z / v.length()).coerceIn(-1.0, 1.0)))

    private fun azimuthOf(v: Vector): Double = (Math.toDegrees(atan2(-v.y, v.x)) + 360.0) % 360.0

    private fun dot(a: Vector, b: Vector) = a.x * b.x + a.y * b.y + a.z * b.z

    private fun crossLength(a: Vector, b: Vector): Double {
        val x = a.y * b.z - a.z * b.y
        val y = a.z * b.x - a.x * b.z
        val z = a.x * b.y - a.y * b.x
        return sqrt(x * x + y * y + z * z)
    }

    private fun normalizeSigned(angle: Double): Double {
        var result = angle % 360.0
        if (result > 180.0) result -= 360.0
        if (result < -180.0) result += 360.0
        return result
    }

    fun widthLabel(widthArcMinutes: Double): String = "${Formats.decimal(widthArcMinutes, 2)}′"
}
