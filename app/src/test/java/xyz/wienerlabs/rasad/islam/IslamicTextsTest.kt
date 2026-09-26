package xyz.wienerlabs.rasad.islam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IslamicTextsTest {
    private data class Verified(val key: String, val url: String, val reference: String, val arabic: String, val meal: String)

    private val verified: Map<String, Verified> by lazy {
        val stream = requireNotNull(javaClass.getResourceAsStream("/islamic-sources.tsv"))
        stream.bufferedReader(Charsets.UTF_8).readLines().drop(1).filter { it.isNotBlank() }.map { line ->
            val parts = line.split('\t')
            Verified(parts[0], parts[1], parts[2], parts[3], parts.getOrElse(4) { "" })
        }.associateBy { it.key }
    }

    private val interfaceKeys = listOf(
        "4:103", "muslim:1094", "tirmidhi:149", "bukhari:1909", "2:189", "10:5", "tirmidhi:3451", "2:144",
        "bukhari:1044", "6:97", "16:16", "67:5", "qatada", "bukhari:846", "53:49", "tirmidhi:149:fecr",
        "abudawud:3905", "muslim:934", "hattabi", "uthaymin:ikiye", "uthaymin:tesyir",
    )

    private val scholarStatements = setOf("qatada", "hattabi", "uthaymin:ikiye", "uthaymin:tesyir")

    private fun normalize(text: String): String =
        text.replace("\u200f", "").replace("\"", "").replace("“", "").replace("”", "")
            .replace(Regex("\\s+"), " ").replace(" .", ".").replace(" ،", "،").trim()

    @Test
    fun everyTextIsCutFromItsVerifiedSource() {
        assertEquals(verified.keys, IslamicTexts.keys)
        IslamicTexts.keys.forEach { key ->
            val text = IslamicTexts[key]!!
            val source = verified.getValue(key)
            assertTrue(source.url, source.url.startsWith("https://"))
            val arabic = requireNotNull(text.arabic) { key }
            assertTrue(key, normalize(source.arabic).contains(normalize(arabic)))
            if (source.meal.isNotEmpty()) assertTrue(key, source.meal.contains(text.meaning.removePrefix("… ")))
        }
    }

    @Test
    fun quranAyatAreVerbatim() {
        IslamicTexts.keys.filter { it.first().isDigit() }.forEach { key ->
            val text = IslamicTexts[key]!!
            assertEquals(key, verified.getValue(key).arabic, text.arabic)
            assertTrue(key, text.translator!!.startsWith("meal: Diyanet"))
        }
    }

    @Test
    fun everyReferencedKeyHasAText() {
        (SacredDays.all.flatMap { it.sources } + interfaceKeys).forEach { assertNotNull(it, IslamicTexts[it]) }
    }

    @Test
    fun everyHadithCarriesItsGrading() {
        IslamicTexts.keys.filter { !it.first().isDigit() && it !in scholarStatements }.forEach { key ->
            assertNotNull(key, IslamicTexts[key]!!.grade)
        }
    }

    @Test
    fun textsUseNoEmDash() {
        IslamicTexts.keys.forEach { key ->
            val text = IslamicTexts[key]!!
            listOfNotNull(text.meaning, text.citation, text.shortCitation, text.grade, text.translator, text.transliteration).forEach {
                assertFalse(key, it.contains('\u2014'))
            }
        }
    }
}
