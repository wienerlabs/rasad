package xyz.wienerlabs.rasad.islam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.wienerlabs.rasad.astro.HijriCalendar
import java.time.LocalDate

class SacredDaysTest {
    private val today = LocalDate.of(2026, 9, 26)

    @Test
    fun hijriDateFollowsUmmAlQuraAndTheLocalSightingOffset() {
        assertEquals("15 Rebîülâhir 1448", HijriCalendar.of(today).formatted())
        assertEquals("14 Rebîülâhir 1448", HijriCalendar.of(today, -1).formatted())
        assertEquals(LocalDate.of(2027, 2, 8), HijriCalendar.gregorian(1448, 8, 1))
        assertEquals(LocalDate.of(2027, 2, 9), HijriCalendar.gregorian(1448, 8, 1, -1))
    }

    @Test
    fun upcomingDaysLandOnTheirUmmAlQuraDates() {
        val upcoming = SacredDays.upcoming(today)
        fun first(key: String) = upcoming.first { it.day.key == key }
        assertEquals(LocalDate.of(2027, 2, 8), first("ramadan").start)
        assertEquals(LocalDate.of(2027, 3, 9), first("fitr").start)
        assertEquals(LocalDate.of(2027, 5, 15), first("arafah").start)
        assertEquals(LocalDate.of(2027, 5, 16), first("adha").start)
        assertEquals(LocalDate.of(2027, 6, 14), first("ashura").start)
        assertEquals(LocalDate.of(2027, 6, 15), first("ashura").end)
        assertEquals(today, first("white").end)
        assertEquals(upcoming.sortedBy { it.start }, upcoming)
        assertTrue(upcoming.any { it.day.key == "sacredMonth" && it.monthIndex == 6 && it.note != null })
    }

    @Test
    fun localSightingOffsetShiftsEveryDay() {
        val shifted = SacredDays.upcoming(today, offsetDays = -1)
        assertEquals(LocalDate.of(2027, 2, 9), shifted.first { it.day.key == "ramadan" }.start)
    }

    @Test
    fun onlyDaysWithSahihGroundsAreListed() {
        val forbidden = listOf("kandil", "mevlid", "regaib", "berat", "miraç", "mirac", "aşure", "hıdrellez")
        SacredDays.all.forEach { day ->
            val text = "${day.key} ${day.title} ${day.summary}".lowercase()
            forbidden.forEach { word -> assertFalse("${day.title} mentions $word", text.contains(word)) }
            assertTrue(day.title, day.sources.isNotEmpty())
        }
    }

    @Test
    fun noVoluntaryFastFallsOnAForbiddenDayOrInsideRamadan() {
        listOf(-1, 0, 1).forEach { offset ->
            val occurrences = SacredDays.upcoming(today, offset, months = 36)
            val blocked = occurrences.filter { it.day.kind == SacredKind.Eid || it.day.kind == SacredKind.NoFast || it.day.key == "ramadan" }
            occurrences.filter { it.day.kind == SacredKind.Fast }.forEach { fast ->
                blocked.forEach { other ->
                    val apart = fast.end.isBefore(other.start) || fast.start.isAfter(other.end)
                    assertTrue("${fast.day.key} ${fast.start} overlaps ${other.day.key} ${other.start}", apart)
                }
            }
        }
    }
}
