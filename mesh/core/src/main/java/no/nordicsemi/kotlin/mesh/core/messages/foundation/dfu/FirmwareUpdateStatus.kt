package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateAdditionalInformation
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePhase
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse

class FirmwareUpdateStatus(
    val status: FirmwareUpdateMessageStatus,
    val updatePhase: FirmwareUpdatePhase,
    val updateTtl: UByte?,
    val additionalInformation: FirmwareUpdateAdditionalInformation?,
) : MeshResponse {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray? = null

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8310u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let {
                TODO("FirmwareUpdateStatus")
            }
    }
}