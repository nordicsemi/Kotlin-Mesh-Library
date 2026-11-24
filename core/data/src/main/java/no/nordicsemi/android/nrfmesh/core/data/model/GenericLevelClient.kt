package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelStatus

/**
 * Handles the Generic Level Client model events, allowing Generic Level Status messages to be
 * decoded.
 */
class GenericLevelClient : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        GenericLevelStatus.opCode to GenericLevelStatus.Initializer,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer
        get() = { GenericLevelSetUnacknowledged(tid = null, level = state) }

    /**
     * The level that is published to the network when set. The level wraps around when it is
     * incremented or decremented beyond its limits.
     */
    var state: Short = Short.MIN_VALUE
        set(value) {
            field = value
            // Publishes the state change to the model.
            val _ = publish()
        }

    companion object {
        /** Amount by which the level is incremented or decremented in a single step. */
        const val LEVEL_STEP = 8192

        /**
         * Returns the level after a single step, wrapping around when the limits of a 16-bit
         * signed level are exceeded, like the iOS app does with `&+` and `&-`.
         */
        fun nextLevel(current: Short, increase: Boolean): Short =
            (current + if (increase) LEVEL_STEP else -LEVEL_STEP).toShort()
    }

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
            msg = event.request
        )

        // Responses are delivered to the caller; status messages published by a server are ignored.
        is ModelEvent.ResponseReceived,
        is ModelEvent.UnacknowledgedMessageReceived -> null
    }
}
