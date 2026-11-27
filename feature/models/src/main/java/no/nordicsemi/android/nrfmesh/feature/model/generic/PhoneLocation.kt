package no.nordicsemi.android.nrfmesh.feature.model.generic

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.math.roundToInt

/** Lowest altitude in meters that a Generic Location global altitude can hold. */
private const val MIN_ALTITUDE_METERS = Short.MIN_VALUE.toInt()

/** Highest altitude in meters that a Generic Location global altitude can hold. */
private const val MAX_ALTITUDE_METERS = 32765

/** How often the phone is asked for a new location while following it. */
private const val LIVE_UPDATE_INTERVAL_MILLIS = 500L

/** How far the phone has to move before a new location is reported while following it. */
private const val LIVE_UPDATE_MIN_DISTANCE_METERS = 0f

/** How long a GPS fix is preferred over the network position. */
private const val GPS_PREFERRED_MILLIS = 10_000L

/** The location of the phone, in the units used by the Generic Location Global state. */
internal data class PhoneLocation(
    val latitude: Double,
    val longitude: Double,
    /** Altitude in meters, or null when the phone did not report a usable one. */
    val altitudeMeters: Short?,
    /** True when only the approximate location permission was granted. */
    val isApproximate: Boolean,
)

/** The outcome of reading the location of the phone. */
internal sealed interface PhoneLocationResult {
    data class Found(val location: PhoneLocation) : PhoneLocationResult

    /** Neither the precise nor the approximate location permission is granted. */
    data object PermissionDenied : PhoneLocationResult

    /** Location is turned off on the phone. */
    data object ProviderDisabled : PhoneLocationResult

    /** The phone did not report a location in time. */
    data object Unavailable : PhoneLocationResult
}

/**
 * Converts an altitude reported by the phone to whole meters that fit a Generic Location global
 * altitude, or null if there is none. The reserved values, 32766 and 32767, are not used.
 */
internal fun altitudeMetersOf(hasAltitude: Boolean, altitude: Double): Short? = when {
    !hasAltitude || altitude.isNaN() || altitude.isInfinite() -> null
    else -> altitude.roundToInt().takeIf { it in MIN_ALTITUDE_METERS..MAX_ALTITUDE_METERS }
        ?.toShort()
}

/**
 * Selects the location provider for the permissions that are granted: GPS if the precise location
 * is allowed, and the network if only the approximate location is allowed.
 */
internal fun providerFor(hasFine: Boolean, hasCoarse: Boolean): String? = when {
    hasFine -> LocationManager.GPS_PROVIDER
    hasCoarse -> LocationManager.NETWORK_PROVIDER
    else -> null
}

private fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** Returns true if the precise or the approximate location permission is granted. */
internal fun Context.hasLocationPermission() =
    isGranted(Manifest.permission.ACCESS_FINE_LOCATION) ||
            isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)

/**
 * Emits the location of the phone every time it changes, until the collector is cancelled.
 *
 * With the precise location permission, GPS and the network are used together: the network
 * position arrives within seconds, also indoors, and is replaced by GPS as soon as it has a fix.
 * Otherwise only the network is used. If the location cannot be tracked, a single failure result is
 * emitted and the flow completes.
 */
@SuppressLint("MissingPermission")
internal fun Context.phoneLocationUpdates(): Flow<PhoneLocationResult> = callbackFlow {
    val hasFine = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
    val hasCoarse = isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
    val manager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (manager == null) {
        trySend(PhoneLocationResult.Unavailable)
        close()
        return@callbackFlow
    }
    if (providerFor(hasFine, hasCoarse) == null) {
        trySend(PhoneLocationResult.PermissionDenied)
        close()
        return@callbackFlow
    }
    val providers = buildList {
        if (hasFine) add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
    }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    if (!LocationManagerCompat.isLocationEnabled(manager) || providers.isEmpty()) {
        trySend(PhoneLocationResult.ProviderDisabled)
        close()
        return@callbackFlow
    }

    var lastGpsFixMillis = 0L
    val listeners = providers.map { provider ->
        object : LocationListenerCompat {
            override fun onLocationChanged(location: Location) {
                val now = SystemClock.elapsedRealtime()
                if (provider == LocationManager.GPS_PROVIDER) {
                    lastGpsFixMillis = now
                } else if (now - lastGpsFixMillis < GPS_PREFERRED_MILLIS) {
                    // A recent GPS fix is more accurate than the network position.
                    return
                }
                trySend(PhoneLocationResult.Found(location.toPhoneLocation(isApproximate = !hasFine)))
            }

            override fun onProviderDisabled(provider: String) {
                if (providers.size == 1) {
                    trySend(PhoneLocationResult.ProviderDisabled)
                    close()
                }
            }
        }
    }
    val request = LocationRequestCompat.Builder(LIVE_UPDATE_INTERVAL_MILLIS)
        .setMinUpdateDistanceMeters(LIVE_UPDATE_MIN_DISTANCE_METERS)
        .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
        .build()
    val executor = ContextCompat.getMainExecutor(this@phoneLocationUpdates)
    providers.zip(listeners).forEach { (provider, listener) ->
        LocationManagerCompat.requestLocationUpdates(manager, provider, request, executor, listener)
    }
    awaitClose { listeners.forEach { LocationManagerCompat.removeUpdates(manager, it) } }
}

private fun Location.toPhoneLocation(isApproximate: Boolean) = PhoneLocation(
    latitude = latitude,
    longitude = longitude,
    altitudeMeters = altitudeMetersOf(hasAltitude(), altitude),
    isApproximate = isApproximate
)
