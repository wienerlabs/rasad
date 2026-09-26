package xyz.wienerlabs.rasad.islam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.Qibla
import java.time.Instant

class QiblaSunTest {
    private fun utc(text: String) = Instant.parse(text).toEpochMilli()

    @Test
    fun sunPassesOverTheKaabaTwiceAYear() {
        val (may, july) = QiblaSun.eventsIn(2027)
        assertTrue("may ${Instant.ofEpochMilli(may.millis)}", may.millis in utc("2027-05-27T09:05:00Z")..utc("2027-05-28T09:35:00Z"))
        assertTrue("july ${Instant.ofEpochMilli(july.millis)}", july.millis in utc("2027-07-15T09:10:00Z")..utc("2027-07-16T09:45:00Z"))
        assertTrue(may.zenithDistanceDegrees < 0.3)
        assertTrue(july.zenithDistanceDegrees < 0.3)
    }

    @Test
    fun atThatMomentTheSunStandsInTheQiblaDirection() {
        val event = QiblaSun.eventsIn(2027).first()
        val sun = QiblaSun.sunAt(event.millis, GeoPoint.Istanbul)
        assertEquals(Qibla.bearingDegrees(GeoPoint.Istanbul), sun.azimuth, 0.6)
        assertTrue(sun.altitude > 40.0)
    }

    @Test
    fun nextEventIsAlwaysInTheFuture() {
        val now = utc("2026-09-26T00:00:00Z")
        val next = QiblaSun.next(now)
        assertTrue(next.millis > now)
        assertTrue(next.millis < utc("2027-06-01T00:00:00Z"))
    }
}
