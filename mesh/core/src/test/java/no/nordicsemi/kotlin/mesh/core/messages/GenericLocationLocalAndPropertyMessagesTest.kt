package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertyStatus
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenericLocationLocalAndPropertyMessagesTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    private val uvIndex = DeviceProperty.UV_INDEX
    private val uvValue = DevicePropertyCharacteristic.UvIndex(7u)

    // ---------------------------------------------------------------- Location Local

    @Test
    fun testLocationLocalOpCodes() {
        assertEquals(0x8226u, GenericLocationLocalGet.opCode)
        assertEquals(0x8227u, GenericLocationLocalStatus.opCode)
        assertEquals(0x8228u, GenericLocationLocalSet.opCode)
        assertEquals(0x8229u, GenericLocationLocalSetUnacknowledged.opCode)
    }

    @Test
    fun testLocationLocalGet() {
        val get = GenericLocationLocalGet()
        assertNull(get.parameters)
        assertEquals(GenericLocationLocalStatus.opCode, get.responseOpCode)
        assertNotNull(GenericLocationLocalGet.init(ByteArray(0)))
        assertNull(GenericLocationLocalGet.init(bytes(0)))
        assertNull(GenericLocationLocalGet.init(null))
    }

    // north = 0x0102, east = 0xFFFE (-2), altitude = 0x7FFF, floor = 0xFD, uncertainty = 0x3A01
    private val localBytes = bytes(0x02, 0x01, 0xFE, 0xFF, 0xFF, 0x7F, 0xFD, 0x01, 0x3A)

    @Test
    fun testLocationLocalEncodingAndDecoding() {
        val uncertainty = LocationUncertainty(0x3A01u)
        val set = GenericLocationLocalSet(
            north = 0x0102,
            east = -2,
            altitude = 0x7FFF,
            floor = FloorNumber.GROUND_FLOOR_0,
            uncertainty = uncertainty
        )
        assertContentEquals(localBytes, set.parameters)
        assertEquals(GenericLocationLocalStatus.opCode, set.responseOpCode)

        val status = GenericLocationLocalStatus.init(localBytes)
        assertNotNull(status)
        assertEquals(0x0102, status.north)
        assertEquals(-2, status.east)
        assertEquals(Altitude.NOT_CONFIGURED, status.altitude)
        assertEquals(FloorNumber.GROUND_FLOOR_0, status.floor)
        assertEquals(uncertainty, status.uncertainty)
        assertContentEquals(localBytes, status.parameters)

        assertContentEquals(localBytes, GenericLocationLocalSetUnacknowledged.init(localBytes)?.parameters)
        assertContentEquals(localBytes, GenericLocationLocalSet.init(localBytes)?.parameters)
    }

    @Test
    fun testLocationLocalInvalidLengthIsRejected() {
        listOf(0, 8, 10).forEach {
            assertNull(GenericLocationLocalStatus.init(ByteArray(it)))
            assertNull(GenericLocationLocalSet.init(ByteArray(it)))
            assertNull(GenericLocationLocalSetUnacknowledged.init(ByteArray(it)))
        }
        assertNull(GenericLocationLocalStatus.init(null))
    }

    @Test
    fun testFloorNumber() {
        assertEquals(-20, FloorNumber(0x00u).number)
        assertEquals(0, FloorNumber(0x14u).number)
        assertEquals(231, FloorNumber(0xFBu).number)
        assertEquals(0, FloorNumber.GROUND_FLOOR_0.number)
        assertEquals(1, FloorNumber.GROUND_FLOOR_1.number)
        assertTrue(FloorNumber.GROUND_FLOOR_1.isGroundFloor)
        assertNull(FloorNumber.UNKNOWN.number)
        assertFalse(FloorNumber.UNKNOWN.isKnown)
        assertEquals(232, FloorNumber(0xFCu).number)
        assertEquals(FloorNumber(0x00u), FloorNumber.of(-20))
        assertEquals(FloorNumber(0xFBu), FloorNumber.of(231))
        assertEquals(FloorNumber(0xFCu), FloorNumber.of(232))
        assertNull(FloorNumber.of(-21))
        assertNull(FloorNumber.of(233))
    }

    @Test
    fun testLocationUncertainty() {
        val uncertainty = LocationUncertainty.of(isMobile = true, updateTimeRaw = 3, precisionRaw = 4)
        assertEquals(0x4301u.toUShort(), uncertainty.raw)
        assertTrue(uncertainty.isMobile)
        assertEquals(1000L, uncertainty.updateTimeMillis)
        assertEquals(2000L, uncertainty.precisionMillimeters)

        val coarsest = LocationUncertainty.of(isMobile = false)
        assertFalse(coarsest.isMobile)
        assertEquals(4_096_000L, coarsest.updateTimeMillis)
        assertEquals(4_096_000L, coarsest.precisionMillimeters)
    }

    // ---------------------------------------------------------------- Property op codes

    @Test
    fun testPropertyOpCodes() {
        assertEquals(0x822Au, GenericManufacturerPropertiesGet.opCode)
        assertEquals(0x43u, GenericManufacturerPropertiesStatus.opCode)
        assertEquals(0x822Bu, GenericManufacturerPropertyGet.opCode)
        assertEquals(0x44u, GenericManufacturerPropertySet.opCode)
        assertEquals(0x45u, GenericManufacturerPropertySetUnacknowledged.opCode)
        assertEquals(0x46u, GenericManufacturerPropertyStatus.opCode)

        assertEquals(0x822Cu, GenericAdminPropertiesGet.opCode)
        assertEquals(0x47u, GenericAdminPropertiesStatus.opCode)
        assertEquals(0x822Du, GenericAdminPropertyGet.opCode)
        assertEquals(0x48u, GenericAdminPropertySet.opCode)
        assertEquals(0x49u, GenericAdminPropertySetUnacknowledged.opCode)
        assertEquals(0x4Au, GenericAdminPropertyStatus.opCode)

        assertEquals(0x822Eu, GenericUserPropertiesGet.opCode)
        assertEquals(0x4Bu, GenericUserPropertiesStatus.opCode)
        assertEquals(0x822Fu, GenericUserPropertyGet.opCode)
        assertEquals(0x4Cu, GenericUserPropertySet.opCode)
        assertEquals(0x4Du, GenericUserPropertySetUnacknowledged.opCode)
        assertEquals(0x4Eu, GenericUserPropertyStatus.opCode)

        assertEquals(0x4Fu, GenericClientPropertiesGet.opCode)
        assertEquals(0x50u, GenericClientPropertiesStatus.opCode)
    }

    @Test
    fun testResponseOpCodes() {
        assertEquals(GenericManufacturerPropertiesStatus.opCode, GenericManufacturerPropertiesGet().responseOpCode)
        assertEquals(GenericManufacturerPropertyStatus.opCode, GenericManufacturerPropertyGet(uvIndex).responseOpCode)
        assertEquals(
            GenericManufacturerPropertyStatus.opCode,
            GenericManufacturerPropertySet(uvIndex, GenericPropertyAccess.READ).responseOpCode
        )
        assertEquals(GenericAdminPropertiesStatus.opCode, GenericAdminPropertiesGet().responseOpCode)
        assertEquals(GenericAdminPropertyStatus.opCode, GenericAdminPropertyGet(uvIndex).responseOpCode)
        assertEquals(
            GenericAdminPropertyStatus.opCode,
            GenericAdminPropertySet(uvIndex, GenericPropertyAccess.READ_WRITE, uvValue).responseOpCode
        )
        assertEquals(GenericUserPropertiesStatus.opCode, GenericUserPropertiesGet().responseOpCode)
        assertEquals(GenericUserPropertyStatus.opCode, GenericUserPropertyGet(uvIndex).responseOpCode)
        assertEquals(GenericUserPropertyStatus.opCode, GenericUserPropertySet(uvIndex, uvValue).responseOpCode)
        assertEquals(GenericClientPropertiesStatus.opCode, GenericClientPropertiesGet(0x0001u).responseOpCode)
    }

    // ---------------------------------------------------------------- Properties Get / Status

    @Test
    fun testPropertiesGetHasNoParameters() {
        listOf(
            GenericManufacturerPropertiesGet.Initializer,
            GenericAdminPropertiesGet.Initializer,
            GenericUserPropertiesGet.Initializer,
        ).forEach {
            assertNotNull(it.init(ByteArray(0)))
            assertNull(it.init(bytes(0x01)))
            assertNull(it.init(null))
        }
        assertNull(GenericManufacturerPropertiesGet().parameters)
    }

    @Test
    fun testPropertiesStatus() {
        val data = bytes(0xB1, 0x00, 0x4D, 0x00)
        val ids = listOf<UShort>(0x00B1u, 0x004Du)

        assertContentEquals(data, GenericManufacturerPropertiesStatus(ids).parameters)
        assertEquals(ids, GenericManufacturerPropertiesStatus.init(data)?.propertyIds)
        assertEquals(ids, GenericAdminPropertiesStatus.init(data)?.propertyIds)
        assertEquals(ids, GenericUserPropertiesStatus.init(data)?.propertyIds)
        assertEquals(ids, GenericClientPropertiesStatus.init(data)?.propertyIds)
        assertEquals(
            listOf(DeviceProperty.UV_INDEX, DeviceProperty.PRESENCE_DETECTED),
            GenericUserPropertiesStatus(ids).properties
        )
    }

    @Test
    fun testPropertiesStatusEmptyAndInvalid() {
        assertEquals(emptyList(), GenericUserPropertiesStatus.init(ByteArray(0))?.propertyIds)
        assertContentEquals(ByteArray(0), GenericUserPropertiesStatus(emptyList()).parameters)
        assertNull(GenericUserPropertiesStatus.init(bytes(0x01)))
        assertNull(GenericUserPropertiesStatus.init(bytes(0x01, 0x02, 0x03)))
        assertNull(GenericUserPropertiesStatus.init(null))
    }

    @Test
    fun testClientPropertiesGet() {
        val get = GenericClientPropertiesGet(0x1234u)
        assertContentEquals(bytes(0x34, 0x12), get.parameters)
        assertEquals(0x1234u.toUShort(), GenericClientPropertiesGet.init(bytes(0x34, 0x12))?.propertyId)
        assertNull(GenericClientPropertiesGet.init(bytes(0x34)))
        assertNull(GenericClientPropertiesGet.init(bytes(0x34, 0x12, 0x00)))
    }

    // ---------------------------------------------------------------- Property Get

    @Test
    fun testPropertyGet() {
        val data = bytes(0xB1, 0x00)
        listOf(
            GenericManufacturerPropertyGet(uvIndex).parameters,
            GenericAdminPropertyGet(uvIndex).parameters,
            GenericUserPropertyGet(uvIndex).parameters,
        ).forEach { assertContentEquals(data, it) }

        assertEquals(uvIndex, GenericManufacturerPropertyGet.init(data)?.property)
        assertEquals(uvIndex, GenericAdminPropertyGet.init(data)?.property)
        assertEquals(uvIndex, GenericUserPropertyGet.init(data)?.property)
        listOf(0, 1, 3).forEach {
            assertNull(GenericUserPropertyGet.init(ByteArray(it)))
            assertNull(GenericAdminPropertyGet.init(ByteArray(it)))
            assertNull(GenericManufacturerPropertyGet.init(ByteArray(it)))
        }
    }

    // ---------------------------------------------------------------- Property Set

    @Test
    fun testManufacturerPropertySet() {
        val data = bytes(0xB1, 0x00, 0x01)
        val set = GenericManufacturerPropertySet(uvIndex, GenericPropertyAccess.READ)
        assertContentEquals(data, set.parameters)
        assertContentEquals(
            data,
            GenericManufacturerPropertySetUnacknowledged(uvIndex, GenericPropertyAccess.READ).parameters
        )

        val decoded = GenericManufacturerPropertySet.init(data)
        assertNotNull(decoded)
        assertEquals(GenericPropertyAccess.READ, decoded.access)
        assertNotNull(GenericManufacturerPropertySetUnacknowledged.init(data))
        assertNotNull(GenericManufacturerPropertySet.init(bytes(0xB1, 0x00, 0x00)))
    }

    @Test
    fun testManufacturerPropertySetRejectsWriteAccess() {
        assertFailsWith<IllegalArgumentException> {
            GenericManufacturerPropertySet(uvIndex, GenericPropertyAccess.WRITE)
        }
        assertFailsWith<IllegalArgumentException> {
            GenericManufacturerPropertySetUnacknowledged(uvIndex, GenericPropertyAccess.READ_WRITE)
        }
        assertNull(GenericManufacturerPropertySet.init(bytes(0xB1, 0x00, 0x02)))
        assertNull(GenericManufacturerPropertySet.init(bytes(0xB1, 0x00, 0x03)))
        assertNull(GenericManufacturerPropertySet.init(bytes(0xB1, 0x00, 0x04)))
        assertNull(GenericManufacturerPropertySet.init(bytes(0xB1, 0x00)))
        assertNull(GenericManufacturerPropertySet.init(bytes(0xB1, 0x00, 0x01, 0x00)))
    }

    @Test
    fun testAdminPropertySet() {
        val data = bytes(0xB1, 0x00, 0x03, 0x07)
        val set = GenericAdminPropertySet(uvIndex, GenericPropertyAccess.READ_WRITE, uvValue)
        assertContentEquals(data, set.parameters)
        assertContentEquals(
            data,
            GenericAdminPropertySetUnacknowledged(uvIndex, GenericPropertyAccess.READ_WRITE, uvValue).parameters
        )

        val decoded = GenericAdminPropertySet.init(data)
        assertNotNull(decoded)
        assertEquals(GenericPropertyAccess.READ_WRITE, decoded.access)
        assertEquals(uvValue, decoded.value)
        assertNotNull(GenericAdminPropertySetUnacknowledged.init(data))
    }

    @Test
    fun testAdminPropertySetWithoutValueAndInvalid() {
        val noValue = GenericAdminPropertySet.init(bytes(0xB1, 0x00, 0x01))
        assertNotNull(noValue)
        assertNull(noValue.value)
        assertContentEquals(bytes(0xB1, 0x00, 0x01), noValue.parameters)

        // Reserved access value.
        assertNull(GenericAdminPropertySet.init(bytes(0xB1, 0x00, 0x04, 0x07)))
        // UV Index is one byte long.
        assertNull(GenericAdminPropertySet.init(bytes(0xB1, 0x00, 0x03, 0x07, 0x08)))
        assertNull(GenericAdminPropertySet.init(bytes(0xB1, 0x00)))
    }

    @Test
    fun testUserPropertySet() {
        val data = bytes(0xB1, 0x00, 0x07)
        assertContentEquals(data, GenericUserPropertySet(uvIndex, uvValue).parameters)
        assertContentEquals(data, GenericUserPropertySetUnacknowledged(uvIndex, uvValue).parameters)

        val decoded = GenericUserPropertySet.init(data)
        assertNotNull(decoded)
        assertEquals(uvValue, decoded.value)
        assertNotNull(GenericUserPropertySetUnacknowledged.init(data))

        assertNull(GenericUserPropertySet.init(bytes(0xB1, 0x00)))
        assertNull(GenericUserPropertySet.init(bytes(0xB1, 0x00, 0x07, 0x08)))
    }

    @Test
    fun testUnknownPropertyKeepsRawValue() {
        // 0xF0F0 is not a known Device Property, so the value is kept as it was received.
        val data = bytes(0xF0, 0xF0, 0x01, 0x02, 0x03)
        val set = GenericUserPropertySet.init(data)
        assertNotNull(set)
        assertNull(set.property)
        assertEquals(DevicePropertyCharacteristic.Other(bytes(0x01, 0x02, 0x03)), set.value)
        assertContentEquals(data, set.parameters)
    }

    // ---------------------------------------------------------------- Property Status

    @Test
    fun testPropertyStatusWithValue() {
        val data = bytes(0xB1, 0x00, 0x03, 0x07)
        listOf(
            GenericManufacturerPropertyStatus.init(data)?.let { it.access to it.value },
            GenericAdminPropertyStatus.init(data)?.let { it.access to it.value },
            GenericUserPropertyStatus.init(data)?.let { it.access to it.value },
        ).forEach {
            assertEquals(GenericPropertyAccess.READ_WRITE to uvValue, it)
        }
        assertContentEquals(
            data,
            GenericAdminPropertyStatus(uvIndex, GenericPropertyAccess.READ_WRITE, uvValue).parameters
        )
    }

    @Test
    fun testPropertyStatusNotFound() {
        val data = bytes(0xB1, 0x00)
        val status = GenericUserPropertyStatus.init(data)
        assertNotNull(status)
        assertEquals(0x00B1u.toUShort(), status.propertyId)
        assertNull(status.access)
        assertNull(status.value)
        assertContentEquals(data, status.parameters)
        assertContentEquals(data, GenericAdminPropertyStatus(uvIndex).parameters)
        assertContentEquals(data, GenericManufacturerPropertyStatus(uvIndex).parameters)
    }

    @Test
    fun testPropertyStatusWithoutValue() {
        // A user property that cannot be read carries the access rights only.
        val data = bytes(0xB1, 0x00, 0x00)
        val status = GenericUserPropertyStatus.init(data)
        assertNotNull(status)
        assertEquals(GenericPropertyAccess.PROHIBITED, status.access)
        assertNull(status.value)
        assertContentEquals(data, status.parameters)
    }

    @Test
    fun testPropertyStatusInvalid() {
        assertNull(GenericUserPropertyStatus.init(bytes(0xB1)))
        assertNull(GenericUserPropertyStatus.init(bytes(0xB1, 0x00, 0x04)))
        assertNull(GenericUserPropertyStatus.init(bytes(0xB1, 0x00, 0x01, 0x07, 0x08)))
        assertNull(GenericUserPropertyStatus.init(null))
        assertFailsWith<IllegalArgumentException> {
            GenericUserPropertyStatus(propertyId = 0x00B1u, access = null, value = uvValue)
        }
    }
}
