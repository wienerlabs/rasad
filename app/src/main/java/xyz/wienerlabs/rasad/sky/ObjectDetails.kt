package xyz.wienerlabs.rasad.sky

import io.github.cosinekitty.astronomy.Time
import xyz.wienerlabs.rasad.astro.BodyState
import xyz.wienerlabs.rasad.astro.Ephemeris
import xyz.wienerlabs.rasad.astro.Formats
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.Hilal
import xyz.wienerlabs.rasad.astro.Qibla
import xyz.wienerlabs.rasad.astro.RiseSet
import xyz.wienerlabs.rasad.astro.RiseTransitSet
import xyz.wienerlabs.rasad.astro.SkyBody
import xyz.wienerlabs.rasad.astro.SkySnapshot
import xyz.wienerlabs.rasad.astro.transform
import xyz.wienerlabs.rasad.astro.TurkishLocale
import xyz.wienerlabs.rasad.astro.Vec3
import xyz.wienerlabs.rasad.islam.QiblaSun
import java.time.format.TextStyle
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import kotlin.math.asin
import kotlin.math.atan2

data class Fact(val label: String, val value: String)

data class RelatedObject(val ref: SkyObjectRef, val label: String)

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
    val related: List<RelatedObject> = emptyList(),
    val relatedLabel: String? = null,
    val bestView: Long? = null,
    val suggestJump: Boolean = false,
    val jumpLabel: String? = null,
    val texts: List<String> = emptyList(),
    val textsLabel: String? = null,
    val starNote: Boolean = false,
) {
    enum class Script { Arabic, Latin }
}

object ObjectDescriber {
    private const val KM_PER_AU = 149_597_870.7
    private const val YOUNG_CRESCENT_DAYS = 3.5
    private val STAR_TEXTS = mapOf(
        "Sirius" to ("Kur'an'da Şi'râ" to listOf("53:49")),
        "Sadalsuud" to ("Yağmur yıldızdan değildir" to listOf("bukhari:846")),
    )

    fun describe(ref: SkyObjectRef, catalog: SkyCatalog, snapshot: SkySnapshot, location: GeoPoint): ObjectDetails = when (ref) {
        is SkyObjectRef.Star -> star(ref.index, catalog, snapshot, location)
        is SkyObjectRef.Body -> body(snapshot.bodies.first { it.body == ref.body }, snapshot, location)
        is SkyObjectRef.Constellation -> constellation(catalog, ref.index, snapshot, location)
        SkyObjectRef.Qibla -> qibla(location, snapshot.millis)
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

    private fun viewingFact(plan: ViewingPlan, nowMillis: Long): Fact = when {
        plan.neverRises -> Fact("Görünürlük", "Bu konumdan hiç doğmaz")
        plan.bestTime == null -> Fact("Görünürlük", if (plan.alwaysUp) "Hiç batmaz" else "Bugün uygun değil")
        else -> Fact(
            if (plan.bestInDarkness) "Karanlıkta en iyi" else "En yüksek",
            "${whenLabel(plan.bestTime, nowMillis)} · ${Formats.degrees(plan.bestAltitude, 0)}",
        )
    }

    private fun whenLabel(millis: Long, nowMillis: Long): String = Formats.relativeTime(millis, nowMillis)

    private fun bestSeason(equatorial: Vec3, location: GeoPoint, nowMillis: Long): String? {
        val zone = ZoneId.systemDefault()
        val observer = location.toObserver()
        val rotation = DoubleArray(9)
        val year = Instant.ofEpochMilli(nowMillis).atZone(zone).year
        var bestMonth = -1
        var bestAltitude = 0.0
        for (month in 1..12) {
            val evening = LocalDate.of(year, month, 15).atTime(21, 0).atZone(zone).toInstant().toEpochMilli()
            Ephemeris.eqjToEnu(Time.fromMillisecondsSince1970(evening), observer, rotation)
            val altitude = rotation.transform(equatorial).altitudeDegrees
            if (altitude > bestAltitude) {
                bestAltitude = altitude
                bestMonth = month
            }
        }
        if (bestMonth < 0) return null
        return "${Month.of(bestMonth).getDisplayName(TextStyle.FULL_STANDALONE, TurkishLocale)} akşamları"
    }

    private fun star(index: Int, catalog: SkyCatalog, snapshot: SkySnapshot, location: GeoPoint): ObjectDetails {
        val meta = catalog.stars.meta[index]
        val magnitude = catalog.stars.magnitudes[index].toDouble()
        val direction = snapshot.eqjToEnu.transform(catalog.stars.direction(index))
        val lore = StarLoreBook.forStar(meta.properName)
        val constellation = Constellations.turkishName(meta.constellation)
        val events = RiseSet.forStar(catalog.stars.raHours(index), catalog.stars.decDegrees(index), meta.distanceParsecs, location, snapshot.millis)
        val plan = VisibilityPlanner.plan(catalog.stars.direction(index), location, snapshot.millis)
        val facts = buildList {
            add(Fact("Parlaklık", signed(magnitude)))
            if (meta.distanceParsecs > 0) add(Fact("Uzaklık", Formats.lightYears(meta.distanceParsecs)))
            if (meta.spectralType.isNotBlank()) add(Fact("Tayf", meta.spectralType))
            addAll(positionFacts(direction.azimuthDegrees, direction.altitudeDegrees))
            addAll(timeFacts(events, direction.altitudeDegrees))
            add(viewingFact(plan, snapshot.millis))
        }
        val constellationIndex = catalog.constellations.indexOfFirst { it.code == meta.constellation }
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
            related = if (constellationIndex >= 0) listOf(RelatedObject(SkyObjectRef.Constellation(constellationIndex), constellation)) else emptyList(),
            relatedLabel = "Takımyıldızı",
            bestView = plan.bestTime,
            suggestJump = direction.altitudeDegrees < 10.0 && plan.bestTime != null,
            texts = STAR_TEXTS[meta.properName]?.second.orEmpty(),
            textsLabel = STAR_TEXTS[meta.properName]?.first,
            starNote = true,
        )
    }

