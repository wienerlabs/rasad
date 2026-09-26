package xyz.wienerlabs.rasad.astro

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

data class HijriDate(val day: Int, val monthIndex: Int, val year: Int) {
    val monthName: String get() = HijriCalendar.monthNames[monthIndex]

    fun formatted(): String = "$day $monthName $year"
}

object HijriCalendar {
    val monthNames = listOf(
        "Muharrem",
        "Safer",
        "Rebîülevvel",
        "Rebîülâhir",
        "Cemâziyelevvel",
        "Cemâziyelâhir",
        "Recep",
        "Şaban",
        "Ramazan",
        "Şevval",
        "Zilkade",
        "Zilhicce",
    )

    fun at(millis: Long, offsetDays: Int = 0, zone: ZoneId = ZoneId.systemDefault()): HijriDate =
        of(Instant.ofEpochMilli(millis).atZone(zone).toLocalDate(), offsetDays)

    fun of(date: LocalDate, offsetDays: Int = 0): HijriDate {
        val hijrah = HijrahDate.from(date.plusDays(offsetDays.toLong()))
        return HijriDate(
            day = hijrah.get(ChronoField.DAY_OF_MONTH),
            monthIndex = hijrah.get(ChronoField.MONTH_OF_YEAR) - 1,
            year = hijrah.get(ChronoField.YEAR_OF_ERA),
        )
    }

    fun gregorian(year: Int, monthIndex: Int, day: Int, offsetDays: Int = 0): LocalDate {
        val first = HijrahDate.of(year, monthIndex + 1, 1)
        val clamped = day.coerceIn(1, first.lengthOfMonth())
        return LocalDate.from(HijrahDate.of(year, monthIndex + 1, clamped)).minusDays(offsetDays.toLong())
    }

    fun monthLength(year: Int, monthIndex: Int): Int = HijrahDate.of(year, monthIndex + 1, 1).lengthOfMonth()

    fun monthStartingAfter(conjunctionMillis: Long): HijriDate = at(conjunctionMillis + 2L * 86_400_000L)
}
