package no.nordicsemi.kotlin.mesh.core.layers.foundation

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateFirmwareMetadataStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateInformationStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateStatus

/**
 * Firmware Update Client Model Handler is responsible for handling the incoming messages for
 * the Firmware Update Client Model.
 */
class FirmwareUpdateClientHandler : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        FirmwareUpdateInformationStatus.opCode to FirmwareUpdateInformationStatus,
        FirmwareUpdateFirmwareMetadataStatus.opCode to FirmwareUpdateFirmwareMetadataStatus,
        FirmwareUpdateStatus.opCode to FirmwareUpdateStatus
    )
    override val isSubscriptionSupported: Boolean = false
    override val publicationMessageComposer: MessageComposer? = null

    override suspend fun handle(event: ModelEvent): MeshResponse? {
        when (event) {
            is ModelEvent.AcknowledgedMessageReceived -> {
                // No acknowledged message supported by this Model.
            }

            is ModelEvent.ResponseReceived -> {
                // Ignore do nothing. There are no CDB fields matching these parameters.
            }

            is ModelEvent.UnacknowledgedMessageReceived -> {
                // Ignore do nothing
            }
        }
        return null
    }
}