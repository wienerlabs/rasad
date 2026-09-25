package xyz.wienerlabs.rasad.sky

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import xyz.wienerlabs.rasad.astro.GeoPoint
import kotlin.math.cos
import kotlin.math.sin

class OrientationTracker(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val rotationVector = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val lock = Any()
    private val latest = FloatArray(9)
    private val scratch = FloatArray(9)
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    @Volatile
    var hasReading = false
        private set

    @Volatile
    var declinationDegrees = 0.0
        private set

    val isAvailable: Boolean
        get() = rotationVector != null || (accelerometer != null && magnetometer != null)

    fun updateDeclination(location: GeoPoint, millis: Long) {
        declinationDegrees = GeomagneticField(
            location.latitude.toFloat(),
            location.longitude.toFloat(),
            location.heightMeters.toFloat(),
            millis,
        ).declination.toDouble()
    }

    fun start() {
        val manager = sensorManager ?: return
        if (rotationVector != null) {
            manager.registerListener(this, rotationVector, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelerometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
            magnetometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME) }
        }
    }

    fun stop() {
        sensorManager?.unregisterListener(this)
        hasReading = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(scratch, event.values)
                publish()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                lowPass(event.values, gravity, hasGravity)
                hasGravity = true
                fuse()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                lowPass(event.values, geomagnetic, hasGeomagnetic)
                hasGeomagnetic = true
                fuse()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun fuse() {
        if (!hasGravity || !hasGeomagnetic) return
        if (SensorManager.getRotationMatrix(scratch, null, gravity, geomagnetic)) publish()
    }

    private fun lowPass(input: FloatArray, output: FloatArray, primed: Boolean) {
        val alpha = if (primed) 0.18f else 1f
        for (i in 0..2) output[i] += alpha * (input[i] - output[i])
    }

    private fun publish() {
        synchronized(lock) { scratch.copyInto(latest) }
        hasReading = true
    }

    fun readBasis(forward: DoubleArray, up: DoubleArray): Boolean {
        if (!hasReading) return false
        val m = FloatArray(9)
        synchronized(lock) { latest.copyInto(m) }
        val declination = Math.toRadians(declinationDegrees)
        val c = cos(declination)
        val s = sin(declination)
        val fx = -m[2].toDouble(); val fy = -m[5].toDouble(); val fz = -m[8].toDouble()
        val ux = m[1].toDouble(); val uy = m[4].toDouble(); val uz = m[7].toDouble()
        forward[0] = fx * c + fy * s
        forward[1] = fy * c - fx * s
        forward[2] = fz
        up[0] = ux * c + uy * s
        up[1] = uy * c - ux * s
        up[2] = uz
        return true
    }
}
