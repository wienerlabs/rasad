package xyz.wienerlabs.rasad.astro

import android.icu.util.IslamicCalendar
import android.icu.util.TimeZone
import android.icu.util.ULocale

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

    fun at(millis: Long): HijriDate {
        val calendar = IslamicCalendar(TimeZone.getDefault(), ULocale.forLanguageTag("tr-TR"))
        calendar.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        calendar.timeInMillis = millis
        return HijriDate(
            day = calendar.get(IslamicCalendar.DAY_OF_MONTH),
            monthIndex = calendar.get(IslamicCalendar.MONTH),
            year = calendar.get(IslamicCalendar.YEAR),
        )
    }

    fun monthStartingAfter(conjunctionMillis: Long): HijriDate = at(conjunctionMillis + 2L * 86_400_000L)
}
