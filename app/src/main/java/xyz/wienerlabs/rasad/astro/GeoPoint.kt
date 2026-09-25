package xyz.wienerlabs.rasad.astro

import io.github.cosinekitty.astronomy.Observer
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class GeoPoint(
    val latitude: Double,
    val longitude: Double,
    val heightMeters: Double = 0.0,
) {
    fun toObserver(): Observer = Observer(latitude, longitude, heightMeters)

    fun formatted(): String {
        val latHemisphere = if (latitude >= 0) "K" else "G"
        val lonHemisphere = if (longitude >= 0) "D" else "B"
        return "%.2f° %s, %.2f° %s".format(TurkishLocale, abs(latitude), latHemisphere, abs(longitude), lonHemisphere)
    }

    companion object {
        val Istanbul = GeoPoint(41.0082, 28.9784, 40.0)
        val Kaaba = GeoPoint(21.422487, 39.826206, 277.0)
    }
}

object Qibla {
    private const val EARTH_RADIUS_KM = 6371.0088

    fun bearingDegrees(from: GeoPoint, to: GeoPoint = GeoPoint.Kaaba): Double {
        val phi1 = Math.toRadians(from.latitude)
        val phi2 = Math.toRadians(to.latitude)
        val deltaLambda = Math.toRadians(to.longitude - from.longitude)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun distanceKm(from: GeoPoint, to: GeoPoint = GeoPoint.Kaaba): Double {
        val phi1 = Math.toRadians(from.latitude)
        val phi2 = Math.toRadians(to.latitude)
        val deltaPhi = phi2 - phi1
        val deltaLambda = Math.toRadians(to.longitude - from.longitude)
        val a = sin(deltaPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(deltaLambda / 2).pow(2)
        return 2 * EARTH_RADIUS_KM * asin(min(1.0, sqrt(a)))
    }
}
