package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer

/**
 * Firmware Distribution Firmware Delete All message is an acknowledged message sent by a Firmware
 * Distribution Client to delete all firmware images on a Firmware Distribution Server.
 */
class FirmwareDistributionFirmwareDeleteAll : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = FirmwareDistributionFirmwareStatus.opCode
    override val parameters = null

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8326u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty()}
            ?.let { _ -> FirmwareDistributionFirmwareDeleteAll() }
    }
}