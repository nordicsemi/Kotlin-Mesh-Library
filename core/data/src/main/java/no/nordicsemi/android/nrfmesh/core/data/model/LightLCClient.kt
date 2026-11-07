package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCLightOnOffStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCModeStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCOccupancyModeStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCPropertyStatus

/**
 * Handles the events of the Light LC Client model.
 *
 * Allows the Light LC Status messages ([LightLCModeStatus], [LightLCOccupancyModeStatus],
 * [LightLCLightOnOffStatus] and [LightLCPropertyStatus]) to be decoded. Responses are delivered to
 * the caller, while status messages published by a server are ignored.
 */
class LightLCClient : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        LightLCModeStatus.opCode to LightLCModeStatus.Initializer,
        LightLCOccupancyModeStatus.opCode to LightLCOccupancyModeStatus.Initializer,
        LightLCLightOnOffStatus.opCode to LightLCLightOnOffStatus.Initializer,
        LightLCPropertyStatus.opCode to LightLCPropertyStatus.Initializer,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer? = null

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> throw ModelError.InvalidMessage(
            msg = event.request
        )

        // Responses are delivered to the caller; status messages published by a server are ignored.
        is ModelEvent.ResponseReceived,
        is ModelEvent.UnacknowledgedMessageReceived -> null
    }
}
