package no.nordicsemi.kotlin.mesh.core.layers.foundation

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionReceiversList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionReceiversStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionUploadStatus

/**
 * Firmware Distribution Client Model Handler is responsible for handling the incoming messages for
 * the Firmware Distribution Client Model.
 */
class FirmwareDistributionClientHandler : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        FirmwareDistributionReceiversStatus.opCode to FirmwareDistributionReceiversStatus,
        FirmwareDistributionReceiversList.opCode to FirmwareDistributionReceiversList,
        FirmwareDistributionCapabilitiesStatus.opCode to FirmwareDistributionCapabilitiesStatus,
        FirmwareDistributionStatus.opCode to FirmwareDistributionStatus,
        FirmwareDistributionUploadStatus.opCode to FirmwareDistributionUploadStatus,
        FirmwareDistributionFirmwareStatus.opCode to FirmwareDistributionFirmwareStatus
    )
    override val isSubscriptionSupported: Boolean = false
    override val publicationMessageComposer: MessageComposer? = null

    override suspend fun handle(event: ModelEvent): MeshResponse? {
        when (event) {
            is ModelEvent.AcknowledgedMessageReceived -> {
                // No acknowledged message supported by this Model.
                throw ModelError.InvalidMessage(
                    msg = event.request
                )
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