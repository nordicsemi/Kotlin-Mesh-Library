package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import java.nio.ByteOrder

/**
 * Firmware Distribution Firmware Get message is an acknowledged message sent by a Firmware
 * Distribution Client to check whether a specific firmware image is stored on a Firmware
 * Distribution Server.
 */
class FirmwareDistributionFirmwareGet(val firmwareId: FirmwareId) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = FirmwareDistributionFirmwareStatus.opCode
    override val parameters = firmwareId.bytes

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8323u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 2 }
            ?.let { params ->
                FirmwareDistributionFirmwareGet(
                    firmwareId = FirmwareId(
                        companyIdentifier = params.getUShort(
                            offset = 0,
                            order = ByteOrder.LITTLE_ENDIAN
                        ),
                        version = params.copyOfRange(2, params.size)
                    )
                )
            }
    }
}