package xyz.wienerlabs.rasad.sky

import xyz.wienerlabs.rasad.astro.SkyBody
import xyz.wienerlabs.rasad.astro.SkySnapshot
import xyz.wienerlabs.rasad.astro.Vec3
import xyz.wienerlabs.rasad.astro.transform

sealed interface SkyObjectRef {
    data class Star(val index: Int) : SkyObjectRef
    data class Body(val body: SkyBody) : SkyObjectRef
    data class Constellation(val index: Int) : SkyObjectRef
    data object Qibla : SkyObjectRef
}

data class SkyLayers(
    val constellationLines: Boolean = true,
    val constellationNames: Boolean = true,
    val starNames: Boolean = true,
    val milkyWay: Boolean = true,
    val grid: Boolean = false,
    val qibla: Boolean = true,
    val ground: Boolean = true,
)

enum class ViewMode { Sensor, Manual }

class PickBuffer(private val capacity: Int = 2048) {
    private val xs = FloatArray(capacity)
    private val ys = FloatArray(capacity)
    private val weights = FloatArray(capacity)
    private val kinds = IntArray(capacity)
    private val ids = IntArray(capacity)
    var count = 0
        private set

    fun clear() {
        count = 0
    }

    fun add(kind: Int, id: Int, x: Float, y: Float, weight: Float) {
        if (count >= capacity) return
        xs[count] = x
        ys[count] = y
        weights[count] = weight
        kinds[count] = kind
        ids[count] = id
        count++
    }

    fun nearest(x: Float, y: Float, radius: Float, weightScale: Float): SkyObjectRef? {
        var best = -1
        var bestScore = Float.MAX_VALUE
        for (i in 0 until count) {
            val dx = xs[i] - x
            val dy = ys[i] - y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)
            if (distance > radius) continue
            val score = distance - weights[i] * weightScale
            if (score < bestScore) {
                bestScore = score
                best = i
            }
        }
        if (best < 0) return null
        return when (kinds[best]) {
            KIND_STAR -> SkyObjectRef.Star(ids[best])
            KIND_BODY -> SkyObjectRef.Body(SkyBody.entries[ids[best]])
            KIND_CONSTELLATION -> SkyObjectRef.Constellation(ids[best])
            else -> SkyObjectRef.Qibla
        }
    }

    companion object {
        const val KIND_STAR = 0
        const val KIND_BODY = 1
        const val KIND_CONSTELLATION = 2
        const val KIND_QIBLA = 3
    }
}

fun SkyObjectRef.direction(catalog: SkyCatalog, snapshot: SkySnapshot, qiblaAzimuth: Double): Vec3 = when (this) {
    is SkyObjectRef.Star -> snapshot.eqjToEnu.transform(catalog.stars.direction(index))
    is SkyObjectRef.Body -> snapshot.bodies.first { it.body == body }.direction
    is SkyObjectRef.Constellation -> snapshot.eqjToEnu.transform(catalog.constellations[index].label)
    SkyObjectRef.Qibla -> Vec3.fromAzimuthAltitude(qiblaAzimuth, 0.0)
}

fun SkyObjectRef.displayName(catalog: SkyCatalog): String = when (this) {
    is SkyObjectRef.Star -> catalog.stars.meta[index].let { it.properName ?: it.designation ?: "Yıldız" }
    is SkyObjectRef.Body -> body.displayName
    is SkyObjectRef.Constellation -> Constellations.turkishName(catalog.constellations[index].code)
    SkyObjectRef.Qibla -> "Kıble"
}
