package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import java.nio.ByteOrder

/**
 * The Firmware Distribution Firmware Get By Index message is an acknowledged message sent by a
 * Firmware Distribution Client to check which firmware image is stored in a particular entry in the
 * Firmware Images List state on a Firmware Distribution Server.
 *
 * @property imageIndex Index of the firmware image.
 */
class FirmwareDistributionFirmwareGetByIndex(val imageIndex: UShort) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = FirmwareDistributionFirmwareStatus.opCode
    override val parameters = imageIndex.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8324u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { params ->
                FirmwareDistributionFirmwareGetByIndex(
                    imageIndex = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                )
            }
    }
}