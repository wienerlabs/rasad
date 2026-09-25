package xyz.wienerlabs.rasad.sky

import xyz.wienerlabs.rasad.astro.BodyState
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.Hilal
import xyz.wienerlabs.rasad.astro.Qibla
import xyz.wienerlabs.rasad.astro.RiseSet
import xyz.wienerlabs.rasad.astro.RiseTransitSet
import xyz.wienerlabs.rasad.astro.SkyBody
import xyz.wienerlabs.rasad.astro.SkySnapshot
import xyz.wienerlabs.rasad.astro.transform

data class Fact(val label: String, val value: String)

data class ObjectDetails(
    val title: String,
    val kind: String,
    val original: String? = null,
    val originalScript: Script = Script.Arabic,
    val transliteration: String? = null,
    val meaning: String? = null,
    val story: String? = null,
    val facts: List<Fact> = emptyList(),
    val footnote: String? = null,
) {
    enum class Script { Arabic, Latin }
}

object ObjectDescriber {
    private const val KM_PER_AU = 149_597_870.7

    fun describe(ref: SkyObjectRef, catalog: SkyCatalog, snapshot: SkySnapshot, location: GeoPoint): ObjectDetails = when (ref) {
        is SkyObjectRef.Star -> star(ref.index, catalog, snapshot, location)
        is SkyObjectRef.Body -> body(snapshot.bodies.first { it.body == ref.body }, snapshot, location)
        is SkyObjectRef.Constellation -> constellation(catalog, ref.index)
        SkyObjectRef.Qibla -> qibla(location)
    }

    private fun signed(value: Double, digits: Int = 2): String = Formats.decimal(value, digits).replace('-', '−')

    private fun positionFacts(azimuth: Double, altitude: Double): List<Fact> = listOf(
        Fact("Yükseklik", Formats.degrees(altitude, 1).replace('-', '−')),
        Fact("Yön", "${Formats.degrees(azimuth, 0)} ${Formats.compassPoint(azimuth)}"),
    )

    private fun timeFacts(events: RiseTransitSet, altitudeNow: Double): List<Fact> {
        if (events.rise == null && events.set == null) {
            val text = if (altitudeNow > 0) "Bugün hiç batmıyor" else "Bugün ufkun üstüne çıkmıyor"
            return listOf(Fact("Doğuş ve batış", text))
        }
        return listOfNotNull(
            Fact("Doğuş", events.rise?.let { Formats.clock(it) } ?: "Bugün yok"),
            events.transit?.let { Fact("En yüksek", "${Formats.clock(it)} · ${Formats.degrees(events.transitAltitude ?: 0.0, 0)}") },
            Fact("Batış", events.set?.let { Formats.clock(it) } ?: "Bugün yok"),
        )
    }

    private fun star(index: Int, catalog: SkyCatalog, snapshot: SkySnapshot, location: GeoPoint): ObjectDetails {
        val meta = catalog.stars.meta[index]
        val magnitude = catalog.stars.magnitudes[index].toDouble()
        val direction = snapshot.eqjToEnu.transform(catalog.stars.direction(index))
        val lore = StarLoreBook.forStar(meta.properName)
        val constellation = Constellations.turkishName(meta.constellation)
        val events = RiseSet.forStar(catalog.stars.raHours(index), catalog.stars.decDegrees(index), meta.distanceParsecs, location, snapshot.millis)
        val facts = buildList {
            add(Fact("Parlaklık", signed(magnitude)))
            if (meta.distanceParsecs > 0) add(Fact("Uzaklık", Formats.lightYears(meta.distanceParsecs)))
            if (meta.spectralType.isNotBlank()) add(Fact("Tayf", meta.spectralType))
            addAll(positionFacts(direction.azimuthDegrees, direction.altitudeDegrees))
            addAll(timeFacts(events, direction.altitudeDegrees))
        }
        val title = meta.properName ?: meta.designation ?: "İsimsiz yıldız"
        val kind = listOfNotNull(meta.designation?.takeIf { meta.properName != null }, "$constellation takımyıldızı").joinToString(" · ")
        return ObjectDetails(
            title = title,
            kind = kind,
            original = lore?.original,
            originalScript = if (lore?.language == "Latince") ObjectDetails.Script.Latin else ObjectDetails.Script.Arabic,
            transliteration = lore?.transliteration,
            meaning = lore?.meaning,
            story = lore?.story,
            facts = facts,
            footnote = lore?.let { "Adın kökeni: ${it.language}" },
        )
    }

