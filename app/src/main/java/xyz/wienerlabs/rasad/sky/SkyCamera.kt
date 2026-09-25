package xyz.wienerlabs.rasad.sky

import xyz.wienerlabs.rasad.astro.Vec3
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class SkyCamera {
    val right = doubleArrayOf(1.0, 0.0, 0.0)
    val up = doubleArrayOf(0.0, 0.0, 1.0)
    val forward = doubleArrayOf(0.0, 1.0, 0.0)

    var width = 1f
        private set
    var height = 1f
        private set
    var centerX = 0.5f
        private set
    var centerY = 0.5f
        private set
    var scale = 1.0
        private set
    var fovDegrees = DEFAULT_FOV
        private set
    var visibleCosine = 0.0
        private set

    fun setViewport(width: Float, height: Float) {
        if (width == this.width && height == this.height) return
        this.width = width.coerceAtLeast(1f)
        this.height = height.coerceAtLeast(1f)
        centerX = this.width / 2f
        centerY = this.height / 2f
        updateScale()
    }

    fun setFov(degrees: Double) {
        fovDegrees = degrees.coerceIn(MIN_FOV, MAX_FOV)
        updateScale()
    }

    private fun updateScale() {
        scale = (width / 2.0) / (2.0 * tan(Math.toRadians(fovDegrees) / 4.0))
        val halfDiagonal = hypot(width / 2.0, height / 2.0)
        val maxAngle = 2.0 * atan(halfDiagonal / (2.0 * scale)) + Math.toRadians(4.0)
        visibleCosine = cos(maxAngle.coerceAtMost(Math.PI * 0.97))
    }

    fun lookAt(azimuthDegrees: Double, altitudeDegrees: Double) {
        val azimuth = Math.toRadians(azimuthDegrees)
        val altitude = Math.toRadians(altitudeDegrees.coerceIn(-89.9, 89.9))
        forward[0] = cos(altitude) * sin(azimuth)
        forward[1] = cos(altitude) * cos(azimuth)
        forward[2] = sin(altitude)
        right[0] = cos(azimuth)
        right[1] = -sin(azimuth)
        right[2] = 0.0
        up[0] = right[1] * forward[2] - right[2] * forward[1]
        up[1] = right[2] * forward[0] - right[0] * forward[2]
        up[2] = right[0] * forward[1] - right[1] * forward[0]
    }

    fun setBasis(forwardVector: DoubleArray, upVector: DoubleArray) {
        normalizeInto(forwardVector, forward)
        val dot = upVector[0] * forward[0] + upVector[1] * forward[1] + upVector[2] * forward[2]
        val ux = upVector[0] - forward[0] * dot
        val uy = upVector[1] - forward[1] * dot
        val uz = upVector[2] - forward[2] * dot
        val length = sqrt(ux * ux + uy * uy + uz * uz).takeIf { it > 1e-9 } ?: return
        up[0] = ux / length; up[1] = uy / length; up[2] = uz / length
        right[0] = forward[1] * up[2] - forward[2] * up[1]
        right[1] = forward[2] * up[0] - forward[0] * up[2]
        right[2] = forward[0] * up[1] - forward[1] * up[0]
    }

    val centerAzimuth: Double
        get() = (Math.toDegrees(atan2(forward[0], forward[1])) + 360.0) % 360.0

    val centerAltitude: Double
        get() = Math.toDegrees(asin(forward[2].coerceIn(-1.0, 1.0)))

    fun depth(x: Double, y: Double, z: Double): Double = x * forward[0] + y * forward[1] + z * forward[2]

    fun project(x: Double, y: Double, z: Double, out: FloatArray): Boolean {
        val cz = x * forward[0] + y * forward[1] + z * forward[2]
        if (cz < -0.96) return false
        val cx = x * right[0] + y * right[1] + z * right[2]
        val cy = x * up[0] + y * up[1] + z * up[2]
        val k = 2.0 / (1.0 + cz) * scale
        out[0] = (centerX + cx * k).toFloat()
        out[1] = (centerY - cy * k).toFloat()
        return true
    }

    fun project(vector: Vec3, out: FloatArray): Boolean = project(vector.x, vector.y, vector.z, out)

    fun screenDirection(vector: Vec3): Pair<Double, Double> {
        val cx = vector.x * right[0] + vector.y * right[1] + vector.z * right[2]
        val cy = vector.x * up[0] + vector.y * up[1] + vector.z * up[2]
        return cx to cy
    }

    fun unproject(screenX: Float, screenY: Float): Vec3 {
        val px = (screenX - centerX) / scale
        val py = -(screenY - centerY) / scale
        val rho2 = px * px + py * py
        val inverse = 1.0 / (4.0 + rho2)
        val lx = 4.0 * px * inverse
        val ly = 4.0 * py * inverse
        val lz = (4.0 - rho2) * inverse
        return Vec3(
            lx * right[0] + ly * up[0] + lz * forward[0],
            lx * right[1] + ly * up[1] + lz * forward[1],
            lx * right[2] + ly * up[2] + lz * forward[2],
        ).normalized()
    }

    fun pixelsPerDegree(): Double = scale * Math.toRadians(1.0)

    fun isOnScreen(x: Float, y: Float, margin: Float): Boolean =
        x > -margin && y > -margin && x < width + margin && y < height + margin

    private fun normalizeInto(source: DoubleArray, target: DoubleArray) {
        val length = sqrt(source[0] * source[0] + source[1] * source[1] + source[2] * source[2]).takeIf { it > 1e-12 } ?: 1.0
        target[0] = source[0] / length
        target[1] = source[1] / length
        target[2] = source[2] / length
    }

    companion object {
        const val MIN_FOV = 6.0
        const val MAX_FOV = 150.0
        const val DEFAULT_FOV = 78.0
    }
}
