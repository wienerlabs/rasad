package xyz.wienerlabs.rasad.astro

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

val TurkishLocale: Locale = Locale.forLanguageTag("tr-TR")

object Formats {
    private val clock = DateTimeFormatter.ofPattern("HH:mm", TurkishLocale)
    private val dayMonth = DateTimeFormatter.ofPattern("d MMMM", TurkishLocale)
    private val dayMonthWeekday = DateTimeFormatter.ofPattern("d MMMM EEEE", TurkishLocale)
    private val dayMonthYear = DateTimeFormatter.ofPattern("d MMMM yyyy", TurkishLocale)
    private val shortDayMonth = DateTimeFormatter.ofPattern("d MMM", TurkishLocale)
    private val weekday = DateTimeFormatter.ofPattern("EEEE", TurkishLocale)

    fun clock(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        clock.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun dayMonth(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dayMonth.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun dayMonthWeekday(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dayMonthWeekday.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun dayMonthYear(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        dayMonthYear.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun shortDayMonth(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        shortDayMonth.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun weekday(millis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        weekday.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun decimal(value: Double, digits: Int = 1): String = "%.${digits}f".format(TurkishLocale, value)

    fun degrees(value: Double, digits: Int = 0): String = "${decimal(value, digits)}°"

    fun percent(fraction: Double): String = "%${decimal(fraction * 100.0, 0)}"

    fun grouped(value: Double): String = "%,d".format(TurkishLocale, value.roundToLong())

    fun duration(millis: Long): String {
        val totalMinutes = abs(millis) / 60_000L
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return when {
            days > 0 -> "$days gün $hours sa"
            hours > 0 -> "$hours sa $minutes dk"
            else -> "$minutes dk"
        }
    }

    fun compassPoint(azimuth: Double): String {
        val points = listOf("K", "KKD", "KD", "DKD", "D", "DGD", "GD", "GGD", "G", "GGB", "GB", "BGB", "B", "BKB", "KB", "KKB")
        val index = (((azimuth % 360.0) + 360.0) % 360.0 / 22.5 + 0.5).toInt() % 16
        return points[index]
    }

    fun lightYears(parsecs: Double): String {
        val lightYears = parsecs * 3.261563777
        return when {
            lightYears < 10 -> "${decimal(lightYears, 2)} ışık yılı"
            lightYears < 100 -> "${decimal(lightYears, 1)} ışık yılı"
            else -> "${grouped(lightYears)} ışık yılı"
        }
    }
}
