package xyz.wienerlabs.rasad.sky

import xyz.wienerlabs.rasad.astro.SkyBody
import xyz.wienerlabs.rasad.astro.SkySnapshot
import xyz.wienerlabs.rasad.astro.Vec3
import kotlin.math.abs
import kotlin.math.roundToInt

data class GuidanceReadout(
    val ref: SkyObjectRef,
    val name: String,
    val azimuth: Int,
    val altitude: Int,
    val deltaAzimuth: Int,
    val deltaAltitude: Int,
    val separation: Int,
    val screenAngleDegrees: Int,
    val onScreen: Boolean,
    val centered: Boolean,
    val belowHorizon: Boolean,
)

object Guidance {
    private const val STEER_THRESHOLD = 6
    private const val OVERHEAD_ALTITUDE = 72
    private const val BEHIND_AZIMUTH = 150

    fun wrapDegrees(delta: Double): Double {
        val wrapped = ((delta + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        return if (wrapped == -180.0) 180.0 else wrapped
    }

    fun instruction(readout: GuidanceReadout): String = when {
        readout.belowHorizon && (readout.centered || readout.onScreen) -> "Şu an ufkun altında"
        readout.centered -> "Tam karşında"
        readout.onScreen -> "Görüş alanında, ortaya al"
        else -> steer(readout.deltaAzimuth, readout.deltaAltitude, readout.altitude)
    }

    fun steer(deltaAzimuth: Int, deltaAltitude: Int, targetAltitude: Int): String {
        val parts = ArrayList<String>(2)
        when {
            targetAltitude > OVERHEAD_ALTITUDE -> parts += "Başının üstünde"
            abs(deltaAzimuth) >= BEHIND_AZIMUTH -> parts += if (deltaAzimuth > 0) "Arkanda, sağından dön" else "Arkanda, solundan dön"
            abs(deltaAzimuth) >= STEER_THRESHOLD -> parts += "${if (deltaAzimuth > 0) "Sağa" else "Sola"} ${abs(deltaAzimuth)}°"
        }
        if (abs(deltaAltitude) >= STEER_THRESHOLD) {
            parts += "${abs(deltaAltitude)}° ${if (deltaAltitude > 0) "yukarı" else "aşağı"}"
        }
        return if (parts.isEmpty()) "Çok yakın, biraz daha" else parts.joinToString(" · ")
    }

    fun readout(
        ref: SkyObjectRef,
        name: String,
        direction: Vec3,
        centerAzimuth: Double,
        centerAltitude: Double,
        screenAngleDegrees: Double,
        onScreen: Boolean,
        foundDegrees: Double,
        separationDegrees: Double,
    ): GuidanceReadout {
        val altitude = direction.altitudeDegrees
        return GuidanceReadout(
            ref = ref,
            name = name,
            azimuth = direction.azimuthDegrees.roundToInt() % 360,
            altitude = altitude.roundToInt(),
            deltaAzimuth = wrapDegrees(direction.azimuthDegrees - centerAzimuth).roundToInt(),
            deltaAltitude = (altitude - centerAltitude).roundToInt(),
            separation = separationDegrees.roundToInt(),
            screenAngleDegrees = (screenAngleDegrees / 3.0).roundToInt() * 3,
            onScreen = onScreen,
            centered = separationDegrees < foundDegrees,
            belowHorizon = altitude < -0.5,
        )
    }
}

fun SkyObjectRef.foundThresholdDegrees(catalog: SkyCatalog): Double = when (this) {
    is SkyObjectRef.Constellation -> catalog.shapes[index].foundThresholdDegrees()
    else -> 2.5
}

fun SkyObjectRef.equatorialDirection(catalog: SkyCatalog, snapshot: SkySnapshot): Vec3? = when (this) {
    is SkyObjectRef.Star -> catalog.stars.direction(index)
    is SkyObjectRef.Constellation -> catalog.shapes[index].centroid
    is SkyObjectRef.Body -> if (body == SkyBody.Sun || body == SkyBody.Moon) {
        null
    } else {
        snapshot.equatorialDirection(snapshot.bodies.first { it.body == body }.direction)
    }
    SkyObjectRef.Qibla -> null
}
