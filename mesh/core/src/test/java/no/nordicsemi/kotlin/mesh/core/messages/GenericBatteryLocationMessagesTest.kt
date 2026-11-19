package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.generic.BatteryChargingState
import no.nordicsemi.kotlin.mesh.core.messages.generic.BatteryIndicator
import no.nordicsemi.kotlin.mesh.core.messages.generic.BatteryPresence
import no.nordicsemi.kotlin.mesh.core.messages.generic.BatteryServiceability
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericBatteryGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericBatteryStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalStatus
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenericBatteryLocationMessagesTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun testOpCodes() {
        assertEquals(0x8223u, GenericBatteryGet.opCode)
        assertEquals(0x8224u, GenericBatteryStatus.opCode)
        assertEquals(0x8225u, GenericLocationGlobalGet.opCode)
        assertEquals(0x40u, GenericLocationGlobalStatus.opCode)
        assertEquals(0x41u, GenericLocationGlobalSet.opCode)
        assertEquals(0x42u, GenericLocationGlobalSetUnacknowledged.opCode)
    }

    @Test
    fun testResponseOpCodes() {
        assertEquals(GenericBatteryStatus.opCode, GenericBatteryGet().responseOpCode)
        assertEquals(GenericLocationGlobalStatus.opCode, GenericLocationGlobalGet().responseOpCode)
        val set = GenericLocationGlobalSet(
            Latitude.notConfigured, Longitude.notConfigured, Altitude.notConfigured
        )
        assertEquals(GenericLocationGlobalStatus.opCode, set.responseOpCode)
    }

    @Test
    fun testGetsHaveNoParameters() {
        assertNull(GenericBatteryGet().parameters)
        assertNull(GenericLocationGlobalGet().parameters)
    }

    @Test
    fun testGetsDecoding() {
        assertNotNull(GenericBatteryGet.init(ByteArray(0)))
        assertNull(GenericBatteryGet.init(bytes(0)))
        assertNull(GenericBatteryGet.init(null))
        assertNotNull(GenericLocationGlobalGet.init(ByteArray(0)))
        assertNull(GenericLocationGlobalGet.init(bytes(0)))
        assertNull(GenericLocationGlobalGet.init(null))
    }

    // Battery

    @Test
    fun testBatteryStatusEncoding() {
        val status = GenericBatteryStatus(
            batteryLevel = 75u,
            timeToDischarge = 0x010203u,
            timeToCharge = 0x040506u,
            flags = 0x9Au
        )
        assertContentEquals(bytes(75, 3, 2, 1, 6, 5, 4, 0x9A), status.parameters)
    }

    @Test
    fun testBatteryStatusDecoding() {
        val status = GenericBatteryStatus.init(bytes(75, 3, 2, 1, 6, 5, 4, 0b10_01_10_01))
        assertNotNull(status)
        assertEquals(75u.toUByte(), status.batteryLevel)
        assertEquals(0x010203u, status.timeToDischarge)
        assertEquals(0x040506u, status.timeToCharge)
        assertEquals(BatteryPresence.REMOVABLE, status.batteryPresence)
        assertEquals(BatteryIndicator.GOOD, status.batteryIndicator)
        assertEquals(BatteryChargingState.NOT_CHARGING, status.batteryChargingState)
        assertEquals(BatteryServiceability.SERVICE_REQUIRED, status.batteryServiceability)
        assertTrue(status.isBatteryLevelKnown)
        assertTrue(status.isTimeToDischargeKnown)
        assertTrue(status.isTimeToChargeKnown)
    }

    @Test
    fun testBatteryStatusUnknownValues() {
        val status = GenericBatteryStatus.init(bytes(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF))
        assertNotNull(status)
        assertFalse(status.isBatteryLevelKnown)
        assertFalse(status.isTimeToDischargeKnown)
        assertFalse(status.isTimeToChargeKnown)
        assertEquals(0xFFFFFFu, status.timeToDischarge)
        assertEquals(BatteryPresence.UNKNOWN, status.batteryPresence)
        assertEquals(BatteryIndicator.UNKNOWN, status.batteryIndicator)
        assertEquals(BatteryChargingState.UNKNOWN, status.batteryChargingState)
        assertEquals(BatteryServiceability.UNKNOWN, status.batteryServiceability)
    }

    @Test
    fun testBatteryStatusInvalidLengthOrNull() {
        assertNull(GenericBatteryStatus.init(ByteArray(7)))
        assertNull(GenericBatteryStatus.init(ByteArray(9)))
        assertNull(GenericBatteryStatus.init(null))
    }

    @Test
    fun testBatteryStatusFlagsConstructorAndClamping() {
        val status = GenericBatteryStatus(
            batteryLevel = 200u,
            timeToDischarge = 0xFFFFFFu,
            timeToCharge = 0x1000000u,
            presence = BatteryPresence.NOT_REMOVABLE,
            indicator = BatteryIndicator.LOW,
            chargingState = BatteryChargingState.CHARGING,
            serviceability = BatteryServiceability.SERVICE_NOT_REQUIRED
        )
        assertEquals(100u.toUByte(), status.batteryLevel)
        assertEquals(0xFFFFFFu, status.timeToDischarge)
        assertEquals(0xFFFFFEu, status.timeToCharge)
        assertEquals(0b01_10_01_10, status.flags.toInt())
        assertEquals(BatteryPresence.NOT_REMOVABLE, status.batteryPresence)
        assertEquals(BatteryIndicator.LOW, status.batteryIndicator)
        assertEquals(BatteryChargingState.CHARGING, status.batteryChargingState)
        assertEquals(BatteryServiceability.SERVICE_NOT_REQUIRED, status.batteryServiceability)
        // Unknown level is preserved.
        val unknown = GenericBatteryStatus(
            0xFFu, 0u, 0u, BatteryPresence.UNKNOWN, BatteryIndicator.UNKNOWN,
            BatteryChargingState.UNKNOWN, BatteryServiceability.UNKNOWN
        )
        assertEquals(0xFFu.toUByte(), unknown.batteryLevel)
        assertEquals(0xFFu.toUByte(), unknown.flags)
    }

    // Location

    private val locationBytes = bytes(
        0x04, 0x03, 0x02, 0x01, // latitude 0x01020304
        0xFC, 0xFF, 0xFF, 0xFF, // longitude -4
        0x38, 0xFF              // altitude -200
    )

    @Test
    fun testLocationDecodingLittleEndianAndNegativeValues() {
        val status = GenericLocationGlobalStatus.init(locationBytes)
        assertNotNull(status)
        assertEquals(0x01020304, status.latitude.raw)
        assertEquals(-4, status.longitude.raw)
        assertEquals((-200).toShort(), status.altitude.raw)
        assertEquals((-200).toShort(), status.altitude.meters)
        assertTrue(status.latitude.isConfigured)
    }

    @Test
    fun testLocationEncoding() {
        val lat = Latitude(0x01020304)
        val lon = Longitude(-4)
        val alt = Altitude((-200).toShort())
        assertContentEquals(locationBytes, GenericLocationGlobalSet(lat, lon, alt).parameters)
        assertContentEquals(
            locationBytes, GenericLocationGlobalSetUnacknowledged(lat, lon, alt).parameters
        )
        assertContentEquals(locationBytes, GenericLocationGlobalStatus(lat, lon, alt).parameters)
    }

    @Test
    fun testLocationDecodingOfSetMessages() {
        val set = GenericLocationGlobalSet.init(locationBytes)
        assertNotNull(set)
        assertEquals(0x01020304, set.latitude.raw)
        val unack = GenericLocationGlobalSetUnacknowledged.init(locationBytes)
        assertNotNull(unack)
        assertEquals(-4, unack.longitude.raw)
    }

    @Test
    fun testLocationInvalidLengthOrNull() {
        listOf(9, 11, 0).forEach {
            assertNull(GenericLocationGlobalStatus.init(ByteArray(it)))
            assertNull(GenericLocationGlobalSet.init(ByteArray(it)))
            assertNull(GenericLocationGlobalSetUnacknowledged.init(ByteArray(it)))
        }
        assertNull(GenericLocationGlobalStatus.init(null))
        assertNull(GenericLocationGlobalSet.init(null))
        assertNull(GenericLocationGlobalSetUnacknowledged.init(null))
    }

    @Test
    fun testLocationSentinels() {
        val status = GenericLocationGlobalStatus.init(
            bytes(0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00, 0x80, 0xFF, 0x7F)
        )
        assertNotNull(status)
        assertFalse(status.latitude.isConfigured)
        assertFalse(status.longitude.isConfigured)
        assertFalse(status.altitude.isConfigured)
        assertNull(status.latitude.degrees)
        assertNull(status.longitude.degrees)
        assertNull(status.altitude.meters)
        assertEquals(Latitude.notConfigured, status.latitude)
        assertEquals(Altitude.notConfigured, status.altitude)
        assertContentEquals(
            bytes(0x00, 0x00, 0x00, 0x80, 0x00, 0x00, 0x00, 0x80, 0xFF, 0x7F),
            GenericLocationGlobalStatus(
                Latitude.notConfigured, Longitude.notConfigured, Altitude.notConfigured
            ).parameters
        )
    }

    @Test
    fun testAltitudeTooLarge() {
        val altitude = Altitude(0x7FFE)
        assertTrue(altitude.isTooLarge)
        assertFalse(altitude.isConfigured)
        assertNull(altitude.meters)
        assertEquals(altitude, Altitude.tooLarge)
    }

    @Test
    fun testCoordinateDegrees() {
        assertEquals(Int.MAX_VALUE, Latitude.fromDegrees(90.0)?.raw)
        assertEquals(-Int.MAX_VALUE, Latitude.fromDegrees(-90.0)?.raw)
        assertEquals(Int.MAX_VALUE, Longitude.fromDegrees(180.0)?.raw)
        assertEquals(-Int.MAX_VALUE, Longitude.fromDegrees(-180.0)?.raw)
        assertEquals(0, Latitude.fromDegrees(0.0)?.raw)
        assertNull(Latitude.fromDegrees(90.1))
        assertNull(Longitude.fromDegrees(-180.1))
        assertEquals(-45.0, Latitude.fromDegrees(-45.0)!!.degrees!!, 1e-6)
        assertEquals(-122.4194, Longitude.fromDegrees(-122.4194)!!.degrees!!, 1e-6)
    }
}
