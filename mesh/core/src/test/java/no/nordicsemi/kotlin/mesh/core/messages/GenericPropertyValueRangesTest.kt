package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertyStatus
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Tests the supported ranges and the reserved values of the Generic Property values. */
class GenericPropertyValueRangesTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    // ---------------------------------------------------------------- Access

    @Test
    fun testEveryAccessValue() {
        assertEquals(GenericPropertyAccess.PROHIBITED, GenericPropertyAccess.from(0x00u))
        assertEquals(GenericPropertyAccess.READ, GenericPropertyAccess.from(0x01u))
        assertEquals(GenericPropertyAccess.WRITE, GenericPropertyAccess.from(0x02u))
        assertEquals(GenericPropertyAccess.READ_WRITE, GenericPropertyAccess.from(0x03u))
    }

    @Test
    fun testReservedAccessValues() {
        listOf(0x04, 0x05, 0x7F, 0x80, 0xFE, 0xFF).forEach {
            assertNull(GenericPropertyAccess.from(it.toUByte()), "access $it")
        }
    }

    @Test
    fun testAccessFlags() {
        assertEquals(listOf(false, true, false, true), GenericPropertyAccess.entries.map { it.canRead })
        assertEquals(listOf(false, false, true, true), GenericPropertyAccess.entries.map { it.canWrite })
    }

    @Test
    fun testStatusWithAReservedAccessIsRejected() {
        listOf(0x04, 0x80, 0xFF).forEach {
            assertNull(GenericUserPropertyStatus.init(bytes(0xB1, 0x00, it)), "access $it")
            assertNull(GenericUserPropertyStatus.init(bytes(0xB1, 0x00, it, 0x07)), "access $it")
            assertNull(GenericAdminPropertySet.init(bytes(0xB1, 0x00, it, 0x07)), "access $it")
        }
    }

    @Test
    fun testManufacturerSetOnlyAcceptsProhibitedAndRead() {
        listOf(0x00, 0x01).forEach {
            assertNotNull(GenericManufacturerPropertySet.init(bytes(0xB1, 0x00, it)), "access $it")
            assertNotNull(GenericManufacturerPropertySetUnacknowledged.init(bytes(0xB1, 0x00, it)))
        }
        listOf(0x02, 0x03, 0x04, 0xFF).forEach {
            assertNull(GenericManufacturerPropertySet.init(bytes(0xB1, 0x00, it)), "access $it")
            assertNull(GenericManufacturerPropertySetUnacknowledged.init(bytes(0xB1, 0x00, it)))
        }
        listOf(GenericPropertyAccess.WRITE, GenericPropertyAccess.READ_WRITE).forEach {
            assertFailsWith<IllegalArgumentException> { GenericManufacturerPropertySet(0x00B1u, it) }
        }
    }

    // ---------------------------------------------------------------- Property IDs

    @Test
    fun testPropertyIdLimits() {
        assertContentEquals(bytes(0x01, 0x00), GenericUserPropertyGet(0x0001u).parameters)
        assertContentEquals(bytes(0xFF, 0xFF), GenericUserPropertyGet(0xFFFFu).parameters)
        assertEquals(0xFFFFu.toUShort(), GenericUserPropertyGet.init(bytes(0xFF, 0xFF))?.propertyId)
        assertEquals(0x0001u.toUShort(), GenericUserPropertyGet.init(bytes(0x01, 0x00))?.propertyId)
    }

    @Test
    fun testPropertyIdByteOrderIsLittleEndian() {
        assertContentEquals(bytes(0x34, 0x12), GenericUserPropertyGet(0x1234u).parameters)
        assertContentEquals(bytes(0x34, 0x12), GenericClientPropertiesGet(0x1234u).parameters)
    }

    @Test
    fun testPropertyListWithTheLimitIds() {
        val ids = listOf<UShort>(0x0001u, 0x7FFFu, 0x8000u, 0xFFFFu)
        val status = GenericUserPropertiesStatus(ids)
        assertContentEquals(bytes(0x01, 0x00, 0xFF, 0x7F, 0x00, 0x80, 0xFF, 0xFF), status.parameters)
        assertEquals(ids, GenericUserPropertiesStatus.init(status.parameters)?.propertyIds)
    }

    @Test
    fun testLongPropertyList() {
        val ids = List(189) { (it + 1).toUShort() }
        val status = GenericUserPropertiesStatus(ids)
        assertEquals(378, status.parameters.size)
        assertEquals(ids, GenericUserPropertiesStatus.init(status.parameters)?.propertyIds)
    }

    // ---------------------------------------------------------------- Value lengths

    @Test
    fun testValueOfAOneByteProperty() {
        // UV Index (0x00B1) is one byte long.
        assertNotNull(GenericUserPropertySet.init(bytes(0xB1, 0x00, 0x00)))
        assertNotNull(GenericUserPropertySet.init(bytes(0xB1, 0x00, 0xFF)))
        assertNull(GenericUserPropertySet.init(bytes(0xB1, 0x00)))
        assertNull(GenericUserPropertySet.init(bytes(0xB1, 0x00, 0x00, 0x00)))
    }

    @Test
    fun testValueOfAThreeByteProperty() {
        // Average Input Current (0x0002) is three bytes long.
        assertNull(GenericUserPropertySet.init(bytes(0x02, 0x00, 0x01, 0x02)))
        assertNotNull(GenericUserPropertySet.init(bytes(0x02, 0x00, 0x01, 0x02, 0x03)))
        assertNull(GenericUserPropertySet.init(bytes(0x02, 0x00, 0x01, 0x02, 0x03, 0x04)))
    }

    @Test
    fun testValueOfAnUnknownPropertyCanHaveAnyLength() {
        listOf(1, 2, 8, 100, 370).forEach { length ->
            val data = bytes(0xF0, 0xF0) + ByteArray(length) { 0x5A }
            val message = assertNotNull(GenericUserPropertySet.init(data), "length $length")
            assertContentEquals(data, message.parameters)
        }
    }

    @Test
    fun testOneByteValueLimits() {
        listOf(0x00, 0x01, 0x7F, 0x80, 0xFF).forEach {
            val message = assertNotNull(GenericUserPropertySet.init(bytes(0xB1, 0x00, it)))
            assertIs<DevicePropertyCharacteristic.UvIndex>(message.value)
            assertEquals(it.toUByte(), (message.value as DevicePropertyCharacteristic.UvIndex).value)
            assertContentEquals(bytes(0xB1, 0x00, it), message.parameters)
        }
    }

    @Test
    fun testAdminSetWithAndWithoutAValue() {
        val withoutValue = assertNotNull(GenericAdminPropertySet.init(bytes(0xB1, 0x00, 0x03)))
        assertNull(withoutValue.value)
        val withValue = assertNotNull(GenericAdminPropertySet.init(bytes(0xB1, 0x00, 0x03, 0xFF)))
        assertEquals(DevicePropertyCharacteristic.UvIndex(255u), withValue.value)
    }

    @Test
    fun testStatusAccessAndValueCombinations() {
        // Not found, access only, and access with a value.
        assertNull(assertNotNull(GenericAdminPropertyStatus.init(bytes(0xB1, 0x00))).access)
        val noValue = assertNotNull(GenericAdminPropertyStatus.init(bytes(0xB1, 0x00, 0x00)))
        assertEquals(GenericPropertyAccess.PROHIBITED, noValue.access)
        assertNull(noValue.value)
        val full = assertNotNull(GenericAdminPropertyStatus.init(bytes(0xB1, 0x00, 0x03, 0x07)))
        assertEquals(GenericPropertyAccess.READ_WRITE, full.access)
        assertEquals(DevicePropertyCharacteristic.UvIndex(7u), full.value)
    }
}
