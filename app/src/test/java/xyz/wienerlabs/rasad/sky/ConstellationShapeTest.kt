package xyz.wienerlabs.rasad.sky

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.wienerlabs.rasad.astro.Vec3
import java.io.File

class ConstellationShapeTest {
    private val figures by lazy { SkyCatalog.readConstellations(File("src/main/assets/constellations.bin").readBytes()) }

    private fun shape(code: String) = ConstellationShape.of(figures.first { it.code == code })

    @Test
    fun orionCentroidSitsBetweenBeltAndShoulders() {
        val orion = shape("Ori")
        assertTrue(orion.centroid.angleTo(Vec3.fromEquatorial(5.43, 7.0)) < 1.0)
        assertEquals(17.5, orion.radiusDegrees, 0.5)
        assertEquals(23, orion.vertexCount)
    }

    @Test
    fun sharedEndpointsAreCountedOnce() {
        val crux = figures.first { it.code == "Cru" }
        assertEquals(2, crux.segmentCount)
        assertEquals(4, ConstellationShape.of(crux).vertexCount)
    }

    @Test
    fun littleBearIsGuidedToItsStarsNotToItsLabel() {
        val littleBear = figures.first { it.code == "UMi" }
        val centroid = ConstellationShape.of(littleBear).centroid
        assertTrue(centroid.angleTo(Vec3.fromEquatorial(15.76, 80.0)) < 1.0)
        assertTrue(centroid.angleTo(littleBear.label) > 10.0)
    }

    @Test
    fun everyShapeEnclosesItsStarsAndFitsTheFramingView() {
        figures.forEach { figure ->
            val shape = ConstellationShape.of(figure)
            assertTrue(figure.code, shape.radiusDegrees in 1.5..70.0)
            for (i in 0 until shape.vertexCount) {
                assertTrue(figure.code, shape.centroid.angleTo(shape.vertex(i)) <= shape.radiusDegrees + 1e-6)
            }
            val fov = shape.framingFov()
            assertTrue(figure.code, fov >= 2.0 * shape.radiusDegrees || fov == 118.0)
            assertTrue(figure.code, shape.foundThresholdDegrees() >= 4.0)
        }
    }

    @Test
    fun everyConstellationHasALatinNameAndClassicalNamesAreRealCodes() {
        assertEquals(Constellations.allCodes, Constellations.latinCodes)
        assertTrue(Constellations.allCodes.containsAll(Constellations.classicalCodes))
        assertEquals("Ursa Major", Constellations.latinName("UMa"))
        assertEquals("ed-Dübbü'l-Ekber", Constellations.classicalName("UMa")?.transliteration)
        assertTrue(Constellations.searchText("Tau").contains("Ülker"))
    }
}
