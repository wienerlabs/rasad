package xyz.wienerlabs.rasad.astro

import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Direction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.defineStar
import io.github.cosinekitty.astronomy.searchHourAngle
import io.github.cosinekitty.astronomy.searchRiseSet
import java.time.Instant
import java.time.ZoneId

data class RiseTransitSet(
    val rise: Long?,
    val transit: Long?,
    val transitAltitude: Double?,
    val set: Long?,
)

object RiseSet {
    private val starLock = Any()

    fun forBody(body: SkyBody, location: GeoPoint, dayMillis: Long, zone: ZoneId = ZoneId.systemDefault()): RiseTransitSet =
        compute(body.body, location, dayMillis, zone)

    fun forStar(raHours: Double, decDegrees: Double, distanceParsecs: Double, location: GeoPoint, dayMillis: Long, zone: ZoneId = ZoneId.systemDefault()): RiseTransitSet =
        synchronized(starLock) {
            val lightYears = if (distanceParsecs > 0) distanceParsecs * 3.261563777 else 1000.0
            defineStar(Body.Star1, ((raHours % 24.0) + 24.0) % 24.0, decDegrees, lightYears.coerceAtLeast(1.0))
            compute(Body.Star1, location, dayMillis, zone)
        }

    private fun compute(body: Body, location: GeoPoint, dayMillis: Long, zone: ZoneId): RiseTransitSet {
        val observer = location.toObserver()
        val midnight = Instant.ofEpochMilli(dayMillis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val start = Time.fromMillisecondsSince1970(midnight)
        val rise = searchRiseSet(body, observer, Direction.Rise, start, 1.0)
        val set = searchRiseSet(body, observer, Direction.Set, start, 1.0)
        val transit = runCatching { searchHourAngle(body, observer, 0.0, start) }.getOrNull()
        val transitWithinDay = transit?.takeIf { it.time.toMillisecondsSince1970() - midnight < 86_400_000L }
        return RiseTransitSet(
            rise = rise?.toMillisecondsSince1970(),
            transit = transitWithinDay?.time?.toMillisecondsSince1970(),
            transitAltitude = transitWithinDay?.hor?.altitude,
            set = set?.toMillisecondsSince1970(),
        )
    }
}
