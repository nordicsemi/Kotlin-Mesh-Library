package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer

/**
 * Firmware Distribution Upload Get message is an acknowledged message sent by a Firmware
 * Distribution Client to check the status of a firmware image upload to a Firmware Distribution
 * Server.
 */
class FirmwareDistributionUploadGet : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = FirmwareDistributionUploadStatus.opCode
    override val parameters = null

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode = 0x831Eu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { FirmwareDistributionUploadGet() }
    }
}