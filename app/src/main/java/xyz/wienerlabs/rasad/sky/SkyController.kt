package xyz.wienerlabs.rasad.sky

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import xyz.wienerlabs.rasad.astro.Ephemeris
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.Qibla
import xyz.wienerlabs.rasad.astro.SkySnapshot
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

@Stable
class SkyClock {
    var offsetMillis by mutableLongStateOf(0L)
    var playing by mutableStateOf(false)
    var speed by mutableFloatStateOf(DEFAULT_SPEED)

    val isLive: Boolean get() = abs(offsetMillis) < 60_000L && !playing

    fun now(): Long = System.currentTimeMillis() + offsetMillis

    fun advance(realDeltaMillis: Double) {
        if (playing) shift((realDeltaMillis * speed).toLong())
    }

    fun shift(millis: Long) {
        offsetMillis = (offsetMillis + millis).coerceIn(-MAX_OFFSET_MILLIS, MAX_OFFSET_MILLIS)
    }

    fun setAbsolute(millis: Long) {
        offsetMillis = (millis - System.currentTimeMillis()).coerceIn(-MAX_OFFSET_MILLIS, MAX_OFFSET_MILLIS)
    }

    fun reset() {
        offsetMillis = 0L
        playing = false
    }

    companion object {
        const val DEFAULT_SPEED = 900f
        private const val MAX_OFFSET_MILLIS = 200L * 365L * 86_400_000L
    }
}

data class SkyPresentation(
    val millis: Long,
    val target: SkyObjectRef? = null,
    val towardSunAltitude: Double? = null,
    val fov: Double? = null,
    val dawnGuide: Boolean = false,
)

