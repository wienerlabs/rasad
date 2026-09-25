package xyz.wienerlabs.rasad.astro

import io.github.cosinekitty.astronomy.Time
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AstronomyTest {
    private val istanbul = GeoPoint(41.0082, 28.9784)

    private fun utc(text: String) = Instant.parse(text).toEpochMilli()

    @Test
    fun qiblaFromIstanbulPointsSouthEast() {
        val bearing = Qibla.bearingDegrees(istanbul)
        val distance = Qibla.distanceKm(istanbul)
        assertEquals(151.6, bearing, 1.5)
        assertEquals(2400.0, distance, 60.0)
    }

    @Test
    fun qiblaFromJakartaPointsWestNorthWest() {
        val bearing = Qibla.bearingDegrees(GeoPoint(-6.2088, 106.8456))
        assertEquals(295.0, bearing, 1.5)
    }

    @Test
    fun polarisSitsAtObserverLatitudeDueNorth() {
        val polaris = Vec3.fromEquatorial(2.530301, 89.264109)
        listOf("2026-09-25T20:00:00Z", "2026-01-01T03:30:00Z", "2030-06-15T12:00:00Z").forEach { moment ->
            val time = Time.fromMillisecondsSince1970(utc(moment))
            val frame = Ephemeris.eqjToEnu(time, istanbul.toObserver())
            val direction = frame.transform(polaris)
            assertEquals(istanbul.latitude, direction.altitudeDegrees, 1.0)
            val azimuthOffset = ((direction.azimuthDegrees + 180.0) % 360.0) - 180.0
            assertTrue("azimuth $azimuthOffset", kotlin.math.abs(azimuthOffset) < 1.5)
        }
    }

    @Test
    fun solsticeSunPeaksNearSeventyTwoDegreesOverIstanbul() {
        var best = -90.0
        var millis = utc("2026-06-21T09:30:00Z")
        val end = utc("2026-06-21T10:40:00Z")
        while (millis <= end) {
            best = maxOf(best, Ephemeris.snapshot(millis, istanbul).sun.altitude)
            millis += 60_000L
        }
        assertEquals(72.45, best, 0.35)
    }

    @Test
    fun ephemerisReportsSensibleBodyProperties() {
        val snapshot = Ephemeris.snapshot(utc("2026-09-25T20:00:00Z"), istanbul)
        val moon = snapshot.moon
        assertTrue(moon.distanceKm in 356_000.0..407_000.0)
        assertTrue(moon.phaseFraction > 0.9)
        val jupiter = snapshot.bodies.first { it.body == SkyBody.Jupiter }
        assertTrue(jupiter.magnitude in -3.0..-1.5)
        assertEquals(0.2666, snapshot.sun.angularRadiusDegrees, 0.01)
    }

    @Test
    fun nextConjunctionAfterLateSeptember2026FallsOnTenthOfOctober() {
        val conjunction = Hilal.nextConjunction(utc("2026-09-25T00:00:00Z"))
        val date = Instant.ofEpochMilli(conjunction).atOffset(ZoneOffset.UTC).toLocalDate()
        assertEquals(LocalDate.of(2026, 10, 10), date)
    }

    @Test
    fun previousConjunctionPrecedesQueryTime() {
        val now = utc("2026-09-25T12:00:00Z")
        val previous = Hilal.previousConjunction(now)
        val date = Instant.ofEpochMilli(previous).atOffset(ZoneOffset.UTC).toLocalDate()
        assertEquals(LocalDate.of(2026, 9, 11), date)
        assertTrue(previous < now)
    }

    @Test
    fun yallopFormulaMatchesHandComputation() {
        val q = Yallop.q(arcv = 10.0, widthArcMinutes = 0.5)
        assertEquals(0.115395, q, 1e-6)
        assertEquals(YallopCategory.B, YallopCategory.fromQ(q))
        assertEquals(YallopCategory.A, YallopCategory.fromQ(0.3))
        assertEquals(YallopCategory.F, YallopCategory.fromQ(-0.5))
    }

    @Test
    fun octoberCrescentOverIstanbulFollowsTheShallowAutumnEcliptic() {
        val conjunction = Hilal.nextConjunction(utc("2026-09-25T00:00:00Z"))
        val evenings = Hilal.eveningsAfter(conjunction, istanbul, 3)
        assertEquals(CrescentStatus.MoonSetsFirst, evenings[0].status)
        assertEquals(YallopCategory.F, evenings[0].category)
        assertEquals(CrescentStatus.Computed, evenings[1].status)
        assertTrue(evenings[1].lagMinutes!! < 10.0)
        assertEquals(YallopCategory.F, evenings[1].category)
        assertTrue(evenings[2].category.isNakedEye)
        assertTrue(evenings[2].q!! > evenings[1].q!!)
        assertEquals(48.0, evenings[2].moonAgeHours!!, 1.0)
    }

    @Test
    fun americasSeeTheOctoberCrescentBeforeTurkeyDoes() {
        val conjunction = Hilal.nextConjunction(utc("2026-09-25T00:00:00Z"))
        val date = LocalDate.of(2026, 10, 11)
        val istanbulEvening = Hilal.assessEvening(date, istanbul, conjunction)
        val limaEvening = Hilal.assessEvening(date, GeoPoint(-12.0464, -77.0428), conjunction)
        assertTrue(limaEvening.q!! > istanbulEvening.q!!)
    }

    @Test
    fun fastVisibilityGridAgreesWithDetailedEveningModel() {
        val conjunction = Hilal.nextConjunction(utc("2026-09-25T00:00:00Z"))
        val date = LocalDate.of(2026, 10, 11)
        listOf(GeoPoint(41.0, 29.0), GeoPoint(-6.0, 107.0), GeoPoint(-23.0, -43.0), GeoPoint(21.0, 40.0)).forEach { place ->
            val detailed = Hilal.assessEvening(date, place, conjunction)
            val (category, q) = HilalMap.evaluateAt(date, conjunction, place)
            if (detailed.q != null && q != null) {
                assertEquals("q at $place", detailed.q!!, q, 0.03)
            }
            assertEquals("category at $place", detailed.category, category)
        }
    }

    @Test
    fun visibilityGridCoversRequestedArea() {
        val conjunction = Hilal.nextConjunction(utc("2026-09-25T00:00:00Z"))
        val grid = HilalMap.compute(LocalDate.of(2026, 10, 11), conjunction, step = 4.0)
        assertEquals(91, grid.columns)
        assertEquals(31, grid.rows)
        val visibleSomewhere = (0 until grid.rows).any { row -> (0 until grid.columns).any { grid.categoryAt(it, row) == YallopCategory.A } }
        assertTrue(visibleSomewhere)
    }
}
