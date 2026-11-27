package no.nordicsemi.android.nrfmesh.feature.model.generic

import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.PropertyFieldType
import no.nordicsemi.kotlin.mesh.core.messages.inputFields
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PropertyFieldTest {

    private val current = DeviceProperty.PRESENT_INPUT_CURRENT.inputFields!!.single()
    private val uvIndex = DeviceProperty.UV_INDEX.inputFields!!.single()

    @Test
    fun testDecimalIsAcceptedInsideTheRange() {
        assertTrue(current.accepts("100.0"))
        assertTrue(current.accepts("0"))
        assertTrue(current.accepts("655.34"))
    }

    @Test
    fun testDecimalIsRejectedOutsideTheRangeOrWhenItIsNotANumber() {
        assertFalse(current.accepts("655.35"))
        assertFalse(current.accepts("-1"))
        assertFalse(current.accepts(""))
        assertFalse(current.accepts("."))
    }

    @Test
    fun testWholeNumberHasNoFraction() {
        assertTrue(uvIndex.accepts("3"))
        assertFalse(uvIndex.accepts("3.5"))
        assertFalse(uvIndex.accepts("256"))
    }

    @Test
    fun testBooleanIsTrueOrFalse() {
        val presence = DeviceProperty.PRESENCE_DETECTED.inputFields!!.single()
        assertTrue(presence.type == PropertyFieldType.BOOLEAN)
        assertTrue(presence.accepts("true"))
        assertFalse(presence.accepts("1"))
    }
}
