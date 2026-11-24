package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeStatus
import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests the state, supported messages and published message of the Generic Default Transition
 * Time Client.
 */
class GenericDefaultTransitionTimeClientTest {

    @Test
    fun testInitialTransitionTimeIsImmediate() {
        assertEquals(TransitionTime(rawValue = 0u), GenericDefaultTransitionTimeClient().transitionTime)
    }

    @Test
    fun testSupportsStatusAndSubscription() {
        val client = GenericDefaultTransitionTimeClient()
        assertTrue(client.isSubscriptionSupported)
        assertEquals(
            setOf(GenericDefaultTransitionTimeStatus.opCode),
            client.messageTypes.keys
        )
    }

    @Test
    fun testStatusIsDecodedByTheRegisteredInitializer() {
        val client = GenericDefaultTransitionTimeClient()
        val initializer =
            assertNotNull(client.messageTypes[GenericDefaultTransitionTimeStatus.opCode])
        // 5 steps of 1 second: resolution 1 (seconds) in the two most significant bits.
        val status = initializer.init(parameters = byteArrayOf(0x45))
        assertIs<GenericDefaultTransitionTimeStatus>(status)
        assertEquals(
            TransitionTime(steps = 5u, stepResolution = StepResolution.SECONDS),
            status.transitionTime
        )
    }

    @Test
    fun testPublishedMessageIsUnacknowledgedSetWithCurrentTime() {
        val composer =
            assertNotNull(GenericDefaultTransitionTimeClient().publicationMessageComposer)
        val message = composer()
        assertIs<GenericDefaultTransitionTimeSetUnacknowledged>(message)
        assertEquals(TransitionTime(rawValue = 0u), message.transitionTime)
    }
}
