package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.ushr
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateAdditionalInformation
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import kotlin.experimental.and


/**
 * Firmware Update Firmware Metadata Status message is an unacknowledged message sent to a Firmware
 * Update Client that is used to report whether a Firmware Update Server can accept a firmware
 * update.
 *
 * The Firmware Update Firmware Metadata Status message is sent in response to a
 * [FirmwareUpdateFirmwareMetadataCheck] message.
 */
class FirmwareUpdateFirmwareMetadataStatus(
    val status: FirmwareUpdateMessageStatus,
    val additionalInformation: FirmwareUpdateAdditionalInformation,
    val imageIndex: UByte,
) : MeshResponse {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray
        get() {
            val byte0 = status.value and 0x07u or (additionalInformation.value shl 3)
            return byte0.toByteArray() + imageIndex.toByteArray()
        }

    /**
     * Convenience constructor to create a Firmware Update Firmware Metadata Status message.
     *
     * @param request               Firmware Update Firmware Metadata Check message to response to.
     * @param status                Status from the firmware metadata check. This should be one of
     *                              [FirmwareUpdateMessageStatus/success],
     *                              [FirmwareUpdateMessageStatus/metadataCheckFailed], or
     *                              [FirmwareUpdateMessageStatus/wrongFirmwareIndex].
     * @param additionalInformation Firmware Update Additional Information state from the Firmware
     *                              Update Server.
     */
    @Suppress("unused")
    constructor(
        request: FirmwareUpdateFirmwareMetadataCheck,
        status: FirmwareUpdateMessageStatus,
        additionalInformation: FirmwareUpdateAdditionalInformation,
    ) : this(
        status = status,
        additionalInformation = additionalInformation,
        imageIndex = request.imageIndex
    )

    override fun toString() = "FirmwareUpdateFirmwareMetadataStatus(status: $status, " +
            "additionalInformation: $additionalInformation, " +
            "imageIndex: $imageIndex)"

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x830Bu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { params ->
                FirmwareUpdateFirmwareMetadataStatus(
                    status = FirmwareUpdateMessageStatus.from(
                        value = (params[0] and 0x07).toUByte()
                    ) ?: return@let null,
                    additionalInformation = FirmwareUpdateAdditionalInformation.from(
                        value = (params[0] ushr 3).toUByte()
                    ) ?: return@let null,
                    imageIndex = params[1].toUByte()
                )
            }
    }
}