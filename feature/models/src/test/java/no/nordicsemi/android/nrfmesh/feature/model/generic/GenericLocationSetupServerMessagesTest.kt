package no.nordicsemi.android.nrfmesh.feature.model.generic

import no.nordicsemi.kotlin.mesh.core.messages.Altitude
import no.nordicsemi.kotlin.mesh.core.messages.FloorNumber
import no.nordicsemi.kotlin.mesh.core.messages.Latitude
import no.nordicsemi.kotlin.mesh.core.messages.Longitude
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSetUnacknowledged
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests the messages sent by the Generic Location Setup Server controls. */
class GenericLocationSetupServerMessagesTest {

    private val latitude = assertNotNull(Latitude.fromDegrees(59.9139))
    private val longitude = assertNotNull(Longitude.fromDegrees(10.7522))
    private val altitude = Altitude(23)

    // ---------------------------------------------------------------- Global

    @Test
    fun testAcknowledgedGlobalSet() {
        val message = globalLocationSetOf(latitude, longitude, altitude, acknowledged = true)
        assertIs<GenericLocationGlobalSet>(message)
        assertEquals(latitude, message.latitude)
        assertEquals(longitude, message.longitude)
        assertEquals(altitude, message.altitude)
    }

    @Test
    fun testUnacknowledgedGlobalSet() {
        val message = globalLocationSetOf(latitude, longitude, altitude, acknowledged = false)
        assertIs<GenericLocationGlobalSetUnacknowledged>(message)
        assertEquals(latitude, message.latitude)
        assertEquals(longitude, message.longitude)
        assertEquals(altitude, message.altitude)
    }

    // ---------------------------------------------------------------- Local

    @Test
    fun testAcknowledgedLocalSet() {
        val message = localLocationSetOf(
            north = 1250,
            east = -340,
            altitude = 45,
            floor = FloorNumber.GROUND_FLOOR_0,
            isMobile = false,
            acknowledged = true
        )
        assertIs<GenericLocationLocalSet>(message)
        assertEquals(1250, message.north)
        assertEquals(-340, message.east)
        assertEquals(45, message.altitude)
        assertEquals(FloorNumber.GROUND_FLOOR_0, message.floor)
    }

    @Test
    fun testUnacknowledgedLocalSet() {
        val message = localLocationSetOf(
            north = 1,
            east = 2,
            altitude = 3,
            floor = FloorNumber.GROUND_FLOOR_1,
            isMobile = true,
            acknowledged = false
        )
        assertIs<GenericLocationLocalSetUnacknowledged>(message)
        assertEquals(1, message.north)
        assertEquals(2, message.east)
        assertEquals(3, message.altitude)
        assertEquals(FloorNumber.GROUND_FLOOR_1, message.floor)
    }

    @Test
    fun testMobileIsSentAndUpdateTimeAndPrecisionAreUnknown() {
        val mobile = localLocationSetOf(0, 0, 0, FloorNumber.UNKNOWN, isMobile = true, acknowledged = true)
        val stationary = localLocationSetOf(0, 0, 0, FloorNumber.UNKNOWN, isMobile = false, acknowledged = true)
        assertIs<GenericLocationLocalSet>(mobile)
        assertIs<GenericLocationLocalSet>(stationary)

        assertTrue(mobile.uncertainty.isMobile)
        assertFalse(stationary.uncertainty.isMobile)
        listOf(mobile, stationary).forEach {
            // Without a chosen update time and precision, the coarsest values are used.
            assertEquals(4_096_000L, it.uncertainty.updateTimeMillis)
            assertEquals(4_096_000L, it.uncertainty.precisionMillimeters)
        }
    }

    // ---------------------------------------------------------------- Floors

    @Test
    fun testFloorsStartWithTheGroundFloorsAndUnknown() {
        assertEquals(
            listOf(FloorNumber.GROUND_FLOOR_0, FloorNumber.GROUND_FLOOR_1, FloorNumber.UNKNOWN),
            floors.take(3)
        )
    }

    @Test
    fun testFloorsIncludeEveryNumberedFloor() {
        val numbered = floors.drop(3)
        assertEquals(FloorNumber.MAX - FloorNumber.MIN + 1, numbered.size)
        assertEquals(FloorNumber.MIN, numbered.first().number)
        assertEquals(FloorNumber.MAX, numbered.last().number)
    }

    @Test
    fun testFloorsAreUnique() {
        assertEquals(floors.size, floors.toSet().size)
    }

    @Test
    fun testThousandthsAreShownWithoutTrailingZeros() {
        assertEquals("0.125", 125L.thousandthsToString())
        assertEquals("1", 1_000L.thousandthsToString())
        assertEquals("2.5", 2_500L.thousandthsToString())
        assertEquals("4096", 4_096_000L.thousandthsToString())
    }
}
