package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer


/**
 * The Firmware Update Cancel message is an acknowledged message used to
 * cancel a firmware update and delete any stored information about the update
 * on a Firmware Update Server.
 */
class FirmwareUpdateCancel : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = FirmwareUpdateStatus.opCode
    override val parameters = null

    override fun toString() = "FirmwareUpdateCancel()"

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x830Eu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { FirmwareUpdateCancel() }
    }
}