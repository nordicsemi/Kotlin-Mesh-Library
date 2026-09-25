package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import java.nio.ByteOrder

/**
 * Firmware Distribution Firmware Delete message is an acknowledged message sent by a Firmware
 * Distribution Client to delete a stored firmware image on a Firmware Distribution Server.
 *
 * @property firmwareId Firmware ID.
 */
class FirmwareDistributionFirmwareDelete(val firmwareId: FirmwareId) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = FirmwareDistributionFirmwareStatus.opCode
    override val parameters = firmwareId.bytes

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8325u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 2 }
            ?.let { params ->
                FirmwareDistributionFirmwareDelete(
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