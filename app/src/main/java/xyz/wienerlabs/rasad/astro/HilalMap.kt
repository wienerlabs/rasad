package xyz.wienerlabs.rasad.astro

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.KM_PER_AU
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.geoVector
import io.github.cosinekitty.astronomy.rotationEqjEqd
import io.github.cosinekitty.astronomy.siderealTime
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class VisibilityGrid(
    val date: LocalDate,
    val lonStart: Double,
    val latStart: Double,
    val step: Double,
    val columns: Int,
    val rows: Int,
    val categories: ByteArray,
) {
    fun categoryAt(column: Int, row: Int): YallopCategory? {
        val value = categories[row * columns + column].toInt()
        return if (value < 0) null else YallopCategory.entries[value]
    }

    fun moonSetsFirstAt(column: Int, row: Int): Boolean = categories[row * columns + column].toInt() == HilalMap.MOON_SETS_FIRST

    fun categoryNear(latitude: Double, longitude: Double): YallopCategory? {
        val column = ((longitude - lonStart) / step).let { Math.round(it).toInt() }.coerceIn(0, columns - 1)
        val row = ((latitude - latStart) / step).let { Math.round(it).toInt() }.coerceIn(0, rows - 1)
        return categoryAt(column, row)
    }
}

object HilalMap {
    const val UNDETERMINED = -1
    const val MOON_SETS_FIRST = -2
    private const val SAMPLE_MINUTES = 10L
    private const val SUNSET_ALTITUDE = -0.8333
    private const val REFRACTION_AT_HORIZON = 0.5667
    private const val MOON_RADIUS_KM = 1737.4
    private const val FLATTENING_RATIO = 0.99664719

    private class Samples(
        val startMillis: Long,
        val count: Int,
        val sunX: DoubleArray, val sunY: DoubleArray, val sunZ: DoubleArray,
        val moonX: DoubleArray, val moonY: DoubleArray, val moonZ: DoubleArray,
        val cosTheta: DoubleArray, val sinTheta: DoubleArray, val theta: DoubleArray,
    ) {
        fun millisAt(index: Double): Long = startMillis + (index * SAMPLE_MINUTES * 60_000.0).toLong()
        fun indexAt(millis: Long): Double = (millis - startMillis) / (SAMPLE_MINUTES * 60_000.0)
    }

    fun compute(
        date: LocalDate,
        conjunctionMillis: Long,
        step: Double = 2.0,
        latMin: Double = -60.0,
        latMax: Double = 60.0,
    ): VisibilityGrid {
        val samples = sample(date)
        val columns = (360.0 / step).toInt() + 1
        val rows = ((latMax - latMin) / step).toInt() + 1
        val categories = ByteArray(columns * rows)
        val utcNoon = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        for (row in 0 until rows) {
            val latitude = latMin + row * step
            for (column in 0 until columns) {
                val longitude = -180.0 + column * step
                val localNoon = utcNoon - (longitude / 15.0 * 3_600_000.0).toLong()
                val category = evaluate(samples, latitude, longitude, localNoon, conjunctionMillis)
                categories[row * columns + column] = (category ?: UNDETERMINED).toByte()
            }
        }
        return VisibilityGrid(date, -180.0, latMin, step, columns, rows, categories)
    }

