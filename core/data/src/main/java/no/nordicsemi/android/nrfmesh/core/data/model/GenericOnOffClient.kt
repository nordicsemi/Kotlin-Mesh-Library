package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffStatus

class GenericOnOffClient : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        GenericOnOffStatus.opCode to GenericOnOffStatus.Initializer,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer
        get() = { GenericOnOffSetUnacknowledged(on = state) }
    var state: Boolean = false
        set(value) {
            field = value
            // Publishes the state change to the model.
            publish()
        }

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
            msg = event.request
        )

        is ModelEvent.ResponseReceived -> {
            // Ignore do nothing
            null
        }

        is ModelEvent.UnacknowledgedMessageReceived -> {
            // The status message may be received here if the Generic OnOff Server model
            // has been configured to publish. Ignore this message.
            null
        }
    }
}