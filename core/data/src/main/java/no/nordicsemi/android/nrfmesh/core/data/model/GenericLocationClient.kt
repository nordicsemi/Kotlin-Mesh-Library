package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalStatus

/**
 * Handles the Generic Location Client model. It allows the [GenericLocationGlobalStatus] and
 * [GenericLocationLocalStatus] to be decoded and delivered as responses.
 */
class GenericLocationClient : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        GenericLocationGlobalStatus.opCode to GenericLocationGlobalStatus.Initializer,
        GenericLocationLocalStatus.opCode to GenericLocationLocalStatus.Initializer,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer? = null

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
            msg = event.request
        )

        // Responses are delivered to the caller; status messages published by a server are ignored.
        is ModelEvent.ResponseReceived,
        is ModelEvent.UnacknowledgedMessageReceived,
        -> null
    }
}