    fun evaluateAt(date: LocalDate, conjunctionMillis: Long, location: GeoPoint): Pair<YallopCategory?, Double?> {
        val samples = sample(date)
        val utcNoon = date.atTime(12, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val localNoon = utcNoon - (location.longitude / 15.0 * 3_600_000.0).toLong()
        var lastQ: Double? = null
        val code = evaluate(samples, location.latitude, location.longitude, localNoon, conjunctionMillis) { lastQ = it }
        val category = when {
            code == null -> null
            code == MOON_SETS_FIRST -> YallopCategory.F
            else -> YallopCategory.entries[code]
        }
        return category to lastQ
    }

    private fun sample(date: LocalDate): Samples {
        val start = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli() - 2L * 3_600_000L
        val count = (40 * 60 / SAMPLE_MINUTES).toInt() + 1
        val sunX = DoubleArray(count); val sunY = DoubleArray(count); val sunZ = DoubleArray(count)
        val moonX = DoubleArray(count); val moonY = DoubleArray(count); val moonZ = DoubleArray(count)
        val cosTheta = DoubleArray(count); val sinTheta = DoubleArray(count); val theta = DoubleArray(count)
        val precession = rotationEqjEqd(Time.fromMillisecondsSince1970(start + 20L * 3_600_000L))
        for (index in 0 until count) {
            val time = Time.fromMillisecondsSince1970(start + index * SAMPLE_MINUTES * 60_000L)
            val sun = precession.rotate(geoVector(Body.Sun, time, Aberration.Corrected))
            val sunLength = sun.length()
            sunX[index] = sun.x / sunLength; sunY[index] = sun.y / sunLength; sunZ[index] = sun.z / sunLength
            val moon = precession.rotate(geoVector(Body.Moon, time, Aberration.Corrected))
            moonX[index] = moon.x * KM_PER_AU; moonY[index] = moon.y * KM_PER_AU; moonZ[index] = moon.z * KM_PER_AU
            val angle = Math.toRadians(siderealTime(time) * 15.0)
            theta[index] = angle
            cosTheta[index] = cos(angle)
            sinTheta[index] = sin(angle)
        }
        return Samples(start, count, sunX, sunY, sunZ, moonX, moonY, moonZ, cosTheta, sinTheta, theta)
    }

    private fun evaluate(
        samples: Samples,
        latitude: Double,
        longitude: Double,
        localNoon: Long,
        conjunctionMillis: Long,
        onQ: (Double) -> Unit = {},
    ): Int? {
        val phi = Math.toRadians(latitude)
        val lambda = Math.toRadians(longitude)
        val cosPhi = cos(phi); val sinPhi = sin(phi)
        val cosLambda = cos(lambda); val sinLambda = sin(lambda)
        val reduced = atan(FLATTENING_RATIO * tan(phi))
        val rhoCos = cos(reduced) * Yallop.EARTH_EQUATORIAL_RADIUS_KM
        val rhoSin = FLATTENING_RATIO * sin(reduced) * Yallop.EARTH_EQUATORIAL_RADIUS_KM
        val sunsetSine = sin(Math.toRadians(SUNSET_ALTITUDE))

        fun zenith(index: Int, out: DoubleArray) {
            val cosLocal = samples.cosTheta[index] * cosLambda - samples.sinTheta[index] * sinLambda
            val sinLocal = samples.sinTheta[index] * cosLambda + samples.cosTheta[index] * sinLambda
            out[0] = cosPhi * cosLocal; out[1] = cosPhi * sinLocal; out[2] = sinPhi
            out[3] = rhoCos * cosLocal; out[4] = rhoCos * sinLocal; out[5] = rhoSin
        }

        val frame = DoubleArray(6)
        fun sunSine(index: Int): Double {
            zenith(index, frame)
            return samples.sunX[index] * frame[0] + samples.sunY[index] * frame[1] + samples.sunZ[index] * frame[2]
        }

        fun moonSetMargin(index: Int): Double {
            zenith(index, frame)
            val tx = samples.moonX[index] - frame[3]
            val ty = samples.moonY[index] - frame[4]
            val tz = samples.moonZ[index] - frame[5]
            val distance = sqrt(tx * tx + ty * ty + tz * tz)
            val altitude = Math.toDegrees(asin(((tx * frame[0] + ty * frame[1] + tz * frame[2]) / distance).coerceIn(-1.0, 1.0)))
            val semiDiameter = Math.toDegrees(atan(MOON_RADIUS_KM / distance))
            return altitude + REFRACTION_AT_HORIZON + semiDiameter
        }

        val startIndex = samples.indexAt(localNoon).toInt().coerceIn(0, samples.count - 2)
        var sunsetIndex = Double.NaN
        var previous = sunSine(startIndex) - sunsetSine
        for (index in startIndex + 1 until samples.count) {
            val current = sunSine(index) - sunsetSine
            if (previous >= 0 && current < 0) {
                sunsetIndex = index - 1 + previous / (previous - current)
                break
            }
            previous = current
        }
        if (sunsetIndex.isNaN()) return null

        var moonsetIndex = Double.NaN
        var previousMargin = moonSetMargin(startIndex)
        for (index in startIndex + 1 until samples.count) {
            val margin = moonSetMargin(index)
            if (previousMargin >= 0 && margin < 0) {
                moonsetIndex = index - 1 + previousMargin / (previousMargin - margin)
                break
            }
            previousMargin = margin
        }
        if (moonsetIndex.isNaN()) return null
        if (moonsetIndex <= sunsetIndex) return MOON_SETS_FIRST

        val bestIndex = sunsetIndex + (moonsetIndex - sunsetIndex) * 4.0 / 9.0
        if (samples.millisAt(bestIndex) < conjunctionMillis) return MOON_SETS_FIRST

        val lower = bestIndex.toInt().coerceIn(0, samples.count - 2)
        val fraction = bestIndex - lower
        fun lerp(values: DoubleArray) = values[lower] + (values[lower + 1] - values[lower]) * fraction

        var theta = samples.theta[lower + 1] - samples.theta[lower]
        if (theta < 0) theta += 2 * Math.PI
        val localTheta = samples.theta[lower] + theta * fraction + lambda
        val zx = cosPhi * cos(localTheta); val zy = cosPhi * sin(localTheta); val zz = sinPhi

        val sx = lerp(samples.sunX); val sy = lerp(samples.sunY); val sz = lerp(samples.sunZ)
        val sunLength = sqrt(sx * sx + sy * sy + sz * sz)
        val mx = lerp(samples.moonX); val my = lerp(samples.moonY); val mz = lerp(samples.moonZ)
        val moonDistance = sqrt(mx * mx + my * my + mz * mz)

        val sunAltitude = Math.toDegrees(asin(((sx * zx + sy * zy + sz * zz) / sunLength).coerceIn(-1.0, 1.0)))
        val moonAltitude = Math.toDegrees(asin(((mx * zx + my * zy + mz * zz) / moonDistance).coerceIn(-1.0, 1.0)))
        val arcv = moonAltitude - sunAltitude

        val crossX = sy * mz - sz * my
        val crossY = sz * mx - sx * mz
        val crossZ = sx * my - sy * mx
        val arcl = Math.toDegrees(atan2(sqrt(crossX * crossX + crossY * crossY + crossZ * crossZ), sx * mx + sy * my + sz * mz))

        val width = Yallop.topocentricWidth(moonDistance, moonAltitude, arcl)
        val q = Yallop.q(arcv, width)
        onQ(q)
        return YallopCategory.fromQ(q).ordinal
    }
}
