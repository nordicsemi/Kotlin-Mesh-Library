package no.nordicsemi.android.nrfmesh.feature.model.generic

import android.location.LocationManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests how the location of the phone is turned into values of the Generic Location state. */
class PhoneLocationTest {

    // ---------------------------------------------------------------- Altitude

    @Test
    fun testAltitudeIsRoundedToWholeMeters() {
        assertEquals(23.toShort(), altitudeMetersOf(hasAltitude = true, altitude = 23.4))
        assertEquals(24.toShort(), altitudeMetersOf(hasAltitude = true, altitude = 23.6))
        assertEquals(0.toShort(), altitudeMetersOf(hasAltitude = true, altitude = 0.0))
    }

    @Test
    fun testNegativeAltitudeIsKept() {
        // For example, the shore of the Dead Sea is about 430 m below sea level.
        assertEquals((-430).toShort(), altitudeMetersOf(hasAltitude = true, altitude = -430.2))
        assertEquals((-1).toShort(), altitudeMetersOf(hasAltitude = true, altitude = -0.9))
    }

    @Test
    fun testAltitudeLimits() {
        assertEquals(32765.toShort(), altitudeMetersOf(hasAltitude = true, altitude = 32765.0))
        assertEquals(Short.MIN_VALUE, altitudeMetersOf(hasAltitude = true, altitude = -32768.0))
    }

    @Test
    fun testAltitudeThatCannotBeRepresentedIsDropped() {
        // 32766 and 32767 are reserved for "too large" and "not configured".
        assertNull(altitudeMetersOf(hasAltitude = true, altitude = 32766.0))
        assertNull(altitudeMetersOf(hasAltitude = true, altitude = 32767.0))
        assertNull(altitudeMetersOf(hasAltitude = true, altitude = 100_000.0))
        assertNull(altitudeMetersOf(hasAltitude = true, altitude = -32769.0))
        assertNull(altitudeMetersOf(hasAltitude = true, altitude = -100_000.0))
    }

    @Test
    fun testMissingAltitudeIsDropped() {
        assertNull(altitudeMetersOf(hasAltitude = false, altitude = 23.0))
        assertNull(altitudeMetersOf(hasAltitude = true, altitude = Double.NaN))
        assertNull(altitudeMetersOf(hasAltitude = true, altitude = Double.POSITIVE_INFINITY))
        assertNull(altitudeMetersOf(hasAltitude = true, altitude = Double.NEGATIVE_INFINITY))
    }

    // ---------------------------------------------------------------- Provider

    @Test
    fun testPreciseLocationUsesGps() {
        assertEquals(LocationManager.GPS_PROVIDER, providerFor(hasFine = true, hasCoarse = true))
        assertEquals(LocationManager.GPS_PROVIDER, providerFor(hasFine = true, hasCoarse = false))
    }

    @Test
    fun testApproximateLocationUsesTheNetwork() {
        assertEquals(LocationManager.NETWORK_PROVIDER, providerFor(hasFine = false, hasCoarse = true))
    }

    @Test
    fun testNoPermissionHasNoProvider() {
        assertNull(providerFor(hasFine = false, hasCoarse = false))
    }
}
