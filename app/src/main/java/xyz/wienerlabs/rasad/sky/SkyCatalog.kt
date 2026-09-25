package xyz.wienerlabs.rasad.sky

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import xyz.wienerlabs.rasad.astro.Vec3
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class StarMeta(
    val index: Int,
    val hip: Int,
    val properName: String?,
    val bayer: String?,
    val flamsteed: String?,
    val constellation: String,
    val distanceParsecs: Double,
    val spectralType: String,
) {
    val designation: String?
        get() = when {
            bayer != null -> "$bayer $constellation"
            flamsteed != null -> "$flamsteed $constellation"
            hip > 0 -> "HIP $hip"
            else -> null
        }
}

class StarField(
    val count: Int,
    val positions: FloatArray,
    val magnitudes: FloatArray,
    val colorBuckets: ByteArray,
    val meta: Array<StarMeta>,
) {
    fun direction(index: Int) = Vec3(
        positions[index * 3].toDouble(),
        positions[index * 3 + 1].toDouble(),
        positions[index * 3 + 2].toDouble(),
    )

    fun raHours(index: Int): Double {
        val ra = Math.toDegrees(atan2(positions[index * 3 + 1].toDouble(), positions[index * 3].toDouble()))
        return ((ra + 360.0) % 360.0) / 15.0
    }

    fun decDegrees(index: Int): Double = Math.toDegrees(asin(positions[index * 3 + 2].toDouble().coerceIn(-1.0, 1.0)))

    fun indexOfName(name: String): Int? = meta.firstOrNull { it.properName.equals(name, ignoreCase = true) }?.index

    companion object {
        val bucketColors = intArrayOf(
            0xFFAFC6FF.toInt(),
            0xFFCFDBFF.toInt(),
            0xFFF3F5FF.toInt(),
            0xFFFFF5E6.toInt(),
            0xFFFFE3BF.toInt(),
            0xFFFFCB94.toInt(),
            0xFFFFB27E.toInt(),
        )

        fun bucketFor(colorIndex: Float): Int = when {
            colorIndex < -0.10f -> 0
            colorIndex < 0.15f -> 1
            colorIndex < 0.45f -> 2
            colorIndex < 0.75f -> 3
            colorIndex < 1.10f -> 4
            colorIndex < 1.50f -> 5
            else -> 6
        }
    }
}

class ConstellationFigure(
    val code: String,
    val label: Vec3,
    val rank: Int,
    val segments: FloatArray,
) {
    val segmentCount: Int get() = segments.size / 6
}

class SkyCatalog(
    val stars: StarField,
    val constellations: List<ConstellationFigure>,
    val milkyWay: Bitmap,
    val moonTexture: Bitmap,
    val land: List<FloatArray>,
) {
    val namedStars: List<StarMeta> by lazy { stars.meta.filter { it.properName != null } }

    companion object {
        fun load(context: Context): SkyCatalog {
            val assets = context.assets
            val stars = readStars(assets.open("stars.bin").use { it.readBytes() }, assets.open("stars_meta.tsv").bufferedReader().use { it.readLines() })
            val constellations = readConstellations(assets.open("constellations.bin").use { it.readBytes() })
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = 2
            }
            val milkyWay = assets.open("milkyway.png").use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("milkyway.png okunamadı")
            val moon = assets.open("moon.jpg").use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 })
            } ?: error("moon.jpg okunamadı")
            val land = readLand(assets.open("land.bin").use { it.readBytes() })
            return SkyCatalog(stars, constellations, milkyWay, moon, land)
        }

        private fun buffer(bytes: ByteArray): ByteBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        fun readStars(bytes: ByteArray, metaLines: List<String>): StarField {
            val data = buffer(bytes)
            val count = data.int
            val positions = FloatArray(count * 3)
            val magnitudes = FloatArray(count)
            val buckets = ByteArray(count)
            for (index in 0 until count) {
                positions[index * 3] = data.float
                positions[index * 3 + 1] = data.float
                positions[index * 3 + 2] = data.float
                magnitudes[index] = data.float
                buckets[index] = StarField.bucketFor(data.float).toByte()
                data.int
            }
            val meta = Array(count) { index -> StarMeta(index, 0, null, null, null, "", 0.0, "") }
            for (line in metaLines) {
                val fields = line.split('\t')
                if (fields.size < 8) continue
                val index = fields[0].toInt()
                if (index !in 0 until count) continue
                meta[index] = StarMeta(
                    index = index,
                    hip = fields[1].toIntOrNull() ?: 0,
                    properName = fields[2].ifBlank { null },
                    bayer = fields[3].ifBlank { null },
                    flamsteed = fields[4].ifBlank { null },
                    constellation = fields[5],
                    distanceParsecs = fields[6].toDoubleOrNull() ?: 0.0,
                    spectralType = fields[7],
                )
            }
            return StarField(count, positions, magnitudes, buckets, meta)
        }

        fun readConstellations(bytes: ByteArray): List<ConstellationFigure> {
            val data = buffer(bytes)
            val count = data.int
            return List(count) {
                val codeBytes = ByteArray(4)
                data.get(codeBytes)
                val code = String(codeBytes, Charsets.US_ASCII).trim()
                val ra = data.float.toDouble()
                val dec = data.float.toDouble()
                val rank = data.int
                val segmentCount = data.int
                val segments = FloatArray(segmentCount * 6)
                for (segment in 0 until segmentCount) {
                    val first = unit(data.float.toDouble(), data.float.toDouble())
                    val second = unit(data.float.toDouble(), data.float.toDouble())
                    segments[segment * 6] = first[0]
                    segments[segment * 6 + 1] = first[1]
                    segments[segment * 6 + 2] = first[2]
                    segments[segment * 6 + 3] = second[0]
                    segments[segment * 6 + 4] = second[1]
                    segments[segment * 6 + 5] = second[2]
                }
                ConstellationFigure(code, Vec3.fromEquatorial(ra / 15.0, dec), rank, segments)
            }
        }

        fun readLand(bytes: ByteArray): List<FloatArray> {
            val data = buffer(bytes)
            val count = data.int
            return List(count) {
                val points = data.int
                FloatArray(points * 2) { data.float }
            }
        }

        private fun unit(raDegrees: Double, decDegrees: Double): FloatArray {
            val ra = Math.toRadians(raDegrees)
            val dec = Math.toRadians(decDegrees)
            return floatArrayOf((cos(dec) * cos(ra)).toFloat(), (cos(dec) * sin(ra)).toFloat(), sin(dec).toFloat())
        }
    }
}
