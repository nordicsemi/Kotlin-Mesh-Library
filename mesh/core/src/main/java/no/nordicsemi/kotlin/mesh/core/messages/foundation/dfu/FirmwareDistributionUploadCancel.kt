package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer

/**
 * Firmware Distribution Upload Cancel message is an acknowledged message sent by a Firmware
 * Distribution Client to stop a firmware image upload to a Firmware Distribution Server.
 */
class FirmwareDistributionUploadCancel : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = FirmwareDistributionUploadStatus.opCode
    override val parameters = null

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode = 0x8321u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { FirmwareDistributionUploadCancel() }
    }
}