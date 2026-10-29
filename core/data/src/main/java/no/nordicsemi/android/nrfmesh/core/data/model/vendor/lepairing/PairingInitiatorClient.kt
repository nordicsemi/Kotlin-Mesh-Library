package no.nordicsemi.android.nrfmesh.core.data.model.vendor.lepairing

import no.nordicsemi.android.nrfmesh.core.data.model.vendor.lepairing.message.PairingResponse
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler

class PairingInitiatorClient : ModelEventHandler() {
    override val messageTypes = mapOf(
        PairingResponse.opCode to PairingResponse.Initializer,
    )
    override val isSubscriptionSupported = false
    override val publicationMessageComposer = null

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
            msg = event.request
        )
        is ModelEvent.ResponseReceived -> null
        is ModelEvent.UnacknowledgedMessageReceived -> null
    }
}