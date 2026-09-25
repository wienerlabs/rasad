package xyz.wienerlabs.rasad.sky

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.wienerlabs.rasad.astro.Vec3

class SkyCameraTest {
    private val camera = SkyCamera().apply {
        setViewport(1080f, 2400f)
        setFov(78.0)
    }

    @Test
    fun lookingAtAnObjectPlacesItAtTheCenter() {
        camera.lookAt(243.0, 31.0)
        val out = FloatArray(2)
        assertTrue(camera.project(Vec3.fromAzimuthAltitude(243.0, 31.0), out))
        assertEquals(540f, out[0], 0.01f)
        assertEquals(1200f, out[1], 0.01f)
        assertEquals(243.0, camera.centerAzimuth, 1e-6)
        assertEquals(31.0, camera.centerAltitude, 1e-6)
    }

    @Test
    fun projectionRoundTripsThroughUnproject() {
        camera.lookAt(120.0, 20.0)
        val out = FloatArray(2)
        listOf(Vec3.fromAzimuthAltitude(150.0, 35.0), Vec3.fromAzimuthAltitude(95.0, 5.0), Vec3.fromAzimuthAltitude(120.0, 70.0)).forEach { direction ->
            assertTrue(camera.project(direction, out))
            val back = camera.unproject(out[0], out[1])
            assertTrue(direction.angleTo(back) < 0.01)
        }
    }

    @Test
    fun eastAppearsRightOfNorthAndZenithAbove() {
        camera.lookAt(0.0, 10.0)
        val out = FloatArray(2)
        camera.project(Vec3.fromAzimuthAltitude(20.0, 10.0), out)
        assertTrue(out[0] > 540f)
        camera.project(Vec3.fromAzimuthAltitude(0.0, 40.0), out)
        assertTrue(out[1] < 1200f)
    }

    @Test
    fun horizontalFieldOfViewSpansTheScreenWidth() {
        camera.lookAt(0.0, 0.0)
        val out = FloatArray(2)
        camera.project(Vec3.fromAzimuthAltitude(39.0, 0.0), out)
        assertEquals(1080f, out[0], 2f)
    }

    @Test
    fun explicitBasisIsOrthonormalised() {
        camera.setBasis(doubleArrayOf(0.0, 2.0, 0.0), doubleArrayOf(0.0, 0.3, 1.0))
        val r = camera.right
        val u = camera.up
        val f = camera.forward
        assertEquals(0.0, r[0] * u[0] + r[1] * u[1] + r[2] * u[2], 1e-9)
        assertEquals(0.0, u[0] * f[0] + u[1] * f[1] + u[2] * f[2], 1e-9)
        assertEquals(1.0, r[0], 1e-9)
    }
}
