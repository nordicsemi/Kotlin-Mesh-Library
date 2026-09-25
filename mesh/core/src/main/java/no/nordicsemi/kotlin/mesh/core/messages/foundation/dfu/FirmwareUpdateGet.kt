package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer


/**
 * Firmware Update Get message is an acknowledged message used to get the current status of the
 * Firmware Update Server.
 */
class FirmwareUpdateGet : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = FirmwareUpdateStatus.opCode
    override val parameters = null

    override fun toString() = "FirmwareUpdateGet()"

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x830Cu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { FirmwareUpdateGet() }
    }
}