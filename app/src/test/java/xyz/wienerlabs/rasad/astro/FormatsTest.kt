package xyz.wienerlabs.rasad.astro

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class FormatsTest {
    private val istanbul = ZoneId.of("Europe/Istanbul")

    private fun utc(text: String) = Instant.parse(text).toEpochMilli()

    @Test
    fun relativeTimesReadTheWayPeopleTalkAboutTheNight() {
        val evening = utc("2026-09-25T19:30:00Z")
        assertEquals("bu gece 01:40", Formats.relativeTime(utc("2026-09-25T22:40:00Z"), evening, istanbul))
        assertEquals("yarın 20:50", Formats.relativeTime(utc("2026-09-26T17:50:00Z"), evening, istanbul))
        assertEquals("23:45", Formats.relativeTime(utc("2026-09-25T20:45:00Z"), evening, istanbul))
        val noon = utc("2026-09-25T09:00:00Z")
        assertEquals("bu akşam 20:00", Formats.relativeTime(utc("2026-09-25T17:00:00Z"), noon, istanbul))
        assertEquals("15:00", Formats.relativeTime(utc("2026-09-25T12:00:00Z"), noon, istanbul))
        assertEquals("28 Eylül 21:00", Formats.relativeTime(utc("2026-09-28T18:00:00Z"), noon, istanbul))
    }
}
