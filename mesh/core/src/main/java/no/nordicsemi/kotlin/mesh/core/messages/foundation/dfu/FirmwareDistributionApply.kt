package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer

/**
 * Firmware Distribution Apply message is an acknowledged message sent from a Firmware Distribution
 * Client to a Firmware Distribution Server to apply the firmware image on the Target Nodes.
 */
class FirmwareDistributionApply : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = FirmwareDistributionStatus.opCode
    override val parameters = null

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode = 0x831Cu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { FirmwareDistributionApply() }
    }
}