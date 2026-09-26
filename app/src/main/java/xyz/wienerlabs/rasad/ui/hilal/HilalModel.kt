package xyz.wienerlabs.rasad.ui.hilal

import xyz.wienerlabs.rasad.astro.CrescentEvening
import xyz.wienerlabs.rasad.astro.Ephemeris
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.HijriCalendar
import xyz.wienerlabs.rasad.astro.HijriDate
import xyz.wienerlabs.rasad.astro.Hilal
import xyz.wienerlabs.rasad.astro.LunationInfo
import xyz.wienerlabs.rasad.astro.YallopCategory

data class HilalOverview(
    val millis: Long,
    val lunation: LunationInfo,
    val heroPose: MoonPose,
    val conjunction: Long,
    val month: HijriDate,
    val evenings: List<CrescentEvening>,
    val eveningPoses: List<MoonPose?>,
) {
    val suggestedEvening: Int
        get() = evenings.indexOfFirst { it.category.isNakedEye }.takeIf { it >= 0 }
            ?: evenings.indexOfFirst { it.category <= YallopCategory.C }.takeIf { it >= 0 }
            ?: 0
}

data class UpcomingMonth(
    val month: HijriDate,
    val conjunction: Long,
    val firstNakedEye: CrescentEvening?,
    val firstOptical: CrescentEvening?,
)

object HilalModel {
    private const val DAY_MILLIS = 86_400_000L

    fun overview(millis: Long, location: GeoPoint): HilalOverview {
        val lunation = Hilal.lunation(millis)
        val conjunction = if (millis - lunation.previousNewMoon < 3 * DAY_MILLIS) lunation.previousNewMoon else lunation.nextNewMoon
        val evenings = Hilal.eveningsAfter(conjunction, location, 3)
        val poses = evenings.map { evening ->
            evening.bestTimeMillis?.let { MoonPoses.fromSnapshot(Ephemeris.snapshot(it, location)) }
        }
        return HilalOverview(
            millis = millis,
            lunation = lunation,
            heroPose = MoonPoses.fromSnapshot(Ephemeris.snapshot(millis, location)),
            conjunction = conjunction,
            month = HijriCalendar.monthStartingAfter(conjunction),
            evenings = evenings,
            eveningPoses = poses,
        )
    }

    fun upcomingMonths(fromConjunction: Long, location: GeoPoint, count: Int): List<UpcomingMonth> {
        val months = ArrayList<UpcomingMonth>(count)
        var conjunction = fromConjunction
        repeat(count) {
            val evenings = Hilal.eveningsAfter(conjunction, location, 3)
            months += UpcomingMonth(
                month = HijriCalendar.monthStartingAfter(conjunction),
                conjunction = conjunction,
                firstNakedEye = evenings.firstOrNull { it.category.isNakedEye },
                firstOptical = evenings.firstOrNull { it.category <= YallopCategory.C },
            )
            conjunction = Hilal.nextConjunction(conjunction + 20 * DAY_MILLIS)
        }
        return months
    }
}
