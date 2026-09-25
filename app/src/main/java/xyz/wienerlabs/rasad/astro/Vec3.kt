package xyz.wienerlabs.rasad.astro

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Double) = Vec3(x * scale, y * scale, z * scale)
    operator fun unaryMinus() = Vec3(-x, -y, -z)

    infix fun dot(other: Vec3) = x * other.x + y * other.y + z * other.z

    infix fun cross(other: Vec3) = Vec3(
        y * other.z - z * other.y,
        z * other.x - x * other.z,
        x * other.y - y * other.x,
    )

    fun length() = sqrt(this dot this)

    fun normalized(): Vec3 {
        val length = length()
        return if (length == 0.0) this else Vec3(x / length, y / length, z / length)
    }

    fun angleTo(other: Vec3): Double {
        val cross = (this cross other).length()
        return Math.toDegrees(atan2(cross, this dot other))
    }

    val azimuthDegrees: Double
        get() = (Math.toDegrees(atan2(x, y)) + 360.0) % 360.0

    val altitudeDegrees: Double
        get() = Math.toDegrees(asin((z / length()).coerceIn(-1.0, 1.0)))

    companion object {
        val Up = Vec3(0.0, 0.0, 1.0)

        fun fromAzimuthAltitude(azimuthDegrees: Double, altitudeDegrees: Double): Vec3 {
            val azimuth = Math.toRadians(azimuthDegrees)
            val altitude = Math.toRadians(altitudeDegrees)
            return Vec3(cos(altitude) * sin(azimuth), cos(altitude) * cos(azimuth), sin(altitude))
        }

        fun fromEquatorial(raHours: Double, decDegrees: Double): Vec3 {
            val ra = Math.toRadians(raHours * 15.0)
            val dec = Math.toRadians(decDegrees)
            return Vec3(cos(dec) * cos(ra), cos(dec) * sin(ra), sin(dec))
        }
    }
}

fun DoubleArray.transform(vector: Vec3): Vec3 = Vec3(
    this[0] * vector.x + this[1] * vector.y + this[2] * vector.z,
    this[3] * vector.x + this[4] * vector.y + this[5] * vector.z,
    this[6] * vector.x + this[7] * vector.y + this[8] * vector.z,
)

fun DoubleArray.transformTransposed(vector: Vec3): Vec3 = Vec3(
    this[0] * vector.x + this[3] * vector.y + this[6] * vector.z,
    this[1] * vector.x + this[4] * vector.y + this[7] * vector.z,
    this[2] * vector.x + this[5] * vector.y + this[8] * vector.z,
)
