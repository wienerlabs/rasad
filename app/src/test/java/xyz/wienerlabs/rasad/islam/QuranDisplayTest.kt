package xyz.wienerlabs.rasad.islam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.wienerlabs.rasad.ui.islam.arabicForDisplay

class QuranDisplayTest {
    private val pauseMark = Regex("[\u06D6-\u06DC]")
    private val pauseMarkAfterSpace = Regex(" [\u06D6-\u06DC]")

    @Test
    fun pauseMarksMoveOntoThePrecedingWordAndNothingElseChanges() {
        val ayat = IslamicTexts.keys.filter { it.first().isDigit() }.map { IslamicTexts[it]!!.arabic!! }
        assertTrue(ayat.any { pauseMarkAfterSpace.containsMatchIn(it) })
        ayat.forEach { ayah ->
            val shown = arabicForDisplay(ayah)
            assertEquals(ayah.replace(" ", ""), shown.replace(" ", ""))
            assertFalse(pauseMarkAfterSpace.containsMatchIn(shown))
            assertEquals(pauseMark.findAll(ayah).count(), pauseMark.findAll(shown).count())
        }
    }
}
