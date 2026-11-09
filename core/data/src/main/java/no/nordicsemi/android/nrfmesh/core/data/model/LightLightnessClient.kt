package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLastStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLinearStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessRangeStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessStatus

/**
 * Handles the events of the Light Lightness Client model.
 *
 * Allows the Light Lightness Status messages ([LightLightnessStatus], [LightLightnessLinearStatus],
 * [LightLightnessLastStatus], [LightLightnessDefaultStatus] and [LightLightnessRangeStatus]) to be
 * decoded. Responses are delivered to the caller, while status messages published by a server are
 * ignored.
 */
class LightLightnessClient : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        LightLightnessStatus.opCode to LightLightnessStatus.Initializer,
        LightLightnessLinearStatus.opCode to LightLightnessLinearStatus.Initializer,
        LightLightnessLastStatus.opCode to LightLightnessLastStatus.Initializer,
        LightLightnessDefaultStatus.opCode to LightLightnessDefaultStatus.Initializer,
        LightLightnessRangeStatus.opCode to LightLightnessRangeStatus.Initializer,
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
