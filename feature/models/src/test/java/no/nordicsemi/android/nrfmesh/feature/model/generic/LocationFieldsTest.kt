package no.nordicsemi.android.nrfmesh.feature.model.generic

import no.nordicsemi.kotlin.mesh.core.messages.Altitude
import no.nordicsemi.kotlin.mesh.core.messages.Latitude
import no.nordicsemi.kotlin.mesh.core.messages.Longitude
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/** Tests the values shown in the location fields of the Generic Location Setup Server. */
class LocationFieldsTest {

    @Test
    fun testDegreesAreShownWithSixDecimals() {
        assertEquals("27.988056", formatDegrees(27.988056))
        assertEquals("0.000000", formatDegrees(0.0))
        assertEquals("-33.868800", formatDegrees(-33.8688))
        assertEquals("180.000000", formatDegrees(180.0))
    }

    @Test
    fun testDegreesAreShownWithADotInEveryLocale() {
        val default = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY)
            assertEquals("59.913900", formatDegrees(59.9139))
        } finally {
            java.util.Locale.setDefault(default)
        }
    }

    @Test
    fun testTheSixthDecimalIsRounded() {
        assertEquals("10.752200", formatDegrees(10.7522004))
        assertEquals("10.752201", formatDegrees(10.7522006))
    }

    @Test
    fun testTheDefaultValuesAreValid() {
        val latitude = assertNotNull(Latitude.fromDegrees(EVEREST_LATITUDE.toDouble()))
        val longitude = assertNotNull(Longitude.fromDegrees(EVEREST_LONGITUDE.toDouble()))
        val altitude = assertNotNull(EVEREST_ALTITUDE.toShortOrNull())
        assertEquals(27.988056, assertNotNull(latitude.degrees), 0.000001)
        assertEquals(86.925278, assertNotNull(longitude.degrees), 0.000001)
        assertEquals(8849.toShort(), altitude)
    }

    @Test
    fun testTheDefaultValuesCanBeSent() {
        val message = globalLocationSetOf(
            latitude = assertNotNull(Latitude.fromDegrees(EVEREST_LATITUDE.toDouble())),
            longitude = assertNotNull(Longitude.fromDegrees(EVEREST_LONGITUDE.toDouble())),
            altitude = Altitude(EVEREST_ALTITUDE.toShort()),
            acknowledged = true
        )
        assertIs<GenericLocationGlobalSet>(message)
        assertEquals(10, message.parameters.size)
    }

    @Test
    fun testAValueShownInTheFieldIsSentAsShown() {
        // A status is shown in the fields, and the shown values are what is sent next.
        val status = GenericLocationGlobalStatus(
            latitude = assertNotNull(Latitude.fromDegrees(58.094166)),
            longitude = assertNotNull(Longitude.fromDegrees(7.768162)),
            altitude = Altitude(59)
        )
        val shown = formatDegrees(assertNotNull(status.latitude.degrees))
        assertEquals(58.094166, shown.toDouble(), 0.000001)
        assertEquals(
            status.latitude.raw,
            assertNotNull(Latitude.fromDegrees(shown.toDouble())).raw,
        )
    }

    @Test
    fun testTextThatIsNotANumberIsNotSent() {
        assertNull("".toDoubleOrNull()?.let { Latitude.fromDegrees(it) })
        assertNull("abc".toDoubleOrNull()?.let { Latitude.fromDegrees(it) })
        assertNull("91".toDoubleOrNull()?.let { Latitude.fromDegrees(it) })
        assertNull("181".toDoubleOrNull()?.let { Longitude.fromDegrees(it) })
        assertNull("40000".toShortOrNull())
    }
}
