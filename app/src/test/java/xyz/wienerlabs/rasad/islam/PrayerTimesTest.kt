package xyz.wienerlabs.rasad.islam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.wienerlabs.rasad.astro.GeoPoint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PrayerTimesTest {
    private val istanbul = ZoneId.of("Europe/Istanbul")
    private val date = LocalDate.of(2026, 9, 25)

    private fun minutesOfDay(millis: Long?): Double {
        val time = Instant.ofEpochMilli(millis!!).atZone(istanbul)
        return time.hour * 60.0 + time.minute + time.second / 60.0
    }

    @Test
    fun istanbulTimesMatchAnIndependentSolarModel() {
        val day = PrayerCalculator.compute(date, GeoPoint.Istanbul, istanbul, PrayerSettings())
        val expected = mapOf(
            Prayer.Fajr to 5 * 60 + 22.3,
            Prayer.Sunrise to 6 * 60 + 54.4,
            Prayer.Dhuhr to 12 * 60 + 55.8,
            Prayer.Asr to 16 * 60 + 18.9,
            Prayer.Maghrib to 18 * 60 + 56.5,
            Prayer.Isha to 20 * 60 + 22.9,
        )
        expected.forEach { (prayer, minutes) ->
            assertEquals(prayer.title, minutes, minutesOfDay(day[prayer]), 2.0)
        }
        assertEquals(Prayer.Asr, day.currentAt(day[Prayer.Asr]!! + 60_000L))
        assertNull(day.currentAt(day[Prayer.Sunrise]!! + 60_000L))
        assertEquals(Prayer.Maghrib, day.nextAfter(day[Prayer.Asr]!! + 60_000L)?.prayer)
        val asrMinute = day[Prayer.Asr]!! / 60_000L
        assertEquals(Prayer.Asr, day.currentAtMinute(asrMinute))
        assertEquals(Prayer.Dhuhr, day.currentAtMinute(asrMinute - 1))
    }

    @Test
    fun hanafiAsrComesLaterAndUmmAlQuraIshaFollowsMaghrib() {
        val majority = PrayerCalculator.compute(date, GeoPoint.Istanbul, istanbul, PrayerSettings())
        val hanafi = PrayerCalculator.compute(date, GeoPoint.Istanbul, istanbul, PrayerSettings(asr = AsrMethod.Hanafi))
        val gap = (hanafi[Prayer.Asr]!! - majority[Prayer.Asr]!!) / 60_000.0
        assertTrue("hanafi gap $gap", gap in 35.0..75.0)
        val ummAlQura = PrayerCalculator.compute(date, GeoPoint.Istanbul, istanbul, PrayerSettings(twilight = TwilightMethod.UmmAlQura))
        assertEquals(90.0, (ummAlQura[Prayer.Isha]!! - ummAlQura[Prayer.Maghrib]!!) / 60_000.0, 0.01)
        val ramadan = PrayerCalculator.compute(date, GeoPoint.Istanbul, istanbul, PrayerSettings(twilight = TwilightMethod.UmmAlQura), ramadan = true)
        assertEquals(120.0, (ramadan[Prayer.Isha]!! - ramadan[Prayer.Maghrib]!!) / 60_000.0, 0.01)
    }

    @Test
    fun deepTwilightThatNeverHappensIsReportedAsMissing() {
        val north = GeoPoint(60.0, 10.75)
        val day = PrayerCalculator.compute(LocalDate.of(2026, 6, 21), north, ZoneId.of("Europe/Oslo"), PrayerSettings())
        assertNull(day[Prayer.Fajr])
        assertNull(day[Prayer.Isha])
        assertTrue(day[Prayer.Maghrib] != null)
    }

    @Test
    fun falseDawnSkipsMoonlitMorningsToTheNearestDarkOne() {
        val fullMoonMorning = LocalDate.of(2026, 9, 26)
        val dark = PrayerCalculator.darkFalseDawn(fullMoonMorning, GeoPoint.Istanbul, istanbul)!!
        assertTrue(dark.moonFree)
        assertTrue(dark.date.toString(), dark.date.isAfter(LocalDate.of(2026, 10, 3)) && dark.date.isBefore(LocalDate.of(2026, 10, 13)))
        assertTrue(PrayerCalculator.moonDark(dark.millis, GeoPoint.Istanbul))
        val sameDay = PrayerCalculator.darkFalseDawn(dark.date, GeoPoint.Istanbul, istanbul)!!
        assertEquals(dark.date, sameDay.date)
        val moonlit = PrayerCalculator.sunAltitudeTime(fullMoonMorning, GeoPoint.Istanbul, istanbul, PrayerCalculator.FALSE_DAWN_ALTITUDE, rising = true)!!
        assertTrue(!PrayerCalculator.moonDark(moonlit, GeoPoint.Istanbul))
    }
}
