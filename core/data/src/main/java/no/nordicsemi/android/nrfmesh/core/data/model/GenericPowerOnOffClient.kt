package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.OnPowerUp

/**
 * Handles the Generic Power OnOff Client model. It allows the [GenericOnPowerUpStatus] to be
 * decoded and delivered as a response.
 */
class GenericPowerOnOffClient : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        GenericOnPowerUpStatus.opCode to GenericOnPowerUpStatus.Initializer,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer
        get() = { GenericOnPowerUpSetUnacknowledged(state = state) }

    /** The Generic OnPowerUp state published by this model. */
    var state: OnPowerUp = OnPowerUp.DEFAULT
        set(value) {
            field = value
            // Publishes the state change to the model.
            val _ = publish()
        }

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
            msg = event.request
        )

        // The status may be received here if the Generic Power OnOff Server has been configured
        // to publish, or as a response. Nothing to do.
        is ModelEvent.ResponseReceived,
        is ModelEvent.UnacknowledgedMessageReceived,
        -> null
    }
}
