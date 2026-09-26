package xyz.wienerlabs.rasad.islam

import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.searchAltitude
import io.github.cosinekitty.astronomy.searchHourAngle
import io.github.cosinekitty.astronomy.searchRiseSet
import xyz.wienerlabs.rasad.astro.GeoPoint
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.roundToInt
import kotlin.math.tan

enum class Prayer(val title: String) {
    Fajr("Fecir"),
    Sunrise("Güneş"),
    Dhuhr("Öğle"),
    Asr("İkindi"),
    Maghrib("Akşam"),
    Isha("Yatsı"),
}

enum class TwilightMethod(val title: String, val fajrAngle: Double, val ishaAngle: Double?, val ishaMinutes: Int?) {
    Diyanet("Diyanet açıları · 18° ve 17°", 18.0, 17.0, null),
    UmmAlQura("Ümmü'l-Kurâ · 18,5° ve akşamdan 90 dakika", 18.5, null, 90),
}

enum class AsrMethod(val title: String, val shadowFactor: Double) {
    Majority("Cumhur · gölge bir boy", 1.0),
    Hanafi("Hanefî · gölge iki boy", 2.0),
}

data class PrayerSettings(
    val twilight: TwilightMethod = TwilightMethod.Diyanet,
    val asr: AsrMethod = AsrMethod.Majority,
)

data class PrayerTime(val prayer: Prayer, val millis: Long?)

data class FalseDawn(val millis: Long, val date: LocalDate, val moonFree: Boolean)

data class DayPrayerTimes(val date: LocalDate, val times: List<PrayerTime>) {
    operator fun get(prayer: Prayer): Long? = times.firstOrNull { it.prayer == prayer }?.millis

    fun nextAfter(millis: Long): PrayerTime? = times.firstOrNull { it.millis != null && it.millis > millis }

    fun currentAt(millis: Long): Prayer? {
        val passed = times.lastOrNull { it.millis != null && it.millis <= millis } ?: return null
        return if (passed.prayer == Prayer.Sunrise) null else passed.prayer
    }

    fun currentAtMinute(minute: Long): Prayer? = currentAt(minute * MINUTE_MILLIS + MINUTE_MILLIS - 1)

    private companion object {
        const val MINUTE_MILLIS = 60_000L
    }
}

object PrayerCalculator {
    const val FALSE_DAWN_ALTITUDE = -25.0
    private const val DAY_MILLIS = 86_400_000.0
    private const val RAMADAN_EXTRA_MINUTES = 30
    private const val DARK_SEARCH_DAYS = 30
    private const val MOON_SET_ALTITUDE = -1.0
    private const val MOON_FAINT_FRACTION = 0.12
    private const val DEGREES_PER_HOUR = 15.0
    private const val SECONDS_PER_HOUR = 3600.0
    private const val MAX_OFFSET_SECONDS = 18 * 3600

    fun compute(date: LocalDate, location: GeoPoint, settings: PrayerSettings, ramadan: Boolean = false): DayPrayerTimes {
        val observer = location.toObserver()
        val midnight = solarMidnight(date, location)
        val noon = searchHourAngle(Body.Sun, observer, 0.0, midnight).time
        val fajr = searchAltitude(Body.Sun, observer, Direction.Rise, midnight, 1.0, -settings.twilight.fajrAngle)
        val sunrise = searchRiseSet(Body.Sun, observer, Direction.Rise, midnight, 1.0)
        val declination = equator(Body.Sun, noon, observer, EquatorEpoch.OfDate, Aberration.Corrected).dec
        val asrAltitude = asrAltitude(location.latitude, declination, settings.asr)
        val asr = searchAltitude(Body.Sun, observer, Direction.Set, noon, 1.0, asrAltitude)
        val maghrib = searchRiseSet(Body.Sun, observer, Direction.Set, noon, 1.0)
        val isha = settings.twilight.ishaMinutes?.let { minutes ->
            maghrib?.addDays((minutes + if (ramadan) RAMADAN_EXTRA_MINUTES else 0) * 60_000.0 / DAY_MILLIS)
        } ?: settings.twilight.ishaAngle?.let { angle -> searchAltitude(Body.Sun, observer, Direction.Set, noon, 1.0, -angle) }
        return DayPrayerTimes(
            date = date,
            times = listOf(
                PrayerTime(Prayer.Fajr, fajr?.toMillisecondsSince1970()),
                PrayerTime(Prayer.Sunrise, sunrise?.toMillisecondsSince1970()),
                PrayerTime(Prayer.Dhuhr, noon.toMillisecondsSince1970()),
                PrayerTime(Prayer.Asr, asr?.toMillisecondsSince1970()),
                PrayerTime(Prayer.Maghrib, maghrib?.toMillisecondsSince1970()),
                PrayerTime(Prayer.Isha, isha?.toMillisecondsSince1970()),
            ),
        )
    }

    fun sunAltitudeTime(date: LocalDate, location: GeoPoint, altitude: Double, rising: Boolean): Long? {
        val observer = location.toObserver()
        val midnight = solarMidnight(date, location)
        val start = if (rising) midnight else searchHourAngle(Body.Sun, observer, 0.0, midnight).time
        return searchAltitude(Body.Sun, observer, if (rising) Direction.Rise else Direction.Set, start, 1.0, altitude)?.toMillisecondsSince1970()
    }

    fun darkFalseDawn(date: LocalDate, location: GeoPoint): FalseDawn? {
        var first: FalseDawn? = null
        for (offset in 0 until DARK_SEARCH_DAYS) {
            val day = date.plusDays(offset.toLong())
            val millis = sunAltitudeTime(day, location, FALSE_DAWN_ALTITUDE, rising = true) ?: continue
            if (moonDark(millis, location)) return FalseDawn(millis, day, moonFree = true)
            if (first == null) first = FalseDawn(millis, day, moonFree = false)
        }
        return first
    }

    fun moonDark(millis: Long, location: GeoPoint): Boolean {
        val time = Time.fromMillisecondsSince1970(millis)
        val observer = location.toObserver()
        val position = equator(Body.Moon, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
        val altitude = horizon(time, observer, position.ra, position.dec, Refraction.None).altitude
        return altitude < MOON_SET_ALTITUDE || illumination(Body.Moon, time).phaseFraction < MOON_FAINT_FRACTION
    }

    private fun solarMidnight(date: LocalDate, location: GeoPoint): Time {
        val offsetSeconds = (location.longitude / DEGREES_PER_HOUR * SECONDS_PER_HOUR).roundToInt().coerceIn(-MAX_OFFSET_SECONDS, MAX_OFFSET_SECONDS)
        return Time.fromMillisecondsSince1970(date.atStartOfDay(ZoneOffset.ofTotalSeconds(offsetSeconds)).toInstant().toEpochMilli())
    }

    fun asrAltitude(latitude: Double, declination: Double, method: AsrMethod): Double =
        Math.toDegrees(atan(1.0 / (method.shadowFactor + tan(Math.toRadians(abs(latitude - declination))))))
}
