package xyz.wienerlabs.rasad.sky

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogTest {
    private val assets = File("src/main/assets")

    private val stars by lazy {
        SkyCatalog.readStars(File(assets, "stars.bin").readBytes(), File(assets, "stars_meta.tsv").readLines())
    }

    @Test
    fun starsAreSortedByMagnitudeSoTheRendererCanStopEarly() {
        for (i in 1 until stars.count) {
            assertTrue("star $i out of order", stars.magnitudes[i] >= stars.magnitudes[i - 1])
        }
        assertEquals(8920, stars.count)
        assertEquals("Sirius", stars.meta[0].properName)
    }

    @Test
    fun starPositionsAreUnitVectors() {
        for (i in 0 until stars.count) {
            val x = stars.positions[i * 3]
            val y = stars.positions[i * 3 + 1]
            val z = stars.positions[i * 3 + 2]
            assertEquals(1.0, (x * x + y * y + z * z).toDouble(), 1e-4)
        }
    }

    @Test
    fun loreEntriesPointAtRealCatalogStars() {
        val names = stars.meta.mapNotNull { it.properName }.toSet()
        val missing = StarLoreBook.names.filterNot { it in names }
        assertTrue("lore without a catalog star: $missing", missing.size <= 1)
    }

    @Test
    fun everyConstellationHasATurkishNameAndLines() {
        val figures = SkyCatalog.readConstellations(File(assets, "constellations.bin").readBytes())
        assertEquals(89, figures.size)
        figures.forEach { figure ->
            assertTrue(figure.code, figure.code in Constellations.allCodes)
            assertTrue(figure.code, figure.segmentCount > 0)
        }
    }

    @Test
    fun landPolygonsCoverTheGlobe() {
        val rings = SkyCatalog.readLand(File(assets, "land.bin").readBytes())
        assertEquals(127, rings.size)
        assertTrue(rings.all { it.size >= 6 && it.size % 2 == 0 })
    }
}
