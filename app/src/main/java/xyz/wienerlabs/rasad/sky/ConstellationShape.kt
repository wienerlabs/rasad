package xyz.wienerlabs.rasad.sky

import xyz.wienerlabs.rasad.astro.Vec3
import kotlin.math.roundToInt

class ConstellationShape(
    val centroid: Vec3,
    val radiusDegrees: Double,
    val vertices: FloatArray,
) {
    val vertexCount: Int get() = vertices.size / 3

    fun vertex(index: Int) = Vec3(vertices[index * 3].toDouble(), vertices[index * 3 + 1].toDouble(), vertices[index * 3 + 2].toDouble())

    fun framingFov(): Double = (radiusDegrees * FRAMING_FACTOR).coerceIn(MIN_FRAMING_FOV, MAX_FRAMING_FOV)

    fun foundThresholdDegrees(): Double = (radiusDegrees * 0.3).coerceAtLeast(MIN_FOUND_DEGREES)

    companion object {
        private const val FRAMING_FACTOR = 2.7
        private const val MIN_FRAMING_FOV = 30.0
        private const val MAX_FRAMING_FOV = 118.0
        private const val MIN_FOUND_DEGREES = 4.0
        private const val QUANTUM = 100_000.0
        private const val OFFSET = 1L shl 20

        fun of(figure: ConstellationFigure): ConstellationShape {
            val seen = HashSet<Long>()
            val coordinates = ArrayList<Float>()
            val source = figure.segments
            for (endpoint in 0 until figure.segmentCount * 2) {
                val x = source[endpoint * 3]
                val y = source[endpoint * 3 + 1]
                val z = source[endpoint * 3 + 2]
                if (seen.add(key(x, y, z))) {
                    coordinates += x
                    coordinates += y
                    coordinates += z
                }
            }
            val vertices = coordinates.toFloatArray()
            var sumX = 0.0
            var sumY = 0.0
            var sumZ = 0.0
            for (i in 0 until vertices.size / 3) {
                sumX += vertices[i * 3]
                sumY += vertices[i * 3 + 1]
                sumZ += vertices[i * 3 + 2]
            }
            val centroid = Vec3(sumX, sumY, sumZ).normalized()
            var radius = 0.0
            for (i in 0 until vertices.size / 3) {
                val vertex = Vec3(vertices[i * 3].toDouble(), vertices[i * 3 + 1].toDouble(), vertices[i * 3 + 2].toDouble())
                radius = maxOf(radius, centroid.angleTo(vertex))
            }
            return ConstellationShape(centroid, radius, vertices)
        }

        private fun key(x: Float, y: Float, z: Float): Long {
            val qx = (x * QUANTUM).roundToInt() + OFFSET
            val qy = (y * QUANTUM).roundToInt() + OFFSET
            val qz = (z * QUANTUM).roundToInt() + OFFSET
            return (qx shl 42) or (qy shl 21) or qz
        }
    }
}
