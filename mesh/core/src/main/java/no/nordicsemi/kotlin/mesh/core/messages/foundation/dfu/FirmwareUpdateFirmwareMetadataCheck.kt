package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer


/**
 * Firmware Update Firmware Metadata Check message is an acknowledged message, sent to a Firmware
 * Update Server, to check whether the Node can accept a firmware update.
 *
 * @property imageIndex Index of the firmware image in the Firmware Information List state to check.
 *                      Update Firmware Image Index field shall identify the firmware image in the
 *                      Firmware Information List state on the Firmware Update Server that the
 *                      metadata is checked against.
 * @property metaData   Vendor-specific metadata. If present, the Incoming Firmware Metadata field
 *                      shall contain the custom data from the firmware vendor. The firmware
 *                      metadata can be used to check whether the installed firmware image
 *                      identified by the Firmware Image Index field will accept an update based on
 *                      the metadata provided for the new firmware image.
 *
 *                      Maximum size is 255 bytes. Metadata should be omitted it empty.
 */
class FirmwareUpdateFirmwareMetadataCheck(
    val imageIndex: UByte,
    val metaData: ByteArray?,
) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = FirmwareUpdateStatus.opCode
    override val parameters = imageIndex.toByteArray() + (metaData ?: byteArrayOf())

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x830Au

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isNotEmpty() }
            ?.let { params ->
                FirmwareUpdateFirmwareMetadataCheck(
                    imageIndex = params[0].toUByte(),
                    metaData = if (params.size > 1)
                        params.copyOfRange(fromIndex = 1, toIndex = params.size)
                    else null
                )
            }
    }
}