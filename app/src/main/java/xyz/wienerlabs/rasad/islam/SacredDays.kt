package xyz.wienerlabs.rasad.islam

import xyz.wienerlabs.rasad.astro.HijriCalendar
import java.time.LocalDate

enum class SacredKind(val title: String) {
    Month("Ay"),
    Days("Faziletli günler"),
    Night("Geceler"),
    Fast("Oruç"),
    Eid("Bayram"),
    NoFast("Oruç tutulmaz"),
}

data class SacredDay(
    val key: String,
    val title: String,
    val kind: SacredKind,
    val monthIndexes: List<Int>?,
    val firstDay: Int,
    val lastDay: Int,
    val summary: String,
    val sources: List<String>,
    val monthNotes: Map<Int, String> = emptyMap(),
)

data class SacredOccurrence(
    val day: SacredDay,
    val hijriYear: Int,
    val monthIndex: Int,
    val start: LocalDate,
    val end: LocalDate,
) {
    val note: String? get() = day.monthNotes[monthIndex]
}

object SacredDays {
    private const val MONTH_END = 30
    private const val MUHARRAM = 0
    private const val RAJAB = 6
    private const val RAMADAN = 8
    private const val SHAWWAL = 9
    private const val DHUL_QADAH = 10
    private const val DHUL_HIJJAH = 11
    private val WHITE_DAY_MONTHS = (0..11).filter { it != RAMADAN && it != DHUL_HIJJAH }

    val all = listOf(
        SacredDay(
            "ramadan", "Ramazan", SacredKind.Month, listOf(RAMADAN), 1, MONTH_END,
            "Oruç ayı. Hilal görülünce başlar ve görülünce biter; hilal görülmezse içinde bulunulan ay otuza tamamlanır.",
            listOf("2:185", "bukhari:1909"),
        ),
        SacredDay(
            "lastTen", "Ramazan'ın son on gecesi", SacredKind.Night, listOf(RAMADAN), 21, MONTH_END,
            "Kadir Gecesi bu gecelerin tek olanlarında aranır. Her gece, bir önceki günün akşamında başlar.",
            listOf("bukhari:2017"),
        ),
        SacredDay(
            "fitr", "Ramazan Bayramı", SacredKind.Eid, listOf(SHAWWAL), 1, 1,
            "Müslümanların iki bayramından biri; bu gün oruç tutulmaz.",
            listOf("abudawud:1134", "bukhari:1990"),
        ),
        SacredDay(
            "shawwal", "Şevval'in altı günü", SacredKind.Fast, listOf(SHAWWAL), 2, MONTH_END,
            "Ramazan'dan sonra Şevval içinde tutulan altı gün oruç; hadiste, sürekli oruç tutmuş gibi olmaya vesile olduğu bildirilir.",
            listOf("muslim:1164"),
        ),
        SacredDay(
            "dhulHijjahTen", "Zilhicce'nin ilk on günü", SacredKind.Days, listOf(DHUL_HIJJAH), 1, 10,
            "Salih amellerin Allah katında en sevimli olduğu günler.",
            listOf("tirmidhi:757"),
        ),
        SacredDay(
            "arafah", "Arefe günü", SacredKind.Fast, listOf(DHUL_HIJJAH), 9, 9,
            "Bu günün orucunun, önceki ve sonraki yılın günahlarına kefaret olması umulur.",
            listOf("muslim:1162"),
        ),
        SacredDay(
            "adha", "Kurban Bayramı", SacredKind.Eid, listOf(DHUL_HIJJAH), 10, 10,
            "Müslümanların iki bayramından biri; bu gün oruç tutulmaz.",
            listOf("abudawud:1134", "bukhari:1990"),
        ),
        SacredDay(
            "tashriq", "Teşrik günleri", SacredKind.NoFast, listOf(DHUL_HIJJAH), 11, 13,
            "Yeme ve içme günleri; oruç tutulmaz.",
            listOf("muslim:1141"),
        ),
        SacredDay(
            "ashura", "Tâsûâ ve Âşûrâ", SacredKind.Fast, listOf(MUHARRAM), 9, 10,
            "Âşûrâ orucunun önceki yılın günahlarına kefaret olması umulur; Peygamber (s.a.v.) gelecek yıla ulaşırsa dokuzuncu günü mutlaka tutacağını söylemiştir.",
            listOf("muslim:1162", "muslim:1134"),
        ),
        SacredDay(
            "white", "Eyyâm-ı bîd", SacredKind.Fast, WHITE_DAY_MONTHS, 13, 15,
            "Aydan üç gün oruç tutmak isteyene hadiste gösterilen günler: her hicrî ayın 13, 14 ve 15. günleri.",
            listOf("tirmidhi:761"),
        ),
        SacredDay(
            "sacredMonth", "Haram ay", SacredKind.Month, listOf(MUHARRAM, RAJAB, DHUL_QADAH, DHUL_HIJJAH), 1, MONTH_END,
            "Allah'ın haram kıldığı dört aydan biri; âyette “o aylar içinde kendinize yazık etmeyin” buyrulur.",
            listOf("9:36", "bukhari:3197"),
            monthNotes = mapOf(
                RAJAB to "Recep'in haram aylardan biri oluşu âyet ve sahih hadisle sabittir. İbn Hacer, Tebyînü'l-aceb'de, bunun dışında Recep ayının faziletine, orucuna, ondan belirli bir günün orucuna ya da belirli bir gecesinde namaza dair delil olmaya elverişli sahih bir hadis bulunmadığını söyler.",
            ),
        ),
    )

    fun upcoming(from: LocalDate, offsetDays: Int = 0, months: Int = 13): List<SacredOccurrence> {
        val today = HijriCalendar.of(from, offsetDays)
        val occurrences = ArrayList<SacredOccurrence>()
        var year = today.year
        var monthIndex = today.monthIndex
        repeat(months) {
            for (day in all) {
                val indexes = day.monthIndexes
                if (indexes != null && monthIndex !in indexes) continue
                val length = HijriCalendar.monthLength(year, monthIndex)
                val first = day.firstDay.coerceAtMost(length)
                val last = day.lastDay.coerceAtMost(length)
                val start = HijriCalendar.gregorian(year, monthIndex, first, offsetDays)
                val end = HijriCalendar.gregorian(year, monthIndex, last, offsetDays)
                if (!end.isBefore(from)) occurrences += SacredOccurrence(day, year, monthIndex, start, end)
            }
            monthIndex++
            if (monthIndex == 12) {
                monthIndex = 0
                year++
            }
        }
        return occurrences.sortedWith(compareBy({ it.start }, { it.end }))
    }
}
