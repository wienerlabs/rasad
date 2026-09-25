package xyz.wienerlabs.rasad

import android.content.Intent
import xyz.wienerlabs.rasad.astro.SkyBody
import xyz.wienerlabs.rasad.astro.TurkishLocale
import xyz.wienerlabs.rasad.sky.Constellations
import xyz.wienerlabs.rasad.sky.SkyCatalog
import xyz.wienerlabs.rasad.sky.SkyObjectRef
import java.time.Instant

data class LaunchRequest(
    val screen: String? = null,
    val azimuth: Double? = null,
    val altitude: Double? = null,
    val fov: Double? = null,
    val timeMillis: Long? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val place: String? = null,
    val select: String? = null,
    val target: String? = null,
    val night: Boolean = false,
    val grid: Boolean = false,
    val evening: Int? = null,
    val quiet: Boolean = false,
    val play: Boolean = false,
    val id: Long = System.nanoTime(),
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null

    companion object {
        fun from(intent: Intent?): LaunchRequest? {
            if (intent == null) return null
            val uri = intent.data
            fun read(key: String): String? = uri?.getQueryParameter(key) ?: intent.getStringExtra(key)
            val keys = listOf("screen", "az", "alt", "fov", "time", "lat", "lon", "place", "select", "target", "night", "grid", "evening", "quiet", "play")
            if (uri?.host == null && keys.none { intent.hasExtra(it) }) return null
            return LaunchRequest(
                screen = read("screen") ?: uri?.host,
                azimuth = read("az")?.toDoubleOrNull(),
                altitude = read("alt")?.toDoubleOrNull(),
                fov = read("fov")?.toDoubleOrNull(),
                timeMillis = read("time")?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
                latitude = read("lat")?.toDoubleOrNull(),
                longitude = read("lon")?.toDoubleOrNull(),
                place = read("place"),
                select = read("select"),
                target = read("target"),
                night = read("night") == "1",
                grid = read("grid") == "1",
                evening = read("evening")?.toIntOrNull(),
                quiet = read("quiet") == "1",
                play = read("play") == "1",
            )
        }
    }
}

fun SkyCatalog.resolve(name: String): SkyObjectRef? {
    val query = name.trim().lowercase(TurkishLocale)
    if (query in setOf("kible", "kıble", "qibla")) return SkyObjectRef.Qibla
    SkyBody.entries.firstOrNull { it.displayName.lowercase(TurkishLocale) == query || it.name.lowercase() == query }?.let {
        return SkyObjectRef.Body(it)
    }
    stars.indexOfName(name.trim())?.let { return SkyObjectRef.Star(it) }
    constellations.indexOfFirst {
        it.code.lowercase() == query ||
            Constellations.turkishName(it.code).lowercase(TurkishLocale) == query ||
            Constellations.latinName(it.code).lowercase(TurkishLocale) == query
    }.takeIf { it >= 0 }?.let { return SkyObjectRef.Constellation(it) }
    return null
}
