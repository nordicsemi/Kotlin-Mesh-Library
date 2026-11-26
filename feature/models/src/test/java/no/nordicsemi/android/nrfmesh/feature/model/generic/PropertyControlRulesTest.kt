package no.nordicsemi.android.nrfmesh.feature.model.generic

import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyAccess
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertyControlRulesTest {

    private val value = DevicePropertyCharacteristic.UvIndex(3u)

    // MARK: - Whether the value can be changed

    @Test
    fun testAUserCannotWriteAReadOnlyProperty() {
        assertFalse(canWriteValue(PropertyServerKind.USER, GenericPropertyAccess.READ))
        assertFalse(canWriteValue(PropertyServerKind.USER, GenericPropertyAccess.PROHIBITED))
    }

    @Test
    fun testAUserCanWriteAWritableProperty() {
        assertTrue(canWriteValue(PropertyServerKind.USER, GenericPropertyAccess.WRITE))
        assertTrue(canWriteValue(PropertyServerKind.USER, GenericPropertyAccess.READ_WRITE))
    }

    @Test
    fun testTheValueCanBeTriedUntilTheAccessIsKnown() {
        assertTrue(canWriteValue(PropertyServerKind.USER, null))
    }

    @Test
    fun testAnAdminIsNotLimitedByTheAccessOfAUser() {
        GenericPropertyAccess.entries.forEach {
            assertTrue(canWriteValue(PropertyServerKind.ADMIN, it), it.name)
        }
    }

    // MARK: - Whether Set can be pressed

    @Test
    fun testSetNeedsATypedValue() {
        assertTrue(set(PropertyServerKind.USER, typed = value))
        assertFalse(set(PropertyServerKind.USER, typed = null))
        assertTrue(set(PropertyServerKind.ADMIN, typed = value))
        assertFalse(set(PropertyServerKind.ADMIN, typed = null))
    }

    @Test
    fun testSetNeedsAtLeastOneHexByteWhenTheValueIsNotTyped() {
        assertTrue(set(PropertyServerKind.USER, hasTypedFields = false, bytes = byteArrayOf(1)))
        assertFalse(set(PropertyServerKind.USER, hasTypedFields = false, bytes = byteArrayOf()))
        assertFalse(set(PropertyServerKind.USER, hasTypedFields = false, bytes = null))
        assertFalse(set(PropertyServerKind.ADMIN, hasTypedFields = false, bytes = byteArrayOf()))
    }

    @Test
    fun testTheAccessOfAManufacturerPropertyCanAlwaysBeSet() {
        assertTrue(set(PropertyServerKind.MANUFACTURER, hasTypedFields = false, bytes = null))
    }

    @Test
    fun testSetIsNotAllowedForAUserWhenThePropertyIsReadOnly() {
        assertFalse(set(PropertyServerKind.USER, canWrite = false, typed = value))
    }

    @Test
    fun testAnAdminMaySetTheAccessOfAReadOnlyPropertyWithoutAValue() {
        assertTrue(set(PropertyServerKind.ADMIN, canWrite = false, typed = null))
    }

    private fun set(
        kind: PropertyServerKind,
        canWrite: Boolean = true,
        hasTypedFields: Boolean = true,
        typed: DevicePropertyCharacteristic? = null,
        bytes: ByteArray? = null,
    ) = isSetAllowed(kind, canWrite, hasTypedFields, typed, bytes)

    // MARK: - Unacknowledged sets

    @Test
    fun testUnacknowledgedSetsAreRecognised() {
        val id = 0x00B1.toUShort()
        listOf(PropertyServerKind.USER, PropertyServerKind.ADMIN, PropertyServerKind.MANUFACTURER)
            .forEach { kind ->
                val unacknowledged = kind.propertySet(id, GenericPropertyAccess.READ, value, false)
                val acknowledged = kind.propertySet(id, GenericPropertyAccess.READ, value, true)
                assertTrue(unacknowledged.isUnacknowledgedPropertySet(), kind.name)
                assertFalse(acknowledged.isUnacknowledgedPropertySet(), kind.name)
            }
    }

    @Test
    fun testGetsAreNotUnacknowledgedSets() {
        val id = 0x00B1.toUShort()
        assertFalse(PropertyServerKind.USER.propertyGet(id).isUnacknowledgedPropertySet())
        assertFalse(PropertyServerKind.USER.propertiesGet().isUnacknowledgedPropertySet())
        assertFalse(null.isUnacknowledgedPropertySet())
    }
}
