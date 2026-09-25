package xyz.wienerlabs.rasad.sky

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.Vec3
import java.time.Instant

class VisibilityPlannerTest {
    private fun utc(text: String) = Instant.parse(text).toEpochMilli()

    private val evening = utc("2026-09-25T15:00:00Z")

    @Test
    fun southernCrossNeverRisesOverIstanbul() {
        val plan = VisibilityPlanner.plan(Vec3.fromEquatorial(12.50, -59.7), GeoPoint.Istanbul, evening)
        assertTrue(plan.neverRises)
        assertNull(plan.bestTime)
        assertNull(plan.nextRise)
    }

    @Test
    fun littleBearNeverSetsOverIstanbul() {
        val plan = VisibilityPlanner.plan(Vec3.fromEquatorial(15.76, 80.0), GeoPoint.Istanbul, evening)
        assertTrue(plan.alwaysUp)
        assertFalse(plan.neverRises)
        assertNull(plan.nextRise)
        assertNotNull(plan.bestTime)
    }

    @Test
    fun orionRisesNearMidnightAndPeaksBeforeDawnInLateSeptember() {
        val plan = VisibilityPlanner.plan(Vec3.fromEquatorial(5.43, 7.0), GeoPoint.Istanbul, evening)
        val rise = plan.nextRise!!
        assertTrue("rise ${Instant.ofEpochMilli(rise)}", rise in utc("2026-09-25T20:00:00Z")..utc("2026-09-25T22:30:00Z"))
        val best = plan.bestTime!!
        assertTrue("best ${Instant.ofEpochMilli(best)}", best in utc("2026-09-26T02:00:00Z")..utc("2026-09-26T03:30:00Z"))
        assertTrue(plan.bestInDarkness)
        assertTrue(plan.bestAltitude > 45.0)
        assertEquals(0L, best % (5 * 60_000L))
    }
}
