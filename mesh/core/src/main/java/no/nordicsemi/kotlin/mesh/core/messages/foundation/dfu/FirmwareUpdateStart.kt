package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getULong
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import java.nio.ByteOrder


/**
 * Firmware Update Start message is an acknowledged message used to start a firmware update on a
 * Firmware Update Server.
 *
 * @property updateTtl             Time To Live (TTL) value to use during firmware image transfer.
 *                                 Valid value is in range 0...127 or 255 (0xFF) to use the default
 *                                 TTL value. Values 128-254 are prohibited.
 * @property updateTimeoutBase     Update Server Timeout Base state is a `UShort` value that
 *                                 indicates the timeout after which the Firmware Update Server
 *                                 suspends firmware image transfer reception. Timeout is
 *                                 calculated as `10 * (updateTimeoutBase + 1)` seconds.
 * @property blobId                BLOB identifier for the firmware image. BLOB ID state is an
 *                                 `ULong` value that uniquely identifies a BLOB on a network.
 * @property imageIndex            Index of the firmware image in the Firmware Information List
 *                                 state to be updated.
 * @property metaData              Vendor-specific firmware metadata. If present, the Incoming
 *                                 Firmware Metadata field contains the custom data from the
 *                                 firmware vendor that is used to check whether the firmware image
 *                                 can be updated. Maximum size is 255 bytes. Metadata should be
 *                                 omitted it empty.
 */
class FirmwareUpdateStart(
    val updateTtl: UByte,
    val updateTimeoutBase: UShort,
    val blobId: ULong,
    val imageIndex: UByte,
    val metaData: ByteArray?,
) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = FirmwareUpdateStatus.opCode
    override val parameters: ByteArray
        get() = updateTtl.toByteArray() +
                updateTimeoutBase.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                blobId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                imageIndex.toByteArray() +
                (metaData ?: byteArrayOf())

    override fun toString() = "FirmwareUpdateStart(updateTtl: $updateTtl, " +
            "updateTimeoutBase: $updateTimeoutBase, blobId: $blobId, imageIndex: $imageIndex, " +
            "metaData: ${metaData?.contentToString() ?: "null"})"

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x830Du

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { params ->
                FirmwareUpdateStart(
                    updateTtl = params[0].toUByte(),
                    updateTimeoutBase = params.getUShort(
                        offset = 1,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    blobId = params.getULong(offset = 3, order = ByteOrder.LITTLE_ENDIAN),
                    imageIndex = params[11].toUByte(),
                    metaData = if (params.size > 12) params.copyOfRange(
                        fromIndex = 12, toIndex = params.size
                    ) else null
                )
            }
    }
}