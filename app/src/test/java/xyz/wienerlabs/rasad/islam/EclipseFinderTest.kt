package xyz.wienerlabs.rasad.islam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.wienerlabs.rasad.astro.GeoPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class EclipseFinderTest {
    private val from = Instant.parse("2026-09-26T00:00:00Z").toEpochMilli()
    private val eclipses by lazy { EclipseFinder.upcoming(GeoPoint.Istanbul, from, 2.5) }

    private fun dateOf(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

    @Test
    fun augustTwentyTwentySevenSolarEclipseIsSeenFromIstanbul() {
        val solar = eclipses.first { it.body == EclipseBody.Sun }
        assertEquals(LocalDate.of(2027, 8, 2), dateOf(solar.peak))
        assertEquals(EclipseType.Partial, solar.type)
        assertTrue(solar.peakAltitude > 20.0)
        assertTrue("obscuration ${solar.obscuration}", solar.obscuration in 0.2..0.95)
        assertTrue(solar.begin < solar.peak && solar.peak < solar.end)
    }

    @Test
    fun penumbralEclipsesAreListedButNotCountedAsNoticeable() {
        val first = eclipses.first()
        assertEquals(EclipseBody.Moon, first.body)
        assertEquals(LocalDate.of(2027, 2, 20), dateOf(first.peak))
        assertEquals(EclipseType.Penumbral, first.type)
        assertFalse(first.noticeable)
        val noticeableLunar = eclipses.first { it.body == EclipseBody.Moon && it.noticeable }
        assertEquals(LocalDate.of(2028, 1, 12), dateOf(noticeableLunar.peak))
        assertEquals(EclipseType.Partial, noticeableLunar.type)
        val total = eclipses.first { it.body == EclipseBody.Moon && it.type == EclipseType.Total }
        assertEquals(LocalDate.of(2028, 12, 31), dateOf(total.peak))
    }

    @Test
    fun everyListedEclipseHappensAboveTheLocalHorizon() {
        assertTrue(eclipses.isNotEmpty())
        eclipses.filter { it.body == EclipseBody.Sun }.forEach { assertTrue(it.peakAltitude > -1.0) }
        assertEquals(eclipses.sortedBy { it.peak }, eclipses)
    }
}
