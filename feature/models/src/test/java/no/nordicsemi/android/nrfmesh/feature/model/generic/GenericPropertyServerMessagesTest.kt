package no.nordicsemi.android.nrfmesh.feature.model.generic

import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyAccess
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesStatus
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
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Tests the messages sent and received by the Generic Property Server controls. */
class GenericPropertyServerMessagesTest {

    private val id: UShort = 0x00B1u
    private val value = DevicePropertyCharacteristic.UvIndex(7u)

    // ---------------------------------------------------------------- Properties Get

    @Test
    fun testPropertiesGetMatchesTheServerKind() {
        assertIs<GenericManufacturerPropertiesGet>(PropertyServerKind.MANUFACTURER.propertiesGet())
        assertIs<GenericAdminPropertiesGet>(PropertyServerKind.ADMIN.propertiesGet())
        assertIs<GenericUserPropertiesGet>(PropertyServerKind.USER.propertiesGet())
    }

    @Test
    fun testClientPropertiesGetStartsAtTheLowestValidId() {
        val message = PropertyServerKind.CLIENT.propertiesGet()
        assertIs<GenericClientPropertiesGet>(message)
        // The Property ID 0x0000 is prohibited.
        assertEquals(0x0001u.toUShort(), message.propertyId)
    }

    // ---------------------------------------------------------------- Property Get

    @Test
    fun testPropertyGetMatchesTheServerKind() {
        assertEquals(
            id,
            assertIs<GenericManufacturerPropertyGet>(PropertyServerKind.MANUFACTURER.propertyGet(id)).propertyId
        )
        assertEquals(
            id,
            assertIs<GenericAdminPropertyGet>(PropertyServerKind.ADMIN.propertyGet(id)).propertyId
        )
        assertEquals(
            id,
            assertIs<GenericUserPropertyGet>(PropertyServerKind.USER.propertyGet(id)).propertyId
        )
    }

    @Test
    fun testClientPropertyServerHasNoPropertyGet() {
        assertNull(PropertyServerKind.CLIENT.propertyGet(id))
    }

    // ---------------------------------------------------------------- Property Set

    @Test
    fun testManufacturerPropertySet() {
        val acknowledged = PropertyServerKind.MANUFACTURER
            .propertySet(id, GenericPropertyAccess.READ, null, acknowledged = true)
        assertIs<GenericManufacturerPropertySet>(acknowledged)
        assertEquals(GenericPropertyAccess.READ, acknowledged.access)

        val unacknowledged = PropertyServerKind.MANUFACTURER
            .propertySet(id, GenericPropertyAccess.PROHIBITED, null, acknowledged = false)
        assertIs<GenericManufacturerPropertySetUnacknowledged>(unacknowledged)
        assertEquals(GenericPropertyAccess.PROHIBITED, unacknowledged.access)
    }

    @Test
    fun testAdminPropertySet() {
        val acknowledged = PropertyServerKind.ADMIN
            .propertySet(id, GenericPropertyAccess.READ_WRITE, value, acknowledged = true)
        assertIs<GenericAdminPropertySet>(acknowledged)
        assertEquals(GenericPropertyAccess.READ_WRITE, acknowledged.access)
        assertEquals(value, acknowledged.value)

        val unacknowledged = PropertyServerKind.ADMIN
            .propertySet(id, GenericPropertyAccess.READ, value, acknowledged = false)
        assertIs<GenericAdminPropertySetUnacknowledged>(unacknowledged)
        assertEquals(GenericPropertyAccess.READ, unacknowledged.access)
    }

    @Test
    fun testAdminPropertySetWithoutValue() {
        val message = PropertyServerKind.ADMIN
            .propertySet(id, GenericPropertyAccess.READ, null, acknowledged = true)
        assertIs<GenericAdminPropertySet>(message)
        assertNull(message.value)
    }

