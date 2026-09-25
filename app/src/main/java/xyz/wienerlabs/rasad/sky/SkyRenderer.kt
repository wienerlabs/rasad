package xyz.wienerlabs.rasad.sky

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import xyz.wienerlabs.rasad.astro.BodyState
import xyz.wienerlabs.rasad.astro.transform
import xyz.wienerlabs.rasad.astro.SkyBody
import xyz.wienerlabs.rasad.astro.SkySnapshot
import xyz.wienerlabs.rasad.astro.Vec3
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

class SkyTypefaces(val sans: Typeface, val display: Typeface)

class SkyRenderState {
    var layers = SkyLayers()
    var qiblaAzimuth = 0.0
    var qiblaAligned = false
    var selected: SkyObjectRef? = null
    var target: SkyObjectRef? = null
    var showReticle = false
    var sensorView = false
    var targetRevealStart = -1f
    var seconds = 0f
}

class SkyRenderer(
    private val catalog: SkyCatalog,
    private val typefaces: SkyTypefaces,
    private val density: Float,
) {
    private val stars = catalog.stars
    private val skyShader = SkyShader(catalog.milkyWay, catalog.moonTexture)
    private val backgroundPaint = Paint().apply { shader = skyShader.shader }

    private val colorCount = StarField.bucketColors.size
    private val classOf = IntArray(stars.count)
    private val classBuffers: Array<FloatArray>
    private val classCounts: IntArray
    private val glowX = FloatArray(64)
    private val glowY = FloatArray(64)
    private val glowIndex = IntArray(64)
    private var glowCount = 0

    private val namedStarsByBrightness: IntArray = catalog.stars.meta
        .filter { it.properName != null }
        .sortedBy { catalog.stars.magnitudes[it.index] }
        .map { it.index }
        .toIntArray()

    private val totalSegments = catalog.constellations.sumOf { it.segmentCount }
    private val lineBuffer = FloatArray(totalSegments * 4)
    private val dimLineBuffer = FloatArray(totalSegments * 4)

    init {
        val sizes = IntArray(MAGNITUDE_BINS * colorCount)
        for (i in 0 until stars.count) {
            val cls = magnitudeBin(stars.magnitudes[i]) * colorCount + stars.colorBuckets[i]
            classOf[i] = cls
            sizes[cls]++
        }
        classBuffers = Array(sizes.size) { FloatArray(sizes[it] * 2) }
        classCounts = IntArray(sizes.size)
    }

    private fun dp(value: Float) = value * density

    private val white = 0xFFF4F2EC.toInt()

    private val starPaints = Array(colorCount) { bucket ->
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = StarField.bucketColors[bucket]
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }
    }
    private val glowBitmap = createGlow(96)
    private val glowRect = RectF()
    private val glowPaints = Array(colorCount) { bucket ->
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(StarField.bucketColors[bucket], PorterDuff.Mode.SRC_IN)
        }
    }
    private val bodyGlowPaints = SkyBody.entries.map { body ->
        Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = PorterDuffColorFilter(body.color, PorterDuff.Mode.SRC_IN)
        }
    }
    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        strokeWidth = dp(0.9f)
        strokeCap = Paint.Cap.ROUND
    }
    private val horizonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.STROKE
        strokeWidth = dp(1.1f)
    }
    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        strokeWidth = dp(1f)
        strokeCap = Paint.Cap.ROUND
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        alpha = 26
        style = Paint.Style.STROKE
        strokeWidth = dp(0.8f)
    }
    private val cardinalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.display
        fontVariationSettings = "'wght' 500"
        textSize = dp(15f)
        color = white
        textAlign = Paint.Align.CENTER
    }
    private val degreePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.sans
        textSize = dp(9.5f)
        color = white
        alpha = 120
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.04f
    }
    private val starLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.sans
        textSize = dp(11.5f)
        color = white
        letterSpacing = 0.02f
    }
    private val bodyLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.sans
        fontVariationSettings = "'wght' 540"
        textSize = dp(12.5f)
        color = white
        letterSpacing = 0.02f
    }
    private val constellationPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.sans
        textSize = dp(10.5f)
        color = white
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.16f
    }
    private val qiblaStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.STROKE
        strokeWidth = dp(1.3f)
    }
    private val qiblaFill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.FILL
    }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
        strokeCap = Paint.Cap.ROUND
    }
    private val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.STROKE
        strokeWidth = dp(1.6f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val guideLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.sans
        fontVariationSettings = "'wght' 540"
        textSize = dp(11.5f)
        color = white
        textAlign = Paint.Align.CENTER
    }

    private val focusGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        strokeWidth = dp(6f)
        strokeCap = Paint.Cap.ROUND
    }
    private val focusLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        strokeWidth = dp(1.7f)
        strokeCap = Paint.Cap.ROUND
    }
    private val vertexPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.FILL
    }
    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        strokeCap = Paint.Cap.ROUND
    }
    private val focusTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.display
        fontVariationSettings = "'wght' 560"
        textSize = dp(19f)
        color = white
    }
    private val focusSubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.sans
        textSize = dp(11.5f)
        color = white
        letterSpacing = 0.02f
    }
    private val roadPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.STROKE
        strokeWidth = dp(1.2f)
        strokeCap = Paint.Cap.ROUND
    }
    private val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = white
        style = Paint.Style.STROKE
        strokeWidth = dp(1.8f)
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val reticleLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = typefaces.sans
        textSize = dp(11f)
        color = white
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.06f
    }
    private val focusX = FloatArray(MAX_FOCUS_VERTICES)
    private val focusY = FloatArray(MAX_FOCUS_VERTICES)
    private val focusVisible = BooleanArray(MAX_FOCUS_VERTICES)
    private val roadX = FloatArray(ROAD_STEPS + 1)
    private val roadY = FloatArray(ROAD_STEPS + 1)
    private val focusTitles = Array(catalog.constellations.size) { Constellations.turkishName(catalog.constellations[it].code) }
    private val focusSubtitles = Array(catalog.constellations.size) { index ->
        val code = catalog.constellations[index].code
        listOfNotNull(Constellations.latinName(code), Constellations.classicalName(code)?.transliteration).joinToString(" · ")
    }
    private val focusMembers = Array(catalog.constellations.size) { index ->
        catalog.namedStarsByConstellation[catalog.constellations[index].code].orEmpty().take(6).toIntArray()
    }
    private val reticleForward = DoubleArray(3)
    private var reticleMillis = Long.MIN_VALUE
    private var reticleConstellation = -1

    private val path = Path()
    private val point = FloatArray(2)
    private val point2 = FloatArray(2)
    private val labelRects = Array(320) { RectF() }
    private var labelCount = 0

    fun render(canvas: Canvas, camera: SkyCamera, snapshot: SkySnapshot, state: SkyRenderState, picks: PickBuffer) {
        picks.clear()
        labelCount = 0
        camera.setSafeInsets(dp(20f), dp(132f), dp(300f))
        val layers = state.layers
        val limit = limitingMagnitude(snapshot)
        val milkyWayStrength = if (layers.milkyWay) ((limit - 3.2f) / 2.8f).coerceIn(0f, 1f) else 0f
        val pixelAngle = 1.0 / camera.scale
        val moonRadius = max(Math.toRadians(snapshot.moon.angularRadiusDegrees), dp(10f) * pixelAngle)
        val sunRadius = max(Math.toRadians(snapshot.sun.angularRadiusDegrees), dp(10f) * pixelAngle)
        skyShader.update(camera, snapshot, milkyWayStrength, moonRadius, sunRadius, if (layers.ground) 0.94f else 0f)
        canvas.drawRect(0f, 0f, camera.width, camera.height, backgroundPaint)

        val darkness = (((2.0 - snapshot.sun.altitude) / 14.0).coerceIn(0.0, 1.0)).toFloat()
        val chromeFade = 0.3f + 0.7f * darkness
        val focus = (state.target as? SkyObjectRef.Constellation)?.index ?: (state.selected as? SkyObjectRef.Constellation)?.index
        if (layers.grid) drawGrid(canvas, camera)
        if (layers.constellationLines) drawConstellationLines(canvas, camera, snapshot, layers.ground, chromeFade * if (focus != null) 0.55f else 1f)
        drawStars(canvas, camera, snapshot, limit, layers.ground, picks)
        drawHorizon(canvas, camera)
        if (layers.qibla) drawQibla(canvas, camera, state, picks)
        drawBodies(canvas, camera, snapshot, layers.ground, picks, moonRadius, sunRadius, limit, state)
        focus?.let { drawFocusedConstellation(canvas, camera, snapshot, it, state, layers.ground) }
        if (layers.starNames) drawStarLabels(canvas, camera, snapshot, limit, layers.ground)
        if (layers.constellationNames) drawConstellationNames(canvas, camera, snapshot, layers.ground, picks, chromeFade, focus)
        state.selected?.let { if (it !is SkyObjectRef.Constellation) drawHighlight(canvas, camera, snapshot, it, state) }
        state.target?.let { drawGuide(canvas, camera, snapshot, it, state) }
        if (state.showReticle) drawReticle(canvas, camera, snapshot, state)
    }

    private fun limitingMagnitude(snapshot: SkySnapshot): Float {
        val sunAltitude = snapshot.sun.altitude
        val base = when {
            sunAltitude >= 0 -> -1.6
            sunAltitude >= -6 -> lerp(1.0, -1.6, (sunAltitude + 6) / 6)
            sunAltitude >= -12 -> lerp(3.6, 1.0, (sunAltitude + 12) / 6)
            sunAltitude >= -18 -> lerp(5.6, 3.6, (sunAltitude + 18) / 6)
            else -> 6.3
        }
        val moon = snapshot.moon
        val moonPenalty = if (moon.altitude > 0) 0.9 * moon.phaseFraction else 0.0
        return (base - moonPenalty).toFloat()
    }

    private fun zoomBoost(camera: SkyCamera): Float = (78.0 / camera.fovDegrees).pow(0.3).toFloat().coerceIn(0.85f, 2.2f)

    private fun starDiameter(magnitude: Float, limit: Float, zoom: Float): Float {
        val relative = (limit - magnitude).coerceAtLeast(0f)
        return dp(0.85f + 0.42f * relative.pow(1.05f)) * zoom
    }

    private fun starAlpha(magnitude: Float, limit: Float): Int {
        val relative = (limit - magnitude).coerceIn(0f, 1.2f)
        return (255f * (0.30f + 0.70f * relative / 1.2f)).toInt()
    }

    private fun drawStars(canvas: Canvas, camera: SkyCamera, snapshot: SkySnapshot, limit: Float, ground: Boolean, picks: PickBuffer) {
        val m = snapshot.eqjToEnu
        val f0 = camera.forward[0]; val f1 = camera.forward[1]; val f2 = camera.forward[2]
        val threshold = camera.visibleCosine
        val positions = stars.positions
        val magnitudes = stars.magnitudes
        classCounts.fill(0)
        glowCount = 0
        val margin = dp(12f)
        for (i in 0 until stars.count) {
            val magnitude = magnitudes[i]
            if (magnitude > limit + 0.3f) break
            val x = positions[i * 3].toDouble()
            val y = positions[i * 3 + 1].toDouble()
            val z = positions[i * 3 + 2].toDouble()
            val east = m[0] * x + m[1] * y + m[2] * z
            val north = m[3] * x + m[4] * y + m[5] * z
            val upward = m[6] * x + m[7] * y + m[8] * z
            if (ground && upward < -0.004) continue
            if (east * f0 + north * f1 + upward * f2 < threshold) continue
            val extinction = if (upward < 0.17) ((0.17 - upward) * 6.0).toFloat() else 0f
            if (magnitude + extinction > limit) continue
            if (!camera.project(east, north, upward, point)) continue
            if (!camera.isOnScreen(point[0], point[1], margin)) continue
            val cls = classOf[i]
            val offset = classCounts[cls] * 2
            val buffer = classBuffers[cls]
            buffer[offset] = point[0]
            buffer[offset + 1] = point[1]
            classCounts[cls]++
            if (magnitude < 5.2f) picks.add(PickBuffer.KIND_STAR, i, point[0], point[1], 5.5f - magnitude)
            if (magnitude < 1.7f && glowCount < glowX.size) {
                glowX[glowCount] = point[0]
                glowY[glowCount] = point[1]
                glowIndex[glowCount] = i
                glowCount++
            }
        }
        val zoom = zoomBoost(camera)
        for (g in 0 until glowCount) {
            val index = glowIndex[g]
            val magnitude = magnitudes[index]
            val size = starDiameter(magnitude, limit, zoom) * 3.4f
            val paint = glowPaints[stars.colorBuckets[index].toInt()]
            paint.alpha = (110 * ((limit - magnitude) / 5f).coerceIn(0.25f, 1f)).toInt()
            glowRect.set(glowX[g] - size, glowY[g] - size, glowX[g] + size, glowY[g] + size)
            canvas.drawBitmap(glowBitmap, null, glowRect, paint)
        }
        for (cls in classCounts.indices) {
            val count = classCounts[cls]
            if (count == 0) continue
            val bin = cls / colorCount
            val bucket = cls % colorCount
            val magnitude = binMagnitude(bin)
            val paint = starPaints[bucket]
            paint.strokeWidth = starDiameter(magnitude, limit, zoom)
            paint.alpha = starAlpha(magnitude, limit)
            canvas.drawPoints(classBuffers[cls], 0, count * 2, paint)
        }
    }

    private fun drawConstellationLines(canvas: Canvas, camera: SkyCamera, snapshot: SkySnapshot, ground: Boolean, fade: Float) {
        val m = snapshot.eqjToEnu
        var bright = 0
        var dim = 0
        val threshold = min(camera.visibleCosine, 0.0) - 0.25
        for (figure in catalog.constellations) {
            val s = figure.segments
            for (seg in 0 until figure.segmentCount) {
                val o = seg * 6
                val ax = m[0] * s[o] + m[1] * s[o + 1] + m[2] * s[o + 2]
                val ay = m[3] * s[o] + m[4] * s[o + 1] + m[5] * s[o + 2]
                val az = m[6] * s[o] + m[7] * s[o + 1] + m[8] * s[o + 2]
                val bx = m[0] * s[o + 3] + m[1] * s[o + 4] + m[2] * s[o + 5]
                val by = m[3] * s[o + 3] + m[4] * s[o + 4] + m[5] * s[o + 5]
                val bz = m[6] * s[o + 3] + m[7] * s[o + 4] + m[8] * s[o + 5]
                if (camera.depth(ax, ay, az) < threshold || camera.depth(bx, by, bz) < threshold) continue
                if (!camera.project(ax, ay, az, point) || !camera.project(bx, by, bz, point2)) continue
                if (!segmentVisible(camera)) continue
                val below = ground && az < 0 && bz < 0
                if (below) {
                    dimLineBuffer[dim++] = point[0]; dimLineBuffer[dim++] = point[1]
                    dimLineBuffer[dim++] = point2[0]; dimLineBuffer[dim++] = point2[1]
                } else {
                    lineBuffer[bright++] = point[0]; lineBuffer[bright++] = point[1]
                    lineBuffer[bright++] = point2[0]; lineBuffer[bright++] = point2[1]
                }
            }
        }
        linePaint.alpha = (64 * fade).toInt()
        if (bright > 0) canvas.drawLines(lineBuffer, 0, bright, linePaint)
        linePaint.alpha = (18 * fade).toInt()
        if (dim > 0) canvas.drawLines(dimLineBuffer, 0, dim, linePaint)
    }

    private fun segmentVisible(camera: SkyCamera): Boolean {
        val minX = min(point[0], point2[0]); val maxX = max(point[0], point2[0])
        val minY = min(point[1], point2[1]); val maxY = max(point[1], point2[1])
        if (maxX - minX > camera.width * 3 || maxY - minY > camera.height * 3) return false
        return maxX > 0 && minX < camera.width && maxY > 0 && minY < camera.height
    }

    private fun horizontalPoint(camera: SkyCamera, azimuthDegrees: Double, altitudeDegrees: Double, out: FloatArray): Boolean {
        val azimuth = Math.toRadians(azimuthDegrees)
        val altitude = Math.toRadians(altitudeDegrees)
        val x = cos(altitude) * sin(azimuth)
        val y = cos(altitude) * cos(azimuth)
        val z = sin(altitude)
        if (camera.depth(x, y, z) < -0.35) return false
        return camera.project(x, y, z, out)
    }

    private fun drawGrid(canvas: Canvas, camera: SkyCamera) {
        drawAltitudeCircle(canvas, camera, gridPaint, 30.0, 2.0)
        drawAltitudeCircle(canvas, camera, gridPaint, 60.0, 2.0)
        var azimuth = 0.0
        while (azimuth < 360.0) {
            drawAzimuthArc(canvas, camera, gridPaint, azimuth)
            azimuth += 30.0
        }
    }

    private fun drawAltitudeCircle(canvas: Canvas, camera: SkyCamera, paint: Paint, altitude: Double, step: Double) {
        path.reset()
        var penDown = false
        var azimuth = 0.0
        while (azimuth <= 360.0 + 1e-9) {
            penDown = extendPath(camera, horizontalPoint(camera, azimuth, altitude, point), penDown)
            azimuth += step
        }
        canvas.drawPath(path, paint)
    }

    private fun drawAzimuthArc(canvas: Canvas, camera: SkyCamera, paint: Paint, azimuth: Double) {
        path.reset()
        var penDown = false
        var altitude = 0.0
        while (altitude <= 88.0) {
            penDown = extendPath(camera, horizontalPoint(camera, azimuth, altitude, point), penDown)
            altitude += 2.0
        }
        canvas.drawPath(path, paint)
    }

    private var previousPathX = 0f
    private var previousPathY = 0f

    private fun extendPath(camera: SkyCamera, visible: Boolean, penDown: Boolean): Boolean {
        if (!visible) return false
        val jump = penDown && (abs(point[0] - previousPathX) > camera.width || abs(point[1] - previousPathY) > camera.height)
        if (!penDown || jump) path.moveTo(point[0], point[1]) else path.lineTo(point[0], point[1])
        previousPathX = point[0]
        previousPathY = point[1]
        return true
    }

    private fun drawHorizon(canvas: Canvas, camera: SkyCamera) {
        horizonPaint.alpha = 150
        drawAltitudeCircle(canvas, camera, horizonPaint, 0.0, 1.0)
        for (azimuth in 0 until 360 step 5) {
            val length = when {
                azimuth % 45 == 0 -> 2.2
                azimuth % 15 == 0 -> 1.3
                else -> 0.6
            }
            if (!horizontalPoint(camera, azimuth.toDouble(), 0.0, point)) continue
            if (!horizontalPoint(camera, azimuth.toDouble(), length, point2)) continue
            if (!camera.isOnScreen(point[0], point[1], dp(40f))) continue
            tickPaint.alpha = if (azimuth % 45 == 0) 190 else 110
            canvas.drawLine(point[0], point[1], point2[0], point2[1], tickPaint)
            if (azimuth % 45 == 0) {
                if (horizontalPoint(camera, azimuth.toDouble(), 3.6, point2)) {
                    cardinalPaint.alpha = if (azimuth % 90 == 0) 235 else 150
                    cardinalPaint.textSize = dp(if (azimuth % 90 == 0) 15f else 12f)
                    canvas.drawText(CARDINALS[azimuth / 45], point2[0], point2[1], cardinalPaint)
                }
            } else if (azimuth % 15 == 0) {
                if (horizontalPoint(camera, azimuth.toDouble(), 2.4, point2)) {
                    canvas.drawText(DEGREE_LABELS[azimuth / 15], point2[0], point2[1], degreePaint)
                }
            }
        }
    }

    private fun drawQibla(canvas: Canvas, camera: SkyCamera, state: SkyRenderState, picks: PickBuffer) {
        val azimuth = state.qiblaAzimuth
        if (!horizontalPoint(camera, azimuth, 0.0, point)) return
        if (!horizontalPoint(camera, azimuth, 5.5, point2)) return
        if (!camera.isOnScreen(point[0], point[1], dp(60f))) return
        qiblaStroke.alpha = 210
        canvas.drawLine(point[0], point[1], point2[0], point2[1], qiblaStroke)
        val size = dp(9f)
        val cx = point2[0]
        val cy = point2[1] - size * 0.9f
        qiblaFill.alpha = 235
        canvas.drawRect(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2, qiblaFill)
        qiblaStroke.alpha = 255
        qiblaFill.color = 0xFF0A0B0F.toInt()
        canvas.drawRect(cx - size / 2, cy - size * 0.22f, cx + size / 2, cy - size * 0.08f, qiblaFill)
        qiblaFill.color = white
        bodyLabelPaint.alpha = 240
        canvas.drawText("Kıble", cx + size, cy + dp(4f), bodyLabelPaint)
        if (state.qiblaAligned) {
            val pulse = (sin(state.seconds * 4f) * 0.5f + 0.5f)
            highlightPaint.alpha = (120 + 120 * pulse).toInt()
            canvas.drawCircle(cx, cy, dp(15f) + dp(4f) * pulse, highlightPaint)
        }
        picks.add(PickBuffer.KIND_QIBLA, 0, cx, cy, 8f)
        registerLabel(cx - size, cy - size, cx + size + bodyLabelPaint.measureText("Kıble") + dp(4f), cy + size)
    }

    private fun drawBodies(
        canvas: Canvas,
        camera: SkyCamera,
        snapshot: SkySnapshot,
        ground: Boolean,
        picks: PickBuffer,
        moonRadius: Double,
        sunRadius: Double,
        limit: Float,
        state: SkyRenderState,
    ) {
        val zoom = zoomBoost(camera)
        for (index in snapshot.bodies.indices.reversed()) {
            val body = snapshot.bodies[index]
            val direction = body.direction
            val isLuminary = body.body == SkyBody.Sun || body.body == SkyBody.Moon
            val focused = (state.selected as? SkyObjectRef.Body)?.body == body.body || (state.target as? SkyObjectRef.Body)?.body == body.body
            if (!isLuminary && !focused && body.magnitude > limit + 1.0) continue
            if (camera.depth(direction.x, direction.y, direction.z) < camera.visibleCosine) continue
            if (!camera.project(direction, point)) continue
            if (!camera.isOnScreen(point[0], point[1], dp(30f))) continue
            val belowHorizon = ground && body.altitude < -0.8
            val labelOffset: Float
            when (body.body) {
                SkyBody.Sun -> labelOffset = (sunRadius * camera.scale).toFloat() + dp(8f)
                SkyBody.Moon -> labelOffset = (moonRadius * camera.scale).toFloat() + dp(8f)
                else -> {
                    val diameter = max(starDiameter(body.magnitude.toFloat().coerceAtLeast(-4.5f), 6.3f, zoom) * 0.95f, dp(3.2f))
                    val glowSize = diameter * 2.6f
                    val glowPaint = bodyGlowPaints[body.body.ordinal]
                    glowPaint.alpha = if (belowHorizon) 40 else 150
                    glowRect.set(point[0] - glowSize, point[1] - glowSize, point[0] + glowSize, point[1] + glowSize)
                    canvas.drawBitmap(glowBitmap, null, glowRect, glowPaint)
                    bodyPaint.color = body.body.color
                    bodyPaint.alpha = if (belowHorizon) 90 else 255
                    canvas.drawCircle(point[0], point[1], diameter / 2f, bodyPaint)
                    labelOffset = diameter / 2f + dp(6f)
                }
            }
            picks.add(PickBuffer.KIND_BODY, body.body.ordinal, point[0], point[1], 12f)
            drawBodyLabel(canvas, body, labelOffset, belowHorizon, camera.width)
        }
    }

    private fun drawBodyLabel(canvas: Canvas, body: BodyState, offset: Float, belowHorizon: Boolean, screenWidth: Float) {
        val label = body.body.displayName
        bodyLabelPaint.alpha = if (belowHorizon) 110 else 245
        val width = bodyLabelPaint.measureText(label)
        val textSize = bodyLabelPaint.textSize
        val candidates = if (body.body == SkyBody.Sun || body.body == SkyBody.Moon) {
            floatArrayOf(
                point[0] - width / 2f, point[1] + offset + textSize * 0.8f,
                point[0] - width / 2f, point[1] - offset - textSize * 0.2f,
                point[0] + offset, point[1] + textSize * 0.36f,
            )
        } else {
            val right = point[0] + offset
            val left = point[0] - offset - width
            val preferRight = right + width < screenWidth - dp(8f)
            floatArrayOf(
                if (preferRight) right else left, point[1] + textSize * 0.36f,
                if (preferRight) left else right, point[1] + textSize * 0.36f,
                point[0] - width / 2f, point[1] + offset + textSize,
            )
        }
        var i = 0
        while (i < candidates.size) {
            val x = candidates[i]
            val y = candidates[i + 1]
            if (registerLabel(x, y - textSize, x + width, y + dp(2f))) {
                canvas.drawText(label, x, y, bodyLabelPaint)
                return
            }
            i += 2
        }
        if (!belowHorizon) canvas.drawText(label, candidates[0], candidates[1], bodyLabelPaint)
    }

    private fun drawStarLabels(canvas: Canvas, camera: SkyCamera, snapshot: SkySnapshot, limit: Float, ground: Boolean) {
        val m = snapshot.eqjToEnu
        val labelLimit = min(limit - 0.8f, (1.6 + (90.0 - camera.fovDegrees).coerceAtLeast(0.0) / 26.0).toFloat())
        val zoom = zoomBoost(camera)
        for (index in namedStarsByBrightness) {
            val magnitude = stars.magnitudes[index]
            if (magnitude > labelLimit) break
            val x = stars.positions[index * 3].toDouble()
            val y = stars.positions[index * 3 + 1].toDouble()
            val z = stars.positions[index * 3 + 2].toDouble()
            val east = m[0] * x + m[1] * y + m[2] * z
            val north = m[3] * x + m[4] * y + m[5] * z
            val upward = m[6] * x + m[7] * y + m[8] * z
            if (ground && upward < 0.0) continue
            if (camera.depth(east, north, upward) < camera.visibleCosine) continue
            if (!camera.project(east, north, upward, point)) continue
            if (!camera.isOnScreen(point[0], point[1], 0f)) continue
            val label = stars.meta[index].properName ?: continue
            val radius = starDiameter(magnitude, limit, zoom) / 2f
            val width = starLabelPaint.measureText(label)
            val left = if (point[0] + radius + dp(5f) + width < camera.width - dp(6f)) point[0] + radius + dp(5f) else point[0] - radius - dp(5f) - width
            val baseline = point[1] + starLabelPaint.textSize * 0.36f
            if (!registerLabel(left, baseline - starLabelPaint.textSize, left + width, baseline + dp(2f))) continue
            starLabelPaint.alpha = (150 + 90 * ((labelLimit - magnitude) / 2f).coerceIn(0f, 1f)).toInt()
            canvas.drawText(label, left, baseline, starLabelPaint)
        }
    }

    private fun drawConstellationNames(canvas: Canvas, camera: SkyCamera, snapshot: SkySnapshot, ground: Boolean, picks: PickBuffer, fade: Float, skip: Int?) {
        if (camera.fovDegrees > 120) return
        val m = snapshot.eqjToEnu
        catalog.constellations.forEachIndexed { index, figure ->
            if (index == skip) return@forEachIndexed
            val label = figure.label
            val east = m[0] * label.x + m[1] * label.y + m[2] * label.z
            val north = m[3] * label.x + m[4] * label.y + m[5] * label.z
            val upward = m[6] * label.x + m[7] * label.y + m[8] * label.z
            if (ground && upward < 0.02) return@forEachIndexed
            if (camera.depth(east, north, upward) < max(camera.visibleCosine, 0.2)) return@forEachIndexed
            if (!camera.project(east, north, upward, point)) return@forEachIndexed
            if (!camera.isOnScreen(point[0], point[1], -dp(20f))) return@forEachIndexed
            val text = Constellations.turkishName(figure.code)
            val width = constellationPaint.measureText(text)
            if (!registerLabel(point[0] - width / 2, point[1] - constellationPaint.textSize, point[0] + width / 2, point[1] + dp(3f))) return@forEachIndexed
            constellationPaint.alpha = (105 * fade).toInt()
            canvas.drawText(text, point[0], point[1], constellationPaint)
            picks.add(PickBuffer.KIND_CONSTELLATION, index, point[0], point[1] - dp(4f), 0f)
        }
    }

    private fun drawHighlight(canvas: Canvas, camera: SkyCamera, snapshot: SkySnapshot, selected: SkyObjectRef, state: SkyRenderState) {
        val direction = selected.direction(catalog, snapshot, state.qiblaAzimuth)
        if (camera.depth(direction.x, direction.y, direction.z) < 0) return
        if (!camera.project(direction, point)) return
        val radius = when (selected) {
            is SkyObjectRef.Body -> when (selected.body) {
                SkyBody.Sun, SkyBody.Moon -> dp(26f)
                else -> dp(17f)
            }
            else -> dp(16f)
        }
        highlightPaint.alpha = 230
        canvas.drawCircle(point[0], point[1], radius, highlightPaint)
        for (i in 0 until 4) {
            val angle = Math.toRadians(45.0 + i * 90.0)
            val c = cos(angle).toFloat()
            val s = sin(angle).toFloat()
            canvas.drawLine(
                point[0] + c * (radius + dp(3f)), point[1] + s * (radius + dp(3f)),
                point[0] + c * (radius + dp(8f)), point[1] + s * (radius + dp(8f)),
                highlightPaint,
            )
        }
    }

    private fun drawFocusedConstellation(canvas: Canvas, camera: SkyCamera, snapshot: SkySnapshot, index: Int, state: SkyRenderState, ground: Boolean) {
        val figure = catalog.constellations[index]
        val shape = catalog.shapes[index]
        val isTarget = (state.target as? SkyObjectRef.Constellation)?.index == index
        val progress = when {
            !isTarget -> 1f
            state.targetRevealStart < 0f -> return
            else -> ((state.seconds - state.targetRevealStart) / REVEAL_SECONDS).coerceIn(0f, 1f)
        }
        val eased = 1f - (1f - progress).pow(3)
        val m = snapshot.eqjToEnu
        val vertexCount = min(shape.vertexCount, MAX_FOCUS_VERTICES)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        var visible = 0
        val v = shape.vertices
        for (i in 0 until vertexCount) {
            val x = m[0] * v[i * 3] + m[1] * v[i * 3 + 1] + m[2] * v[i * 3 + 2]
            val y = m[3] * v[i * 3] + m[4] * v[i * 3 + 1] + m[5] * v[i * 3 + 2]
            val z = m[6] * v[i * 3] + m[7] * v[i * 3 + 1] + m[8] * v[i * 3 + 2]
            focusVisible[i] = camera.depth(x, y, z) > 0.05 && camera.project(x, y, z, point)
            if (!focusVisible[i]) continue
            focusX[i] = point[0]
            focusY[i] = point[1]
            minX = min(minX, point[0]); maxX = max(maxX, point[0])
            minY = min(minY, point[1]); maxY = max(maxY, point[1])
            visible++
        }
        val s = figure.segments
        val drawn = eased * figure.segmentCount
        for (seg in 0 until figure.segmentCount) {
            val local = (drawn - seg).coerceIn(0f, 1f)
            if (local <= 0f) break
            val o = seg * 6
            val ax = m[0] * s[o] + m[1] * s[o + 1] + m[2] * s[o + 2]
            val ay = m[3] * s[o] + m[4] * s[o + 1] + m[5] * s[o + 2]
            val az = m[6] * s[o] + m[7] * s[o + 1] + m[8] * s[o + 2]
            val bx = m[0] * s[o + 3] + m[1] * s[o + 4] + m[2] * s[o + 5]
            val by = m[3] * s[o + 3] + m[4] * s[o + 4] + m[5] * s[o + 5]
            val bz = m[6] * s[o + 3] + m[7] * s[o + 4] + m[8] * s[o + 5]
            if (camera.depth(ax, ay, az) < 0.05 || camera.depth(bx, by, bz) < 0.05) continue
            if (!camera.project(ax, ay, az, point) || !camera.project(bx, by, bz, point2)) continue
            if (!segmentVisible(camera)) continue
            val ex = point[0] + (point2[0] - point[0]) * local
            val ey = point[1] + (point2[1] - point[1]) * local
            val buried = if (ground && az < 0 && bz < 0) 0.35f else 1f
            focusGlowPaint.alpha = (34 * buried).toInt()
            canvas.drawLine(point[0], point[1], ex, ey, focusGlowPaint)
            focusLinePaint.alpha = (235 * buried).toInt()
            canvas.drawLine(point[0], point[1], ex, ey, focusLinePaint)
        }
        for (i in 0 until vertexCount) {
            if (!focusVisible[i]) continue
            val buried = if (ground && m[6] * v[i * 3] + m[7] * v[i * 3 + 1] + m[8] * v[i * 3 + 2] < 0) 0.35f else 1f
            vertexPaint.alpha = (215 * eased * buried).toInt()
            canvas.drawCircle(focusX[i], focusY[i], dp(2.2f), vertexPaint)
        }
        if (visible < 2) return
        if (maxX < camera.safeLeft || minX > camera.safeRight || maxY < camera.safeTop || minY > camera.safeBottom) return
        val pad = dp(22f) * (1f + 0.6f * (1f - eased))
        val left = minX - pad
        val top = minY - pad
        val right = maxX + pad
        val bottom = maxY + pad
        val corner = min(dp(26f), min(right - left, bottom - top) / 4f)
        framePaint.alpha = (200 * eased).toInt()
        canvas.drawLine(left, top, left + corner, top, framePaint)
        canvas.drawLine(left, top, left, top + corner, framePaint)
        canvas.drawLine(right, top, right - corner, top, framePaint)
        canvas.drawLine(right, top, right, top + corner, framePaint)
        canvas.drawLine(left, bottom, left + corner, bottom, framePaint)
        canvas.drawLine(left, bottom, left, bottom - corner, framePaint)
        canvas.drawLine(right, bottom, right - corner, bottom, framePaint)
        canvas.drawLine(right, bottom, right, bottom - corner, framePaint)
        val title = focusTitles[index]
        val subtitle = focusSubtitles[index]
        val titleWidth = focusTitlePaint.measureText(title)
        val subtitleWidth = focusSubtitlePaint.measureText(subtitle)
        val blockWidth = max(titleWidth, subtitleWidth)
        val minTop = camera.safeTop + dp(8f)
        val maxBottom = camera.safeBottom
        var titleBaseline = top - dp(26f)
        if (titleBaseline - focusTitlePaint.textSize < minTop) titleBaseline = bottom + dp(26f)
        if (titleBaseline + dp(20f) > maxBottom) titleBaseline = minTop + focusTitlePaint.textSize
        val textLeft = left.coerceIn(dp(16f), max(dp(16f), camera.width - blockWidth - dp(16f)))
        registerLabel(textLeft, titleBaseline - focusTitlePaint.textSize, textLeft + blockWidth, titleBaseline + dp(20f))
        focusTitlePaint.alpha = (250 * eased).toInt()
        canvas.drawText(title, textLeft, titleBaseline, focusTitlePaint)
        focusSubtitlePaint.alpha = (175 * eased).toInt()
        canvas.drawText(subtitle, textLeft, titleBaseline + dp(17f), focusSubtitlePaint)
        for (starIndex in focusMembers[index]) {
            val direction = snapshot.eqjToEnu.transform(stars.direction(starIndex))
            if (camera.depth(direction.x, direction.y, direction.z) < 0.05 || !camera.project(direction, point)) continue
            starLabelPaint.alpha = (225 * eased * if (ground && direction.z < 0) 0.45f else 1f).toInt()
            if (!camera.isOnScreen(point[0], point[1], 0f)) continue
            val label = stars.meta[starIndex].properName ?: continue
            val width = starLabelPaint.measureText(label)
            val labelLeft = if (point[0] + dp(7f) + width < camera.width - dp(6f)) point[0] + dp(7f) else point[0] - dp(7f) - width
            val baseline = point[1] + starLabelPaint.textSize * 0.36f
            if (registerLabel(labelLeft, baseline - starLabelPaint.textSize, labelLeft + width, baseline + dp(2f))) {
                canvas.drawText(label, labelLeft, baseline, starLabelPaint)
            }
        }
    }

    private fun drawGuide(canvas: Canvas, camera: SkyCamera, snapshot: SkySnapshot, target: SkyObjectRef, state: SkyRenderState) {
        val direction = target.direction(catalog, snapshot, state.qiblaAzimuth)
        val depth = camera.depth(direction.x, direction.y, direction.z)
        val projected = depth > 0.2 && camera.project(direction, point)
        if (projected && camera.inSafeArea(point[0], point[1])) {
            if (target is SkyObjectRef.Constellation) return
            val pulse = sin(state.seconds * 3f) * 0.5f + 0.5f
            highlightPaint.alpha = (150 + 100 * pulse).toInt()
            canvas.drawCircle(point[0], point[1], dp(24f) + dp(5f) * pulse, highlightPaint)
            return
        }
        val below = direction.z < -0.009
        drawRoad(canvas, camera, direction, state, if (below) 0.55f else 1f)
        val (cx, cy) = camera.screenDirection(direction)
        val angle = atan2(-cy, cx)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        val margin = dp(26f)
        val scaleX = when {
            dx > 1e-4f -> (camera.safeRight - margin - camera.centerX) / dx
            dx < -1e-4f -> (camera.safeLeft + margin - camera.centerX) / dx
            else -> Float.MAX_VALUE
        }
        val scaleY = when {
            dy > 1e-4f -> (camera.safeBottom - margin - camera.centerY) / dy
            dy < -1e-4f -> (camera.safeTop + margin - camera.centerY) / dy
            else -> Float.MAX_VALUE
        }
        val reach = min(scaleX, scaleY).coerceAtLeast(0f)
        val ax = camera.centerX + dx * reach
        val ay = camera.centerY + dy * reach
        val pulse = sin(state.seconds * 4f) * 0.5f + 0.5f
        val size = dp(14f) + dp(2f) * pulse
        path.reset()
        path.moveTo(ax - dx * size - dy * size * 0.8f, ay - dy * size + dx * size * 0.8f)
        path.lineTo(ax, ay)
        path.lineTo(ax - dx * size + dy * size * 0.8f, ay - dy * size - dx * size * 0.8f)
        guidePaint.alpha = if (below) 170 else 245
        canvas.drawPath(path, guidePaint)
        val name = target.displayName(catalog)
        val separation = Math.toDegrees(acos(depth.coerceIn(-1.0, 1.0))).roundToInt()
        val detail = if (below) "$separation° · ufkun altında" else "$separation°"
        val labelX = ax - dx * dp(40f)
        val labelY = ay - dy * dp(40f)
        guideLabelPaint.alpha = 245
        canvas.drawText(name, labelX, labelY, guideLabelPaint)
        guideLabelPaint.alpha = 170
        canvas.drawText(detail, labelX, labelY + guideLabelPaint.textSize * 1.25f, guideLabelPaint)
    }

    private fun drawRoad(canvas: Canvas, camera: SkyCamera, target: Vec3, state: SkyRenderState, strength: Float) {
        val fx = camera.forward[0]
        val fy = camera.forward[1]
        val fz = camera.forward[2]
        val theta = acos((fx * target.x + fy * target.y + fz * target.z).coerceIn(-1.0, 1.0))
        if (theta < Math.toRadians(3.0) || theta > Math.toRadians(172.0)) return
        val sinTheta = sin(theta)
        var count = 0
        for (i in 0..ROAD_STEPS) {
            val fraction = i.toDouble() / ROAD_STEPS
            val a = sin((1.0 - fraction) * theta) / sinTheta
            val b = sin(fraction * theta) / sinTheta
            val x = a * fx + b * target.x
            val y = a * fy + b * target.y
            val z = a * fz + b * target.z
            if (camera.depth(x, y, z) < 0.05 || !camera.project(x, y, z, point)) break
            roadX[count] = point[0]
            roadY[count] = point[1]
            count++
            if (!camera.inSafeArea(point[0], point[1])) break
        }
        if (count < 2) return
        path.reset()
        path.moveTo(roadX[0], roadY[0])
        for (i in 1 until count) path.lineTo(roadX[i], roadY[i])
        roadPaint.alpha = (60 * strength).toInt()
        canvas.drawPath(path, roadPaint)
        val spacing = dp(46f)
        val start = dp(30f)
        var next = start + spacing - (state.seconds * dp(38f)) % spacing
        var travelled = 0f
        for (i in 1 until count) {
            val dx = roadX[i] - roadX[i - 1]
            val dy = roadY[i] - roadY[i - 1]
            val segment = hypot(dx, dy)
            if (segment < 1e-3f) continue
            while (travelled + segment >= next) {
                val t = (next - travelled) / segment
                val ramp = ((next - start) / dp(70f)).coerceIn(0f, 1f)
                drawChevron(canvas, roadX[i - 1] + dx * t, roadY[i - 1] + dy * t, dx / segment, dy / segment, (215 * ramp * strength).toInt())
                next += spacing
            }
            travelled += segment
        }
    }

    private fun drawChevron(canvas: Canvas, x: Float, y: Float, ux: Float, uy: Float, alpha: Int) {
        val size = dp(6.5f)
        path.reset()
        path.moveTo(x - ux * size - uy * size * 0.8f, y - uy * size + ux * size * 0.8f)
        path.lineTo(x, y)
        path.lineTo(x - ux * size + uy * size * 0.8f, y - uy * size - ux * size * 0.8f)
        chevronPaint.alpha = alpha
        canvas.drawPath(path, chevronPaint)
    }

    private fun drawReticle(canvas: Canvas, camera: SkyCamera, snapshot: SkySnapshot, state: SkyRenderState) {
        highlightPaint.alpha = 110
        val cx = camera.centerX
        val cy = camera.centerY
        val gap = dp(6f)
        val arm = dp(14f)
        canvas.drawLine(cx - arm, cy, cx - gap, cy, highlightPaint)
        canvas.drawLine(cx + gap, cy, cx + arm, cy, highlightPaint)
        canvas.drawLine(cx, cy - arm, cx, cy - gap, highlightPaint)
        canvas.drawLine(cx, cy + gap, cx, cy + arm, highlightPaint)
        if (!state.sensorView) return
        val forward = camera.forward
        val drift = forward[0] * reticleForward[0] + forward[1] * reticleForward[1] + forward[2] * reticleForward[2]
        if (drift < RETICLE_REFRESH_COSINE || abs(snapshot.millis - reticleMillis) > 60_000L) {
            forward.copyInto(reticleForward)
            reticleMillis = snapshot.millis
            reticleConstellation = constellationAt(snapshot.equatorialDirection(Vec3(forward[0], forward[1], forward[2])))
        }
        val best = reticleConstellation
        if (best < 0) return
        reticleLabelPaint.alpha = 165
        canvas.drawText(focusTitles[best], cx, cy + arm + dp(18f), reticleLabelPaint)
    }

    private fun constellationAt(center: Vec3): Int {
        var best = -1
        var bestScore = 1.0
        catalog.shapes.forEachIndexed { index, shape ->
            val score = shape.centroid.angleTo(center) / max(shape.radiusDegrees, 8.0)
            if (score < bestScore) {
                bestScore = score
                best = index
            }
        }
        return best
    }

    private fun registerLabel(left: Float, top: Float, right: Float, bottom: Float): Boolean {
        for (i in 0 until labelCount) {
            val r = labelRects[i]
            if (left < r.right && right > r.left && top < r.bottom && bottom > r.top) return false
        }
        if (labelCount >= labelRects.size) return false
        labelRects[labelCount++].set(left, top, right, bottom)
        return true
    }

    private fun createGlow(size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = size / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                radius, radius, radius,
                intArrayOf(0xFFFFFFFF.toInt(), 0x80FFFFFF.toInt(), 0x22FFFFFF, 0x00FFFFFF),
                floatArrayOf(0f, 0.12f, 0.42f, 1f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawCircle(radius, radius, radius, paint)
        return bitmap
    }

    companion object {
        private val CARDINALS = arrayOf("K", "KD", "D", "GD", "G", "GB", "B", "KB")
        private val DEGREE_LABELS = Array(24) { (it * 15).toString() }
        private const val MAGNITUDE_BINS = 18
        private const val MAX_FOCUS_VERTICES = 128
        private const val ROAD_STEPS = 96
        private const val REVEAL_SECONDS = 1.1f
        private const val RETICLE_REFRESH_COSINE = 0.99996
        private const val BIN_START = -1.75f
        private const val BIN_WIDTH = 0.5f

        fun magnitudeBin(magnitude: Float): Int = ((magnitude - BIN_START) / BIN_WIDTH).toInt().coerceIn(0, MAGNITUDE_BINS - 1)

        fun binMagnitude(bin: Int): Float = BIN_START + (bin + 0.5f) * BIN_WIDTH

        private fun lerp(from: Double, to: Double, t: Double) = from + (to - from) * t.coerceIn(0.0, 1.0)
    }
}
