package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.and
import no.nordicsemi.kotlin.data.getULong
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.ushr
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateAdditionalInformation
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePhase
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import java.nio.ByteOrder


/**
 * The Firmware Update Status message is an unacknowledged message sent by a Firmware Update Server
 * to report the status of a firmware update.
 *
 * Firmware Updates Status message is sent in response to [FirmwareUpdateGet] message,
 * a [FirmwareUpdateStart] message, [FirmwareUpdateCancel] message, or [FirmwareUpdateApply]
 * message.
 *
 * @property status                Status for the requesting message.
 * @property updatePhase           Update Phase state of the Firmware Update Server.
 * @property updateTtl             TTL value to use during firmware image transfer.
 * @property additionalInformation Firmware Update Additional Information state from the Firmware
 *                                 Update Server.
 * @property updateTimeoutBase     Update Server Timeout Base state is a [UShort] value that
 *                                 indicates the timeout after which the Firmware Update Server
 *                                 suspends firmware image transfer reception.
 *                                 The timeout is calculated as ``10 * (updateTimeoutBase + 1)``
 *                                 seconds.
 * @property blobId                BLOB identifier for the firmware image.
 * @property imageIndex            Index of the firmware image in the Firmware Information List
 *                                 state being updated.
 */
class FirmwareUpdateStatus(
    val status: FirmwareUpdateMessageStatus,
    val updatePhase: FirmwareUpdatePhase,
    val updateTtl: UByte?,
    val additionalInformation: FirmwareUpdateAdditionalInformation?,
    val updateTimeoutBase: UShort?,
    val blobId: ULong?,
    val imageIndex: UByte?,
) : MeshResponse {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray?
        get() {
            val byte0 = (status.value and 0x07u) or (updatePhase.value shl 5)
            val updateTtl = updateTtl ?: return byte0.toByteArray()
            val additionalInformation = additionalInformation ?: return byte0.toByteArray()
            val updateTimeoutBase = updateTimeoutBase ?: return byte0.toByteArray()
            val blobId = blobId ?: return byte0.toByteArray()
            val imageIndex = imageIndex ?: return byte0.toByteArray()
            val byte1 = updateTtl and 0x7F
            val byte2 = additionalInformation.value and 0x1F
            return byte0.toByteArray() +
                    byte1.toByteArray() +
                    byte2.toByteArray() +
                    updateTimeoutBase.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    blobId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    imageIndex.toByteArray()
        }

    /**
     * Creates a Firmware Update Status message with given parameters.
     *
     * @param status      Status for the requesting messa
     * @param updatePhase Update Phase state of the Firmware Update Server.
     */
    @Suppress("unused")
    constructor(status: FirmwareUpdateMessageStatus, updatePhase: FirmwareUpdatePhase) : this(
        status = status,
        updatePhase = updatePhase,
        updateTtl = null,
        additionalInformation = null,
        updateTimeoutBase = null,
        blobId = null,
        imageIndex = null
    )

    override fun toString() = "FirmwareUpdateStatus(status: $status, " +
            "updatePhase: $updatePhase, " +
            "updateTtl: ${updateTtl ?: "N/A"}, " +
            "additionalInformation: ${additionalInformation ?: "N/A"}, " +
            "updateTimeoutBase: ${updateTimeoutBase ?: "N/A"}, " +
            "blobId: ${blobId ?: "N/A"}, " +
            "imageIndex: ${imageIndex ?: "N/A"})"

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8310u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isNotEmpty() }
            ?.let { params ->
                val status = FirmwareUpdateMessageStatus.from(
                    value = (params[0] and 0x07).toUByte()
                ) ?: return@let null
                val phase = FirmwareUpdatePhase.from(
                    value = (params[0] ushr 5).toUByte()
                ) ?: return@let null

                if (params.size > 1) {
                    if (params.size == 14) {
                        FirmwareUpdateStatus(
                            status = status,
                            updatePhase = phase,
                            updateTtl = params[1].toUByte(),
                            additionalInformation = FirmwareUpdateAdditionalInformation.from(
                                value = (params[2] and 0x1F).toUByte()
                            ),
                            updateTimeoutBase = params.getUShort(
                                offset = 3,
                                order = ByteOrder.LITTLE_ENDIAN
                            ),
                            blobId = params.getULong(offset = 5, order = ByteOrder.LITTLE_ENDIAN),
                            imageIndex = params[13].toUByte()
                        )
                    } else return@let null
                } else {
                    FirmwareUpdateStatus(
                        status = status,
                        updatePhase = phase,
                        updateTtl = null,
                        additionalInformation = null,
                        updateTimeoutBase = null,
                        blobId = null,
                        imageIndex = null
                    )
                }
            }
    }
}