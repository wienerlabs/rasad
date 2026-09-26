package xyz.wienerlabs.rasad.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import xyz.wienerlabs.rasad.astro.GeoPoint
import xyz.wienerlabs.rasad.astro.TurkishLocale
import kotlin.coroutines.resume

private const val GEOCODE_GRID_DEGREES = 0.02

class LocationSource(private val context: Context) {
    private val manager = context.getSystemService(LocationManager::class.java)
    private val preferences = context.getSharedPreferences("rasad", Context.MODE_PRIVATE)

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun saved(): GeoPoint? {
        if (!preferences.contains("lat")) return null
        return GeoPoint(
            preferences.getFloat("lat", 0f).toDouble(),
            preferences.getFloat("lon", 0f).toDouble(),
            preferences.getFloat("alt", 0f).toDouble(),
        )
    }

    fun savedPlaceName(): String? = preferences.getString("place", null)

    fun save(point: GeoPoint, placeName: String?) {
        preferences.edit()
            .putFloat("lat", point.latitude.toFloat())
            .putFloat("lon", point.longitude.toFloat())
            .putFloat("alt", point.heightMeters.toFloat())
            .putString("place", placeName)
            .apply()
    }

    @SuppressLint("MissingPermission")
    suspend fun current(): GeoPoint? {
        val locationManager = manager ?: return null
        if (!hasPermission()) return null
        val providers = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { locationManager.isProviderEnabled(it) }.getOrDefault(false) }
        val fresh = withTimeoutOrNull(8_000L) {
            channelFlow {
                providers.forEach { provider ->
                    launch { currentFrom(locationManager, provider)?.let { send(it) } }
                }
            }.firstOrNull()
        }
        if (fresh != null) return fresh.toGeoPoint()
        return providers
            .mapNotNull { runCatching { locationManager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
            ?.toGeoPoint()
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentFrom(locationManager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = android.os.CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            runCatching {
                locationManager.getCurrentLocation(provider, signal, context.mainExecutor) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            }.onFailure { if (continuation.isActive) continuation.resume(null) }
        }

    suspend fun placeName(point: GeoPoint): String? {
        if (!Geocoder.isPresent()) return null
        return withTimeoutOrNull(5_000L) {
            suspendCancellableCoroutine { continuation ->
                runCatching {
                    Geocoder(context, TurkishLocale).getFromLocation(coarse(point.latitude), coarse(point.longitude), 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<android.location.Address>) {
                            val address = addresses.firstOrNull()
                            val name = address?.locality ?: address?.subAdminArea ?: address?.adminArea
                            if (continuation.isActive) continuation.resume(name)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    })
                }.onFailure { if (continuation.isActive) continuation.resume(null) }
            }
        }
    }

    private fun coarse(degrees: Double): Double = Math.round(degrees / GEOCODE_GRID_DEGREES) * GEOCODE_GRID_DEGREES

    private fun Location.toGeoPoint() = GeoPoint(latitude, longitude, if (hasAltitude()) altitude else 0.0)
}
