package xyz.wienerlabs.rasad.astro

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.KM_PER_AU
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.constellation
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.rotationEqjHor
import kotlin.math.atan

enum class SkyBody(
    val body: Body,
    val displayName: String,
    val classicalName: String?,
    val classicalArabic: String?,
    val radiusKm: Double,
    val color: Int,
) {
    Sun(Body.Sun, "Güneş", "Şems", "شمس", 695_700.0, 0xFFFFF3DC.toInt()),
    Moon(Body.Moon, "Ay", "Kamer", "قمر", 1_737.4, 0xFFF1EEE6.toInt()),
    Mercury(Body.Mercury, "Merkür", "Utârid", "عطارد", 2_439.7, 0xFFE3DAD0.toInt()),
    Venus(Body.Venus, "Venüs", "Zühre", "زهرة", 6_051.8, 0xFFFFF7E8.toInt()),
    Mars(Body.Mars, "Mars", "Merih", "مريخ", 3_389.5, 0xFFFFB08A.toInt()),
    Jupiter(Body.Jupiter, "Jüpiter", "Müşterî", "مشتري", 69_911.0, 0xFFFFEBD0.toInt()),
    Saturn(Body.Saturn, "Satürn", "Zühal", "زحل", 58_232.0, 0xFFFFE3B0.toInt()),
    Uranus(Body.Uranus, "Uranüs", null, null, 25_362.0, 0xFFD2F3F4.toInt()),
    Neptune(Body.Neptune, "Neptün", null, null, 24_622.0, 0xFFC4D2FF.toInt()),
}

data class BodyState(
    val body: SkyBody,
    val direction: Vec3,
    val azimuth: Double,
    val altitude: Double,
    val distanceAu: Double,
    val magnitude: Double,
    val phaseFraction: Double,
    val angularRadiusDegrees: Double,
    val constellationCode: String,
) {
    val distanceKm: Double get() = distanceAu * KM_PER_AU
}

data class SkySnapshot(
    val millis: Long,
    val location: GeoPoint,
    val eqjToEnu: DoubleArray,
    val bodies: List<BodyState>,
) {
    val sun: BodyState get() = bodies.first { it.body == SkyBody.Sun }
    val moon: BodyState get() = bodies.first { it.body == SkyBody.Moon }

    fun equatorialDirection(enu: Vec3): Vec3 = eqjToEnu.transformTransposed(enu)

    override fun equals(other: Any?): Boolean = other is SkySnapshot && other.millis == millis && other.location == location

    override fun hashCode(): Int = 31 * millis.hashCode() + location.hashCode()
}

object Ephemeris {
    private const val SUN_MAGNITUDE = -26.74

    fun snapshot(millis: Long, location: GeoPoint): SkySnapshot {
        val time = Time.fromMillisecondsSince1970(millis)
        val observer = location.toObserver()
        val bodies = SkyBody.entries.map { state(it, time, observer) }
        return SkySnapshot(millis, location, eqjToEnu(time, observer), bodies)
    }

    fun eqjToEnu(time: Time, observer: Observer, out: DoubleArray = DoubleArray(9)): DoubleArray {
        val r = rotationEqjHor(time, observer).rot
        out[0] = -r[0][1]; out[1] = -r[1][1]; out[2] = -r[2][1]
        out[3] = r[0][0]; out[4] = r[1][0]; out[5] = r[2][0]
        out[6] = r[0][2]; out[7] = r[1][2]; out[8] = r[2][2]
        return out
    }

    fun state(skyBody: SkyBody, time: Time, observer: Observer): BodyState {
        val equatorial = equator(skyBody.body, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val topocentric = horizon(time, observer, equatorial.ra, equatorial.dec, Refraction.Normal)
        val j2000 = equator(skyBody.body, time, observer, EquatorEpoch.J2000, Aberration.Corrected)
        val (magnitude, phase) = if (skyBody == SkyBody.Sun) {
            SUN_MAGNITUDE to 1.0
        } else {
            val info = illumination(skyBody.body, time)
            info.mag to info.phaseFraction
        }
        val distanceKm = equatorial.dist * KM_PER_AU
        return BodyState(
            body = skyBody,
            direction = Vec3.fromAzimuthAltitude(topocentric.azimuth, topocentric.altitude),
            azimuth = topocentric.azimuth,
            altitude = topocentric.altitude,
            distanceAu = equatorial.dist,
            magnitude = magnitude,
            phaseFraction = phase,
            angularRadiusDegrees = Math.toDegrees(atan(skyBody.radiusKm / distanceKm)),
            constellationCode = constellation(j2000.ra, j2000.dec).symbol,
        )
    }
}