@Stable
class SkyController(
    val catalog: SkyCatalog,
    val clock: SkyClock,
    initialLocation: GeoPoint,
) {
    val camera = SkyCamera()
    val renderState = SkyRenderState()
    val picks = PickBuffer()

    var location by mutableStateOf(initialLocation)
    var mode by mutableStateOf(ViewMode.Sensor)
    var layers by mutableStateOf(SkyLayers())
    var selected by mutableStateOf<SkyObjectRef?>(null)
    var target by mutableStateOf<SkyObjectRef?>(null)
    var frame by mutableLongStateOf(0L)
        private set
    var centerAzimuth by mutableFloatStateOf(0f)
        private set
    var centerAltitude by mutableFloatStateOf(0f)
        private set
    var qiblaAligned by mutableStateOf(false)
        private set
    var displayedMinute by mutableLongStateOf(0L)
        private set
    var published by mutableStateOf(Ephemeris.snapshot(clock.now(), initialLocation))
        private set
    var sensorActive by mutableStateOf(false)
        private set
    var guidance by mutableStateOf<GuidanceReadout?>(null)
        private set
    var dawnGuide by mutableStateOf(false)
        private set

    var current: SkySnapshot = published
        private set

    var onQiblaAligned: (() -> Unit)? = null
    var onTargetCentered: (() -> Unit)? = null
    var onTargetInView: (() -> Unit)? = null

    private var manualAzimuth = 180.0
    private var manualAltitude = 30.0
    private var velocityAzimuth = 0.0
    private var velocityAltitude = 0.0
    private var flight: Flight? = null
    private val projected = FloatArray(2)
    private val sensorForward = DoubleArray(3)
    private val sensorUp = DoubleArray(3)
    private val smoothForward = DoubleArray(3)
    private val smoothUp = DoubleArray(3)
    private var smoothPrimed = false
    private var lastFrameNanos = 0L
    private var lastSnapshotMillis = Long.MIN_VALUE
    private var snapshotLocation = initialLocation
    private var lastPublishNanos = 0L
    private var readoutFrames = 0
    private var targetWasCentered = false
    private var targetWasInView = false
    private var trackedTarget: SkyObjectRef? = null
    private var guidanceFrames = 0
    private var pendingFlight = false
    private var pendingPresentation: SkyPresentation? = null
    private var dawnGuideMillis = 0L
    private var seconds = 0.0

    val qiblaAzimuth: Double get() = Qibla.bearingDegrees(location)

    init {
        camera.lookAt(manualAzimuth, manualAltitude)
    }

    fun onFrame(frameNanos: Long, tracker: OrientationTracker?) {
        val dt = if (lastFrameNanos == 0L) 1.0 / 60.0 else ((frameNanos - lastFrameNanos) / 1e9).coerceIn(0.0, 0.1)
        lastFrameNanos = frameNanos
        seconds += dt
        clock.advance(dt * 1000.0)
        val now = clock.now()
        if (abs(now - lastSnapshotMillis) > SNAPSHOT_INTERVAL_MILLIS || snapshotLocation != location) {
            current = Ephemeris.snapshot(now, location)
            lastSnapshotMillis = now
            snapshotLocation = location
        }
        if (frameNanos - lastPublishNanos > 250_000_000L) {
            published = current
            lastPublishNanos = frameNanos
        }
        val minute = now / 60_000L
        if (minute != displayedMinute) displayedMinute = minute
        if (pendingFlight) {
            pendingFlight = false
            targetWasInView = false
            targetWasCentered = false
            renderState.targetRevealStart = -1f
            target?.let { flyTo(it) }
        }
        pendingPresentation?.let { presentation ->
            pendingPresentation = null
            selected = null
            val requested = presentation.target
            if (requested != null) {
                switchToManual()
                flyTo(requested, presentation.fov)
            } else {
                target = null
                val altitude = presentation.towardSunAltitude ?: camera.centerAltitude
                val azimuth = if (presentation.towardSunAltitude != null) current.sun.azimuth else camera.centerAzimuth
                flyToDirection(azimuth, altitude, presentation.fov)
            }
        }
        if (dawnGuide && abs(now - dawnGuideMillis) > DAWN_GUIDE_SPAN_MILLIS) dawnGuide = false

        updateCamera(frameNanos, dt, tracker)
        updateReadouts()

        renderState.layers = layers
        renderState.qiblaAzimuth = qiblaAzimuth
        renderState.qiblaAligned = qiblaAligned
        renderState.selected = selected
        renderState.target = target
        renderState.showReticle = mode == ViewMode.Sensor && sensorActive
        renderState.sensorView = mode == ViewMode.Sensor && sensorActive
        renderState.dawnGuide = dawnGuide
        renderState.seconds = seconds.toFloat()
        frame = frameNanos
    }

    private fun updateCamera(frameNanos: Long, dt: Double, tracker: OrientationTracker?) {
        val reading = mode == ViewMode.Sensor && tracker != null && tracker.readBasis(sensorForward, sensorUp)
        sensorActive = reading
        if (reading) {
            if (!smoothPrimed) {
                camera.forward.copyInto(smoothForward)
                camera.up.copyInto(smoothUp)
                smoothPrimed = true
            }
            val alpha = 1.0 - exp(-dt / SENSOR_SMOOTHING_SECONDS)
            blend(smoothForward, sensorForward, alpha)
            blend(smoothUp, sensorUp, alpha)
            camera.setBasis(smoothForward, smoothUp)
            return
        }
        smoothPrimed = false
        val activeFlight = flight
        if (activeFlight != null) {
            val t = ((frameNanos - activeFlight.startNanos) / 1e9 / activeFlight.durationSeconds).coerceIn(0.0, 1.0)
            val eased = if (t < 0.5) 4 * t * t * t else 1 - Math.pow(-2 * t + 2, 3.0) / 2
            manualAzimuth = activeFlight.startAzimuth + activeFlight.deltaAzimuth * eased
            manualAltitude = activeFlight.startAltitude + (activeFlight.endAltitude - activeFlight.startAltitude) * eased
            camera.setFov(activeFlight.startFov + (activeFlight.endFov - activeFlight.startFov) * eased)
            if (t >= 1.0) flight = null
        } else if (abs(velocityAzimuth) > 0.01 || abs(velocityAltitude) > 0.01) {
            manualAzimuth += velocityAzimuth * dt
            manualAltitude += velocityAltitude * dt
            val decay = exp(-dt * 3.2)
            velocityAzimuth *= decay
            velocityAltitude *= decay
        }
        manualAzimuth = ((manualAzimuth % 360.0) + 360.0) % 360.0
        manualAltitude = manualAltitude.coerceIn(-85.0, 89.5)
        camera.lookAt(manualAzimuth, manualAltitude)
    }

    private fun blend(current: DoubleArray, target: DoubleArray, alpha: Double) {
        for (i in 0..2) current[i] += (target[i] - current[i]) * alpha
        val length = sqrt(current[0] * current[0] + current[1] * current[1] + current[2] * current[2]).takeIf { it > 1e-9 } ?: return
        for (i in 0..2) current[i] /= length
    }

    private fun updateReadouts() {
        readoutFrames++
        if (readoutFrames % 6 == 0) {
            centerAzimuth = camera.centerAzimuth.toFloat()
            centerAltitude = camera.centerAltitude.toFloat()
        }
        val aligned = layers.qibla && angularDifference(camera.centerAzimuth, qiblaAzimuth) < QIBLA_TOLERANCE_DEGREES &&
            abs(camera.centerAltitude) < 45.0
        if (aligned && !qiblaAligned) onQiblaAligned?.invoke()
        if (aligned != qiblaAligned) qiblaAligned = aligned

        val currentTarget = target
        if (currentTarget != trackedTarget) {
            trackedTarget = currentTarget
            targetWasCentered = false
            targetWasInView = false
            renderState.targetRevealStart = -1f
            guidanceFrames = 0
        }
        if (currentTarget == null) {
            if (guidance != null) guidance = null
            return
        }
        val direction = currentTarget.direction(catalog, current, qiblaAzimuth)
        val depth = camera.depth(direction.x, direction.y, direction.z)
        val separation = Math.toDegrees(acos(depth.coerceIn(-1.0, 1.0)))
        val found = currentTarget.foundThresholdDegrees(catalog)
        val inView = depth > 0.2 && camera.project(direction, projected) && camera.inSafeArea(projected[0], projected[1])
        if (inView && !targetWasInView) {
            renderState.targetRevealStart = seconds.toFloat()
            onTargetInView?.invoke()
        }
        targetWasInView = targetWasInView || inView
        val centered = separation < found
        if (centered && !targetWasCentered) onTargetCentered?.invoke()
        targetWasCentered = centered
        if (guidanceFrames++ % 6 == 0) {
            val (screenX, screenY) = camera.screenDirection(direction)
            guidance = Guidance.readout(
                ref = currentTarget,
                name = currentTarget.displayName(catalog),
                direction = direction,
                centerAzimuth = camera.centerAzimuth,
                centerAltitude = camera.centerAltitude,
                screenAngleDegrees = Math.toDegrees(atan2(screenY, screenX)),
                onScreen = inView,
                foundDegrees = found,
                separationDegrees = separation,
            )
        }
    }

    fun present(presentation: SkyPresentation) {
        clock.playing = false
        clock.setAbsolute(presentation.millis)
        dawnGuide = presentation.dawnGuide
        dawnGuideMillis = presentation.millis
        refreshNow()
        pendingPresentation = presentation
    }

    fun dismissDawnGuide() {
        dawnGuide = false
    }

    fun flyToDirection(azimuth: Double, altitude: Double, fov: Double? = null) {
        switchToManual()
        var delta = azimuth - manualAzimuth
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        flight = Flight(
            startNanos = if (lastFrameNanos == 0L) System.nanoTime() else lastFrameNanos,
            durationSeconds = 1.1,
            startAzimuth = manualAzimuth,
            deltaAzimuth = delta,
            startAltitude = manualAltitude,
            endAltitude = altitude.coerceIn(-85.0, 89.5),
            startFov = camera.fovDegrees,
            endFov = fov ?: camera.fovDegrees,
        )
    }

    fun jumpTo(millis: Long) {
        clock.playing = false
        clock.setAbsolute(millis)
        refreshNow()
        if (target != null && mode == ViewMode.Manual) pendingFlight = true
    }

    fun switchToManual() {
        if (mode == ViewMode.Manual) return
        manualAzimuth = camera.centerAzimuth
        manualAltitude = camera.centerAltitude
        velocityAzimuth = 0.0
        velocityAltitude = 0.0
        mode = ViewMode.Manual
    }

    fun switchToSensor() {
        flight = null
        velocityAzimuth = 0.0
        velocityAltitude = 0.0
        mode = ViewMode.Sensor
    }

    fun onPan(dx: Float, dy: Float) {
        switchToManual()
        flight = null
        velocityAzimuth = 0.0
        velocityAltitude = 0.0
        val degreesPerPixel = Math.toDegrees(1.0 / camera.scale)
        val altitudeFactor = max(cos(Math.toRadians(manualAltitude)), 0.2)
        manualAzimuth -= dx * degreesPerPixel / altitudeFactor
        manualAltitude += dy * degreesPerPixel
    }

    fun onZoom(factor: Float) {
        flight = null
        camera.setFov(camera.fovDegrees / factor)
    }

    fun onFling(velocityX: Float, velocityY: Float) {
        if (mode != ViewMode.Manual) return
        val degreesPerPixel = Math.toDegrees(1.0 / camera.scale)
        val altitudeFactor = max(cos(Math.toRadians(manualAltitude)), 0.2)
        velocityAzimuth = -velocityX * degreesPerPixel / altitudeFactor
        velocityAltitude = velocityY * degreesPerPixel
    }

    fun onTap(x: Float, y: Float, radiusPx: Float, weightPx: Float) {
        selected = picks.nearest(x, y, radiusPx, weightPx)
    }

    fun lookAt(azimuth: Double, altitude: Double, fov: Double? = null) {
        switchToManual()
        flight = null
        manualAzimuth = azimuth
        manualAltitude = altitude
        fov?.let { camera.setFov(it) }
        camera.lookAt(manualAzimuth, manualAltitude)
    }

    fun flyTo(ref: SkyObjectRef, fov: Double? = null) {
        target = ref
        if (mode == ViewMode.Sensor && sensorActive) return
        switchToManual()
        val direction = ref.direction(catalog, current, qiblaAzimuth)
        val endAzimuth = direction.azimuthDegrees
        val endAltitude = direction.altitudeDegrees.coerceIn(-85.0, 89.5)
        var delta = endAzimuth - manualAzimuth
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        flight = Flight(
            startNanos = if (lastFrameNanos == 0L) System.nanoTime() else lastFrameNanos,
            durationSeconds = 1.1,
            startAzimuth = manualAzimuth,
            deltaAzimuth = delta,
            startAltitude = manualAltitude,
            endAltitude = endAltitude,
            startFov = camera.fovDegrees,
            endFov = fov ?: when (ref) {
                is SkyObjectRef.Constellation -> catalog.shapes[ref.index].framingFov()
                else -> camera.fovDegrees.coerceAtMost(70.0)
            },
        )
    }

    fun refreshNow() {
        lastSnapshotMillis = Long.MIN_VALUE
    }

    private class Flight(
        val startNanos: Long,
        val durationSeconds: Double,
        val startAzimuth: Double,
        val deltaAzimuth: Double,
        val startAltitude: Double,
        val endAltitude: Double,
        val startFov: Double,
        val endFov: Double,
    )

    companion object {
        private const val SNAPSHOT_INTERVAL_MILLIS = 15_000L
        private const val DAWN_GUIDE_SPAN_MILLIS = 3L * 3_600_000L
        private const val SENSOR_SMOOTHING_SECONDS = 0.08
        private const val QIBLA_TOLERANCE_DEGREES = 2.0

        fun angularDifference(a: Double, b: Double): Double {
            val difference = abs(((a - b) % 360.0 + 540.0) % 360.0 - 180.0)
            return difference
        }
    }
}
