package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertyStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests that the Generic Location and Property clients decode the status messages. */
class GenericLocationAndPropertyClientTest {

    @Test
    fun testLocationClientSupportsGlobalAndLocalStatus() {
        val client = GenericLocationClient()
        assertTrue(client.isSubscriptionSupported)
        assertNull(client.publicationMessageComposer)
        assertEquals(
            setOf(GenericLocationGlobalStatus.opCode, GenericLocationLocalStatus.opCode),
            client.messageTypes.keys
        )
    }

    @Test
    fun testLocalStatusIsDecodedByTheRegisteredInitializer() {
        val initializer = assertNotNull(
            GenericLocationClient().messageTypes[GenericLocationLocalStatus.opCode]
        )
        val status = initializer.init(
            parameters = byteArrayOf(0x02, 0x01, 0xFE.toByte(), 0xFF.toByte(), 0x2D, 0x00, 0xFC.toByte(), 0x01, 0x3A)
        )
        assertIs<GenericLocationLocalStatus>(status)
        assertEquals(0x0102, status.north)
        assertEquals(-2, status.east)
    }

    @Test
    fun testPropertyClientSupportsAllStatusMessages() {
        val client = GenericPropertyClient()
        assertTrue(client.isSubscriptionSupported)
        assertEquals(
            setOf(
                GenericManufacturerPropertiesStatus.opCode,
                GenericManufacturerPropertyStatus.opCode,
                GenericAdminPropertiesStatus.opCode,
                GenericAdminPropertyStatus.opCode,
                GenericUserPropertiesStatus.opCode,
                GenericUserPropertyStatus.opCode,
                GenericClientPropertiesStatus.opCode,
            ),
            client.messageTypes.keys
        )
    }

    @Test
    fun testPropertyStatusIsDecodedByTheRegisteredInitializer() {
        val initializer = assertNotNull(
            GenericPropertyClient().messageTypes[GenericUserPropertyStatus.opCode]
        )
        val status = initializer.init(parameters = byteArrayOf(0xB1.toByte(), 0x00, 0x03, 0x07))
        assertIs<GenericUserPropertyStatus>(status)
        assertEquals(0x00B1u.toUShort(), status.propertyId)
    }
}
