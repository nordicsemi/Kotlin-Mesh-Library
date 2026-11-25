package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalStatus
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests the supported ranges and the reserved values of the Generic Location values. */
class LocationValueRangesTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    // ---------------------------------------------------------------- Latitude and longitude

    @Test
    fun testLatitudeLimits() {
        assertEquals(Int.MAX_VALUE, assertNotNull(Latitude.fromDegrees(90.0)).raw)
        // -90 degrees is Int.MIN_VALUE + 1, as Int.MIN_VALUE means not configured.
        assertEquals(Int.MIN_VALUE + 1, assertNotNull(Latitude.fromDegrees(-90.0)).raw)
        assertEquals(0, assertNotNull(Latitude.fromDegrees(0.0)).raw)
        assertEquals(90.0, Latitude.fromDegrees(90.0)?.degrees)
        assertEquals(-90.0, Latitude.fromDegrees(-90.0)?.degrees)
    }

    @Test
    fun testLatitudeOutOfRangeIsRejected() {
        listOf(90.0001, -90.0001, 180.0, -180.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach { assertNull(Latitude.fromDegrees(it), "$it was accepted") }
    }

    @Test
    fun testLongitudeLimits() {
        assertEquals(Int.MAX_VALUE, assertNotNull(Longitude.fromDegrees(180.0)).raw)
        assertEquals(Int.MIN_VALUE + 1, assertNotNull(Longitude.fromDegrees(-180.0)).raw)
        assertEquals(0, assertNotNull(Longitude.fromDegrees(0.0)).raw)
        assertEquals(180.0, Longitude.fromDegrees(180.0)?.degrees)
        assertEquals(-180.0, Longitude.fromDegrees(-180.0)?.degrees)
    }

    @Test
    fun testLongitudeOutOfRangeIsRejected() {
        listOf(180.0001, -180.0001, 360.0, -360.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
            .forEach { assertNull(Longitude.fromDegrees(it), "$it was accepted") }
    }

    @Test
    fun testPositiveAndNegativeCoordinatesSurviveARoundTrip() {
        listOf(59.9139, 0.000001, -33.8688, -0.000001, 89.999999, -89.999999).forEach {
            val latitude = assertNotNull(Latitude.fromDegrees(it))
            assertEquals(it, assertNotNull(latitude.degrees), absoluteTolerance = 1e-6)
        }
        listOf(10.7522, 179.999999, -122.4194, -179.999999).forEach {
            val longitude = assertNotNull(Longitude.fromDegrees(it))
            assertEquals(it, assertNotNull(longitude.degrees), absoluteTolerance = 1e-6)
        }
    }

    @Test
    fun testNotConfiguredCoordinates() {
        assertEquals(0x80000000.toInt(), Latitude.NOT_CONFIGURED)
        assertFalse(Latitude.notConfigured.isConfigured)
        assertNull(Latitude.notConfigured.degrees)
        assertFalse(Longitude.notConfigured.isConfigured)
        assertNull(Longitude.notConfigured.degrees)
        // The value next to the reserved one is still a coordinate.
        assertTrue(Latitude(Int.MIN_VALUE + 1).isConfigured)
        assertTrue(Longitude(Int.MIN_VALUE + 1).isConfigured)
    }

    // ---------------------------------------------------------------- Altitude

    @Test
    fun testAltitudeRange() {
        assertEquals(32765, Altitude(32765).meters?.toInt())
        assertEquals(-32768, Altitude(Short.MIN_VALUE).meters?.toInt())
        assertEquals(0, Altitude(0).meters?.toInt())
        assertEquals(-1, Altitude(-1).meters?.toInt())
        assertTrue(Altitude(32765).isConfigured)
        assertFalse(Altitude(32765).isTooLarge)
    }

    @Test
    fun testReservedAltitudes() {
        assertEquals(0x7FFF.toShort(), Altitude.NOT_CONFIGURED)
        assertEquals(0x7FFE.toShort(), Altitude.TOO_LARGE)

        assertFalse(Altitude.notConfigured.isConfigured)
        assertFalse(Altitude.notConfigured.isTooLarge)
        assertNull(Altitude.notConfigured.meters)

        assertFalse(Altitude.tooLarge.isConfigured)
        assertTrue(Altitude.tooLarge.isTooLarge)
        assertNull(Altitude.tooLarge.meters)
    }

    // ---------------------------------------------------------------- Global location messages

    @Test
    fun testGlobalStatusWithTheExtremeValues() {
        val data = bytes(
            0xFF, 0xFF, 0xFF, 0x7F, // latitude Int.MAX_VALUE
            0x01, 0x00, 0x00, 0x80, // longitude Int.MIN_VALUE + 1
            0xFD, 0x7F // altitude 32765
        )
        val status = assertNotNull(GenericLocationGlobalStatus.init(data))
        assertEquals(90.0, status.latitude.degrees)
        assertEquals(-180.0, status.longitude.degrees)
        assertEquals(32765, status.altitude.meters?.toInt())
        assertContentEquals(data, status.parameters)
    }

    @Test
    fun testGlobalStatusWithNothingConfigured() {
        val data = bytes(0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00, 0x80, 0xFF, 0x7F)
        val status = assertNotNull(GenericLocationGlobalStatus.init(data))
        assertFalse(status.latitude.isConfigured)
        assertFalse(status.longitude.isConfigured)
        assertFalse(status.altitude.isConfigured)
        assertContentEquals(data, status.parameters)
    }

    @Test
    fun testGlobalStatusWithTooLargeAltitude() {
        val data = bytes(0, 0, 0, 0, 0, 0, 0, 0, 0xFE, 0x7F)
        val status = assertNotNull(GenericLocationGlobalStatus.init(data))
        assertTrue(status.altitude.isTooLarge)
        assertContentEquals(data, status.parameters)
    }

    // ---------------------------------------------------------------- Local location

    @Test
    fun testLocalStatusWithTheExtremeValues() {
        val status = GenericLocationLocalStatus(
            north = Short.MAX_VALUE,
            east = (Short.MIN_VALUE + 1).toShort(),
            altitude = Short.MIN_VALUE,
            floor = FloorNumber.of(FloorNumber.MAX)!!,
            uncertainty = LocationUncertainty(0xFFFFu)
        )
        val decoded = assertNotNull(GenericLocationLocalStatus.init(status.parameters))
        assertEquals(Short.MAX_VALUE, decoded.north)
        assertEquals((Short.MIN_VALUE + 1).toShort(), decoded.east)
        assertEquals(Short.MIN_VALUE, decoded.altitude)
        assertEquals(FloorNumber.MAX, decoded.floor.number)
        assertEquals(LocationUncertainty(0xFFFFu), decoded.uncertainty)
    }

    @Test
    fun testLocalStatusWithNothingConfigured() {
        val data = bytes(0x00, 0x80, 0x00, 0x80, 0xFF, 0x7F, 0xFF, 0x00, 0xFF)
        val status = assertNotNull(GenericLocationLocalStatus.init(data))
        assertEquals(LocalLocationStatusMessage.LOCAL_NOT_CONFIGURED, status.north)
        assertEquals(LocalLocationStatusMessage.LOCAL_NOT_CONFIGURED, status.east)
        assertEquals(Altitude.NOT_CONFIGURED, status.altitude)
        assertFalse(status.floor.isKnown)
        assertContentEquals(data, status.parameters)
    }

    @Test
    fun testLocalNotConfiguredValue() {
        assertEquals(0x8000.toShort(), LocalLocationStatusMessage.LOCAL_NOT_CONFIGURED)
    }

    @Test
    fun testNegativeLocalCoordinates() {
        val status = GenericLocationLocalStatus(-1, -32767, -1, FloorNumber.of(-1)!!, LocationUncertainty(0u))
        val decoded = assertNotNull(GenericLocationLocalStatus.init(status.parameters))
        assertEquals(-1, decoded.north)
        assertEquals(-32767, decoded.east)
        assertEquals(-1, decoded.altitude)
        assertEquals(-1, decoded.floor.number)
    }

    // ---------------------------------------------------------------- Floor number

    @Test
    fun testEveryNumberedFloor() {
        for (raw in 0x00..0xFB) {
            val floor = FloorNumber(raw.toUByte())
            assertEquals(raw - 20, floor.number, "raw $raw")
            assertTrue(floor.isKnown, "raw $raw")
            assertFalse(floor.isGroundFloor, "raw $raw")
        }
    }

    @Test
    fun testFloorLimits() {
        // 0x00 is floor -20 and any floor below, 0xFC is floor 232 and any floor above.
        assertEquals(-20, FloorNumber(0x00u).number)
        assertTrue(FloorNumber(0x00u).isMinOrBelow)
        assertEquals(-19, FloorNumber(0x01u).number)
        assertFalse(FloorNumber(0x01u).isMinOrBelow)
        assertEquals(231, FloorNumber(0xFBu).number)
        assertFalse(FloorNumber(0xFBu).isMaxOrAbove)
        assertEquals(232, FloorNumber(0xFCu).number)
        assertTrue(FloorNumber(0xFCu).isMaxOrAbove)
        assertFalse(FloorNumber(0xFCu).isGroundFloor)
        assertEquals(FloorNumber(0x00u), FloorNumber.of(FloorNumber.MIN))
        assertEquals(FloorNumber(0xFBu), FloorNumber.of(231))
        assertEquals(FloorNumber(0xFCu), FloorNumber.of(FloorNumber.MAX))
        assertNull(FloorNumber.of(FloorNumber.MIN - 1))
        assertNull(FloorNumber.of(FloorNumber.MAX + 1))
        assertNull(FloorNumber.of(Int.MIN_VALUE))
        assertNull(FloorNumber.of(Int.MAX_VALUE))
    }

    @Test
    fun testGroundFloorsAndNotConfigured() {
        assertEquals(0, FloorNumber(0xFDu).number)
        assertEquals(1, FloorNumber(0xFEu).number)
        assertTrue(FloorNumber(0xFDu).isGroundFloor)
        assertTrue(FloorNumber(0xFEu).isGroundFloor)
        assertEquals(FloorNumber(0xFDu), FloorNumber.GROUND_FLOOR_0)
        assertEquals(FloorNumber(0xFEu), FloorNumber.GROUND_FLOOR_1)
        assertNull(FloorNumber(0xFFu).number)
        assertFalse(FloorNumber(0xFFu).isKnown)
        assertFalse(FloorNumber(0xFFu).isGroundFloor)
    }

    // ---------------------------------------------------------------- Uncertainty

    @Test
    fun testEveryUpdateTimeAndPrecision() {
        for (value in 0..15) {
            val uncertainty = LocationUncertainty.of(isMobile = false, updateTimeRaw = value, precisionRaw = value)
            assertEquals(125L shl value, uncertainty.updateTimeMillis, "update time $value")
            assertEquals(125L shl value, uncertainty.precisionMillimeters, "precision $value")
        }
    }

    @Test
    fun testLargestUpdateTimeAndPrecision() {
        // 4096 s and 4096 m are valid values, not "unknown".
        val uncertainty = LocationUncertainty.of(isMobile = true, updateTimeRaw = 0x0F, precisionRaw = 0x0F)
        assertEquals(4_096_000L, uncertainty.updateTimeMillis)
        assertEquals(4_096_000L, uncertainty.precisionMillimeters)
        // Only the lowest four bits are used.
        val masked = LocationUncertainty.of(isMobile = false, updateTimeRaw = 0x1F, precisionRaw = 0x2F)
        assertEquals(0x0F, masked.updateTimeRaw)
        assertEquals(0x0F, masked.precisionRaw)
    }

    @Test
    fun testMobileIsOnlyTheLowestBit() {
        assertTrue(LocationUncertainty(0x0001u).isMobile)
        assertFalse(LocationUncertainty(0x0000u).isMobile)
        // The bits 1 to 7 are reserved for future use and are ignored.
        assertFalse(LocationUncertainty(0x00FEu).isMobile)
        assertEquals(0, LocationUncertainty(0x00FEu).updateTimeRaw)
        assertEquals(0, LocationUncertainty(0x00FEu).precisionRaw)
    }

    @Test
    fun testUpdateTimeAndPrecisionDoNotOverlap() {
        val uncertainty = LocationUncertainty(0x3A01u)
        assertTrue(uncertainty.isMobile)
        assertEquals(0x0A, uncertainty.updateTimeRaw)
        assertEquals(0x03, uncertainty.precisionRaw)
    }
}
