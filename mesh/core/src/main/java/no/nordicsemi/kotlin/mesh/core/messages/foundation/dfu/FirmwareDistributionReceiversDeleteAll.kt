package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import java.nio.ByteOrder

/**
 * Firmware Distribution Receivers Delete All message is an acknowledged message sent by a
 * Firmware Distribution Client to remove all entries from the Distribution Receivers List state of
 * a Firmware Distribution Server.
 *
 * @property firstIndex   Index of the first firmware image.
 * @property entriesLimit Number of firmware images.
 */
class FirmwareDistributionReceiversDeleteAll(
    val firstIndex: UShort,
    val entriesLimit: UShort
) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = FirmwareDistributionReceiversList.opCode
    override val parameters = firstIndex.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            entriesLimit.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8312u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 4 }
            ?.let { params ->
                FirmwareDistributionReceiversDeleteAll(
                    firstIndex = params.getUShort(
                        offset = 0,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    entriesLimit = params.getUShort(
                        offset = 2,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                )
            }
    }
}