    private fun body(state: BodyState, snapshot: SkySnapshot, location: GeoPoint): ObjectDetails {
        val events = RiseSet.forBody(state.body, location, snapshot.millis)
        val constellation = Constellations.turkishName(state.constellationCode)
        val facts = buildList {
            when (state.body) {
                SkyBody.Sun -> {
                    add(Fact("Uzaklık", "${Formats.decimal(state.distanceAu, 4)} AB"))
                    addAll(positionFacts(state.azimuth, state.altitude))
                    addAll(timeFacts(events, state.altitude))
                    if (events.rise != null && events.set != null && events.set > events.rise) {
                        add(Fact("Gün uzunluğu", Formats.duration(events.set - events.rise)))
                    }
                }
                SkyBody.Moon -> {
                    val lunation = Hilal.lunation(snapshot.millis)
                    add(Fact("Evre", lunation.phaseName))
                    add(Fact("Aydınlık", Formats.percent(state.phaseFraction)))
                    add(Fact("Yaş", "${Formats.decimal(lunation.ageDays, 1)} gün"))
                    add(Fact("Uzaklık", "${Formats.grouped(state.distanceKm)} km"))
                    addAll(positionFacts(state.azimuth, state.altitude))
                    addAll(timeFacts(events, state.altitude))
                }
                else -> {
                    add(Fact("Parlaklık", signed(state.magnitude)))
                    add(Fact("Uzaklık", "${Formats.decimal(state.distanceAu, 2)} AB · ${Formats.grouped(state.distanceAu * KM_PER_AU / 1_000_000)} milyon km"))
                    add(Fact("Aydınlık", Formats.percent(state.phaseFraction)))
                    add(Fact("Takımyıldız", constellation))
                    addAll(positionFacts(state.azimuth, state.altitude))
                    addAll(timeFacts(events, state.altitude))
                }
            }
        }
        val story = when (state.body) {
            SkyBody.Sun -> "Osmanlıca metinlerde Şems diye anılır. Namaz vakitleri doğrudan onun ufka göre yüksekliğinden hesaplanır."
            SkyBody.Moon -> "Kamer; hicrî takvimin her ayı yeni hilalin görülmesiyle başlar. Hilal sekmesinde bir sonraki ayın görünürlüğünü bulabilirsin."
            SkyBody.Venus -> "Halk arasında Çoban Yıldızı; sabah ya da akşam ufkunda Ay'dan sonra gökteki en parlak cisimdir."
            SkyBody.Jupiter -> "Osmanlıca Müşterî. Küçük bir dürbünle dört büyük uydusu yan yana dizilmiş görülür."
            SkyBody.Saturn -> "Osmanlıca Zühal. Halkaları küçük bir teleskopla seçilir."
            SkyBody.Mars -> "Osmanlıca Merih. Kızıl rengi yüzeyindeki demir oksitten gelir."
            SkyBody.Mercury -> "Osmanlıca Utârid. Güneş'e çok yakın olduğu için yalnızca alacakaranlıkta kısa süre görülür."
            SkyBody.Uranus -> "Çıplak gözle görülebilecek sınırdadır; 1781'de teleskopla keşfedildi."
            SkyBody.Neptune -> "Gözle görülmez. Konumu önce hesapla bulundu, sonra 1846'da gözlendi."
        }
        return ObjectDetails(
            title = state.body.displayName,
            kind = when (state.body) {
                SkyBody.Sun -> "Yıldızımız"
                SkyBody.Moon -> "Doğal uydu"
                else -> "Gezegen · $constellation"
            },
            original = state.body.classicalArabic,
            transliteration = state.body.classicalName,
            meaning = state.body.classicalName?.let { "klasik adı" },
            story = story,
            facts = facts,
        )
    }

    private fun constellation(catalog: SkyCatalog, index: Int): ObjectDetails {
        val figure = catalog.constellations[index]
        val brightest = catalog.stars.meta
            .filter { it.constellation == figure.code && it.properName != null }
            .sortedBy { catalog.stars.magnitudes[it.index] }
            .take(5)
            .mapNotNull { it.properName }
        return ObjectDetails(
            title = Constellations.turkishName(figure.code),
            kind = "Takımyıldız · ${figure.code}",
            story = if (brightest.isEmpty()) null else "Adı olan en parlak yıldızları: ${brightest.joinToString(", ")}.",
            facts = listOf(Fact("Çizgi sayısı", "${figure.segmentCount}")),
        )
    }

    private fun qibla(location: GeoPoint): ObjectDetails {
        val bearing = Qibla.bearingDegrees(location)
        val distance = Qibla.distanceKm(location)
        return ObjectDetails(
            title = "Kıble",
            kind = "Kâbe yönü",
            original = "القبلة",
            transliteration = "el-Kıble",
            meaning = "yönelinen taraf",
            story = "Ufuktaki işaret, Kâbe'ye giden en kısa yolun (büyük daire) başladığı yönü gösterir. Telefonu bu işarete çevirdiğinde hafif bir titreşim duyarsın.",
            facts = listOf(
                Fact("Açı", "${Formats.degrees(bearing, 1)} ${Formats.compassPoint(bearing)}"),
                Fact("Kâbe'ye uzaklık", "${Formats.grouped(distance)} km"),
                Fact("Konum", location.formatted()),
            ),
        )
    }
}
