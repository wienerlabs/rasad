package xyz.wienerlabs.rasad.sky

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import xyz.wienerlabs.rasad.astro.Ephemeris
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.Vec3
import xyz.wienerlabs.rasad.astro.transform
import kotlin.math.abs
import kotlin.math.asin

data class ViewingPlan(
    val neverRises: Boolean,
    val alwaysUp: Boolean,
    val nextRise: Long?,
    val bestTime: Long?,
    val bestAltitude: Double,
    val bestInDarkness: Boolean,
)

object VisibilityPlanner {
    private const val STEP_MINUTES = 10
    private const val STEP_MILLIS = STEP_MINUTES * 60_000L
    private const val DARK_SUN_ALTITUDE = -12.0
    private const val DUSK_SUN_ALTITUDE = -6.0
    private const val GOOD_ALTITUDE = 15.0
    private const val ROUNDING_MILLIS = 5 * 60_000L

    fun plan(equatorial: Vec3, location: GeoPoint, fromMillis: Long, hours: Int = 24): ViewingPlan {
        val declination = Math.toDegrees(asin(equatorial.z.coerceIn(-1.0, 1.0)))
        val highest = 90.0 - abs(location.latitude - declination)
        val lowest = abs(location.latitude + declination) - 90.0
        if (highest < 0.0) {
            return ViewingPlan(neverRises = true, alwaysUp = false, nextRise = null, bestTime = null, bestAltitude = highest, bestInDarkness = false)
        }
        val observer = location.toObserver()
        val steps = hours * 60 / STEP_MINUTES
        val altitudes = DoubleArray(steps + 1)
        val sunAltitudes = DoubleArray(steps + 1)
        val rotation = DoubleArray(9)
        for (i in 0..steps) {
            val time = Time.fromMillisecondsSince1970(fromMillis + i * STEP_MILLIS)
            Ephemeris.eqjToEnu(time, observer, rotation)
            altitudes[i] = rotation.transform(equatorial).altitudeDegrees
            sunAltitudes[i] = sunAltitude(time, observer)
        }
        var nextRise: Long? = null
        if (altitudes[0] < 0.0) {
            for (i in 1..steps) {
                if (altitudes[i - 1] < 0.0 && altitudes[i] >= 0.0) {
                    val fraction = -altitudes[i - 1] / (altitudes[i] - altitudes[i - 1])
                    nextRise = fromMillis + ((i - 1 + fraction) * STEP_MILLIS).toLong()
                    break
                }
            }
        }
        val dark = bestIndex(altitudes, sunAltitudes, DARK_SUN_ALTITUDE, GOOD_ALTITUDE)
        val dusk = if (dark < 0) bestIndex(altitudes, sunAltitudes, DUSK_SUN_ALTITUDE, GOOD_ALTITUDE / 2) else -1
        val any = if (dark < 0 && dusk < 0) bestIndex(altitudes, sunAltitudes, 90.0, 0.0) else -1
        val chosen = listOf(dark, dusk, any).firstOrNull { it >= 0 }
        return ViewingPlan(
            neverRises = false,
            alwaysUp = lowest > 0.0,
            nextRise = nextRise,
            bestTime = chosen?.let { round(fromMillis + it * STEP_MILLIS) },
            bestAltitude = chosen?.let { altitudes[it] } ?: highest,
            bestInDarkness = dark >= 0 || dusk >= 0,
        )
    }

    private fun bestIndex(altitudes: DoubleArray, sunAltitudes: DoubleArray, sunLimit: Double, altitudeLimit: Double): Int {
        var best = -1
        for (i in altitudes.indices) {
            if (sunAltitudes[i] > sunLimit || altitudes[i] < altitudeLimit) continue
            if (best < 0 || altitudes[i] > altitudes[best] + 0.05) best = i
        }
        return best
    }

    private fun round(millis: Long): Long = (millis + ROUNDING_MILLIS / 2) / ROUNDING_MILLIS * ROUNDING_MILLIS

    private fun sunAltitude(time: Time, observer: Observer): Double {
        val equatorial = equator(Body.Sun, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        return horizon(time, observer, equatorial.ra, equatorial.dec, Refraction.Normal).altitude
    }
}