    @Test
    fun testUserPropertySet() {
        val acknowledged = PropertyServerKind.USER
            .propertySet(id, null, value, acknowledged = true)
        assertIs<GenericUserPropertySet>(acknowledged)
        assertEquals(value, acknowledged.value)

        assertIs<GenericUserPropertySetUnacknowledged>(
            PropertyServerKind.USER.propertySet(id, null, value, acknowledged = false)
        )
    }

    @Test
    fun testSetNeedsWhatTheServerKindRequires() {
        // A manufacturer and an admin property are set with access rights, and a user property with a value.
        assertNull(PropertyServerKind.MANUFACTURER.propertySet(id, null, value, true))
        assertNull(PropertyServerKind.ADMIN.propertySet(id, null, value, true))
        assertNull(PropertyServerKind.USER.propertySet(id, GenericPropertyAccess.READ, null, true))
    }

    @Test
    fun testClientPropertyServerCannotBeSet() {
        assertNull(
            PropertyServerKind.CLIENT.propertySet(id, GenericPropertyAccess.READ, value, true)
        )
    }

    // ---------------------------------------------------------------- Responses

    @Test
    fun testPropertyIdsAreTakenFromEveryListStatus() {
        val ids = listOf<UShort>(0x00B1u, 0x004Du)
        assertEquals(ids, GenericManufacturerPropertiesStatus(ids).propertyIds())
        assertEquals(ids, GenericAdminPropertiesStatus(ids).propertyIds())
        assertEquals(ids, GenericUserPropertiesStatus(ids).propertyIds())
        assertEquals(ids, GenericClientPropertiesStatus(ids).propertyIds())
    }

    @Test
    fun testEmptyPropertyListIsNotMistakenForNoResponse() {
        assertEquals(emptyList(), GenericUserPropertiesStatus(emptyList()).propertyIds())
    }

    @Test
    fun testOtherMessagesHaveNoPropertyIds() {
        assertNull(GenericUserPropertyStatus(id, GenericPropertyAccess.READ, value).propertyIds())
        assertNull(GenericAdminPropertiesGet().propertyIds())
    }

    @Test
    fun testPropertyStatusIsTakenFromEveryPropertyStatus() {
        val expected = PropertyStatus(id, GenericPropertyAccess.READ_WRITE, value)
        assertEquals(
            expected,
            GenericManufacturerPropertyStatus(id, GenericPropertyAccess.READ_WRITE, value).toPropertyStatus()
        )
        assertEquals(
            expected,
            GenericAdminPropertyStatus(id, GenericPropertyAccess.READ_WRITE, value).toPropertyStatus()
        )
        assertEquals(
            expected,
            GenericUserPropertyStatus(id, GenericPropertyAccess.READ_WRITE, value).toPropertyStatus()
        )
    }

    @Test
    fun testPropertyStatusOfAPropertyThatWasNotFound() {
        val status = GenericUserPropertyStatus(id, access = null, value = null).toPropertyStatus()
        assertEquals(PropertyStatus(id, null, null), status)
    }

    @Test
    fun testListStatusIsNotAPropertyStatus() {
        assertNull(GenericUserPropertiesStatus(listOf(id)).toPropertyStatus())
    }

    // ---------------------------------------------------------------- Hex values

    @Test
    fun testHexIsParsedToBytes() {
        assertContentEquals(byteArrayOf(0x07), "07".parseHexBytes())
        assertContentEquals(byteArrayOf(0x0A, 0xFF.toByte()), "0aFF".parseHexBytes())
    }

    @Test
    fun testHexIgnoresWhiteSpace() {
        assertContentEquals(byteArrayOf(0x0A, 0xFF.toByte()), " 0A  FF ".parseHexBytes())
    }

    @Test
    fun testEmptyHexIsNoBytes() {
        assertContentEquals(byteArrayOf(), "".parseHexBytes())
        assertContentEquals(byteArrayOf(), "  ".parseHexBytes())
    }

    @Test
    fun testInvalidHexIsRejected() {
        assertNull("0".parseHexBytes())
        assertNull("ABC".parseHexBytes())
        assertNull("0G".parseHexBytes())
        assertNull("0x07".parseHexBytes())
    }
}
