package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer

/**
 * The Firmware Distribution Capabilities Get message is an acknowledged message sent by a Firmware
 * Distribution Client to get the capabilities of a Firmware Distribution Server.
 */
class FirmwareDistributionCapabilitiesGet : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = FirmwareDistributionCapabilitiesStatus.opCode
    override val parameters = null

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode = 0x8316u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { FirmwareDistributionCapabilitiesGet() }
    }
}