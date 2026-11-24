package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeStatus
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Handles the Generic Default Transition Time Client model. It allows the
 * [GenericDefaultTransitionTimeStatus] to be decoded and delivered as a response.
 */
class GenericDefaultTransitionTimeClient : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        GenericDefaultTransitionTimeStatus.opCode to GenericDefaultTransitionTimeStatus.Initializer,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer
        get() = { GenericDefaultTransitionTimeSetUnacknowledged(transitionTime = transitionTime) }

    /** The Default Transition Time published by this model. */
    var transitionTime: TransitionTime = TransitionTime(rawValue = 0u)
        set(value) {
            field = value
            // Publishes the state change to the model.
            val _ = publish()
        }

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
            msg = event.request
        )

        // The status may be received here if the Generic Default Transition Time Server has been
        // configured to publish, or as a response. Nothing to do.
        is ModelEvent.ResponseReceived,
        is ModelEvent.UnacknowledgedMessageReceived,
        -> null
    }
}
