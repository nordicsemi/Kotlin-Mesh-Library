package no.nordicsemi.kotlin.mesh.core.layers.access

import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelayGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelayStatus
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.Publish
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResponseMatchingTest {

    private fun get(element: UShort, model: UShort) = ConfigModelPublicationGet(
        elementAddress = UnicastAddress(address = element),
        modelIdentifier = model,
        companyIdentifier = null
    )

    private fun status(element: UShort, model: UShort) = ConfigModelPublicationStatus(
        elementAddress = UnicastAddress(address = element),
        modelIdentifier = model,
        companyIdentifier = null,
        publish = Publish()
    )

    @Test
    fun testStatusOfTheSameModelIsAResponse() {
        assertTrue(isResponseTo(get(0x0017u, 0x1000u), status(0x0017u, 0x1000u)))
    }

    @Test
    fun testStatusOfAnotherModelIsNotAResponse() {
        // For example a late duplicate of the status of the previous request.
        assertFalse(isResponseTo(get(0x0017u, 0x1000u), status(0x0017u, 0x1311u)))
    }

    @Test
    fun testStatusOfAnotherElementIsNotAResponse() {
        assertFalse(isResponseTo(get(0x0017u, 0x1000u), status(0x0018u, 0x1000u)))
    }

    @Test
    fun testMessagesWithoutAModelAreMatchedByOpcodeOnly() {
        assertTrue(
            isResponseTo(ConfigRelayGet(), ConfigRelayStatus(FeatureState.Disabled, 0u, 0u))
        )
        assertTrue(isResponseTo(get(0x0017u, 0x1000u), response = null))
    }
}
