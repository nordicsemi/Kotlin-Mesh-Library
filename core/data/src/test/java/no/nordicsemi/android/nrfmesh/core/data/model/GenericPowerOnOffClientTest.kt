package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.OnPowerUp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Tests the state, supported messages and published message of the Generic Power OnOff Client. */
class GenericPowerOnOffClientTest {

    @Test
    fun testInitialStateIsDefault() {
        assertEquals(OnPowerUp.DEFAULT, GenericPowerOnOffClient().state)
    }

    @Test
    fun testSupportsOnPowerUpStatusAndSubscription() {
        val client = GenericPowerOnOffClient()
        assertTrue(client.isSubscriptionSupported)
        assertEquals(setOf(GenericOnPowerUpStatus.opCode), client.messageTypes.keys)
    }

    @Test
    fun testStatusIsDecodedByTheRegisteredInitializer() {
        val client = GenericPowerOnOffClient()
        val initializer = assertNotNull(client.messageTypes[GenericOnPowerUpStatus.opCode])
        val status = initializer.init(parameters = byteArrayOf(0x02))
        assertIs<GenericOnPowerUpStatus>(status)
        assertEquals(OnPowerUp.RESTORE, status.state)
    }

    @Test
    fun testPublishedMessageIsUnacknowledgedSetWithCurrentState() {
        val composer = assertNotNull(GenericPowerOnOffClient().publicationMessageComposer)
        val message = composer()
        assertIs<GenericOnPowerUpSetUnacknowledged>(message)
        assertEquals(OnPowerUp.DEFAULT, message.state)
    }
}
