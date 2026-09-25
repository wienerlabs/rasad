package xyz.wienerlabs.rasad.sky

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.wienerlabs.rasad.astro.Vec3

class GuidanceTest {
    @Test
    fun azimuthDifferencesWrapToTheShortTurn() {
        assertEquals(-10.0, Guidance.wrapDegrees(350.0), 1e-9)
        assertEquals(170.0, Guidance.wrapDegrees(-190.0), 1e-9)
        assertEquals(180.0, Guidance.wrapDegrees(180.0), 1e-9)
        assertEquals(180.0, Guidance.wrapDegrees(-180.0), 1e-9)
    }

    @Test
    fun steeringReadsLikeSpokenDirections() {
        assertEquals("Sağa 42° · 18° yukarı", Guidance.steer(42, 18, 30))
        assertEquals("Sola 42°", Guidance.steer(-42, -3, 10))
        assertEquals("Arkanda, sağından dön", Guidance.steer(170, 0, 20))
        assertEquals("Arkanda, solundan dön · 12° aşağı", Guidance.steer(-160, -12, 5))
        assertEquals("Başının üstünde · 30° yukarı", Guidance.steer(20, 30, 80))
        assertEquals("Çok yakın, biraz daha", Guidance.steer(2, 3, 20))
    }

    @Test
    fun readoutMeasuresTheTurnFromTheViewCenter() {
        val target = Vec3.fromAzimuthAltitude(10.0, 40.0)
        val readout = Guidance.readout(
            ref = SkyObjectRef.Qibla,
            name = "Test",
            direction = target,
            centerAzimuth = 330.0,
            centerAltitude = 20.0,
            screenAngleDegrees = 44.0,
            onScreen = false,
            foundDegrees = 2.5,
            separationDegrees = 36.0,
        )
        assertEquals(40, readout.deltaAzimuth)
        assertEquals(20, readout.deltaAltitude)
        assertEquals(45, readout.screenAngleDegrees)
        assertFalse(readout.centered)
        assertFalse(readout.belowHorizon)
        assertEquals("Sağa 40° · 20° yukarı", Guidance.instruction(readout))
    }

    @Test
    fun readoutFlagsCenteredAndBelowHorizonTargets() {
        val below = Guidance.readout(SkyObjectRef.Qibla, "Test", Vec3.fromAzimuthAltitude(200.0, -12.0), 200.0, -10.0, 0.0, true, 4.0, 2.0)
        assertTrue(below.centered)
        assertTrue(below.belowHorizon)
        assertEquals("Şu an ufkun altında", Guidance.instruction(below))
        val found = below.copy(belowHorizon = false)
        assertEquals("Tam karşında", Guidance.instruction(found))
        assertEquals("Görüş alanında, ortaya al", Guidance.instruction(found.copy(centered = false)))
    }
}