    private fun body(state: BodyState, snapshot: SkySnapshot, location: GeoPoint): ObjectDetails {
        val events = RiseSet.forBody(state.body, location, snapshot.millis)
        val constellation = Constellations.turkishName(state.constellationCode)
        val lunation = if (state.body == SkyBody.Moon) Hilal.lunation(snapshot.millis) else null
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
                    lunation?.let {
                        add(Fact("Evre", it.phaseName))
                        add(Fact("Aydınlık", Formats.percent(state.phaseFraction)))
                        add(Fact("Yaş", "${Formats.decimal(it.ageDays, 1)} gün"))
                    }
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
            SkyBody.Moon -> "Kamer; hicrî takvimin her ayı yeni hilalin görülmesiyle başlar. Takvim'in Hilal sekmesinde bir sonraki hilalin görünürlüğünü bulabilirsin."
            SkyBody.Venus -> "Halk arasında Çoban Yıldızı; sabah ya da akşam ufkunda Ay'dan sonra gökteki en parlak cisimdir."
            SkyBody.Jupiter -> "Osmanlıca Müşterî. Küçük bir dürbünle dört büyük uydusu yan yana dizilmiş görülür."
            SkyBody.Saturn -> "Osmanlıca Zühal. Halkaları küçük bir teleskopla seçilir."
            SkyBody.Mars -> "Osmanlıca Merih. Kızıl rengi yüzeyindeki demir oksitten gelir."
            SkyBody.Mercury -> "Osmanlıca Utârid. Güneş'e çok yakın olduğu için yalnızca alacakaranlıkta kısa süre görülür."
            SkyBody.Uranus -> "Çıplak gözle görülebilecek sınırdadır; 1781'de teleskopla keşfedildi."
            SkyBody.Neptune -> "Gözle görülmez. Konumu önce hesapla bulundu, sonra 1846'da gözlendi."
        }
        val plan = if (state.body == SkyBody.Sun || state.body == SkyBody.Moon) {
            null
        } else {
            VisibilityPlanner.plan(snapshot.equatorialDirection(state.direction), location, snapshot.millis)
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
            facts = if (plan == null) facts else facts + viewingFact(plan, snapshot.millis),
            bestView = plan?.bestTime,
            suggestJump = plan?.bestTime != null && state.altitude < 10.0,
            texts = if (lunation != null && lunation.ageDays < YOUNG_CRESCENT_DAYS) listOf("tirmidhi:3451") else emptyList(),
            textsLabel = "Hilali görünce",
        )
    }

    private fun constellation(catalog: SkyCatalog, index: Int, snapshot: SkySnapshot, location: GeoPoint): ObjectDetails {
        val figure = catalog.constellations[index]
        val shape = catalog.shapes[index]
        val code = figure.code
        val classical = Constellations.classicalName(code)
        val members = catalog.namedStarsByConstellation[code].orEmpty()
        val direction = snapshot.eqjToEnu.transform(shape.centroid)
        val centroid = shape.centroid
        val raHours = ((Math.toDegrees(atan2(centroid.y, centroid.x)) + 360.0) % 360.0) / 15.0
        val declination = Math.toDegrees(asin(centroid.z.coerceIn(-1.0, 1.0)))
        val plan = VisibilityPlanner.plan(centroid, location, snapshot.millis)
        val facts = buildList {
            addAll(positionFacts(direction.azimuthDegrees, direction.altitudeDegrees))
            if (!plan.neverRises) addAll(timeFacts(RiseSet.forStar(raHours, declination, 0.0, location, snapshot.millis), direction.altitudeDegrees))
            add(viewingFact(plan, snapshot.millis))
            if (!plan.neverRises) bestSeason(centroid, location, snapshot.millis)?.let { add(Fact("En güzel dönem", it)) }
            add(Fact("Genişlik", "yaklaşık ${Formats.degrees(shape.radiusDegrees * 2.0)}"))
            members.firstOrNull()?.let { brightest ->
                add(Fact("En parlak yıldızı", "${catalog.stars.meta[brightest].properName} · ${signed(catalog.stars.magnitudes[brightest].toDouble())}"))
            }
        }
        val folk = Constellations.folkName(code)
        val story = listOfNotNull(
            folk?.let { "Halk arasında $it diye de bilinir." },
            if (plan.neverRises) "Bulunduğun enlemden hiç doğmaz; görmek için daha güneye gitmek gerekir." else null,
        ).joinToString(" ").ifEmpty { null }
        return ObjectDetails(
            title = Constellations.turkishName(code),
            kind = "Takımyıldız · ${Constellations.latinName(code)}",
            original = classical?.arabic,
            transliteration = classical?.transliteration,
            meaning = classical?.meaning,
            story = story,
            facts = facts,
            footnote = classical?.let { "Adın kökeni: Arapça" },
            related = members.take(8).mapNotNull { star -> catalog.stars.meta[star].properName?.let { RelatedObject(SkyObjectRef.Star(star), it) } },
            relatedLabel = "Yıldızları",
            bestView = plan.bestTime,
            suggestJump = direction.altitudeDegrees < 10.0 && plan.bestTime != null,
        )
    }

    private fun qibla(location: GeoPoint, millis: Long): ObjectDetails {
        val bearing = Qibla.bearingDegrees(location)
        val distance = Qibla.distanceKm(location)
        val qiblaSun = runCatching { QiblaSun.next(millis) }.getOrNull()
        val sunUsable = qiblaSun != null && QiblaSun.sunAt(qiblaSun.millis, location).altitude > 5.0
        return ObjectDetails(
            title = "Kıble",
            kind = "Kâbe yönü",
            original = "القبلة",
            transliteration = "el-Kıble",
            meaning = "yönelinen taraf",
            story = "Ufuktaki işaret, Kâbe'ye giden en kısa yolun (büyük daire) başladığı yönü gösterir. Telefonu bu işarete çevirdiğinde hafif bir titreşim duyarsın.",
            facts = listOfNotNull(
                Fact("Açı", "${Formats.degrees(bearing, 1)} ${Formats.compassPoint(bearing)}"),
                Fact("Kâbe'ye uzaklık", "${Formats.grouped(distance)} km"),
                Fact("Konum", location.formatted()),
                qiblaSun?.let { Fact("Kıble güneşi", "${Formats.dayMonth(it.millis)} ${Formats.clock(it.millis)}") },
            ),
            footnote = when {
                qiblaSun == null -> null
                sunUsable -> "Kıble güneşi anında Güneş Kâbe'nin tam üstündedir; o an Güneş'e dönen kıbleye dönmüş olur."
                else -> "Kıble güneşi anında Güneş senin ufkunun altında olacak; bu yöntem bulunduğun yerde kullanılamaz."
            },
            bestView = qiblaSun?.millis?.takeIf { sunUsable },
            suggestJump = sunUsable,
            jumpLabel = "Kıble güneşine git",
            texts = listOf("2:144"),
            textsLabel = "Kur'an'da kıble",
        )
    }
}
