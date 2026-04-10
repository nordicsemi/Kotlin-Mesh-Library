@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionPhase
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import kotlin.experimental.and

/**
 * Firmware Distribution Upload Status message is an unacknowledged message sent by a Firmware
 * Distribution Server to report the status of a firmware image upload.
 *
 * A Firmware Distribution Upload Status message is sent as a response to any of the following
 * messages [FirmwareDistributionUploadGet], [FirmwareDistributionUploadStart],
 * [FirmwareDistributionUploadOOBStart], [FirmwareDistributionCancel],
 */
class FirmwareDistributionUploadStatus(
    override val status: FirmwareDistributionMessageStatus,
    val phase: FirmwareDistributionPhase,
    val progress: UByte?,
    val isOob: Boolean?,
    val firmwareId: FirmwareId?,
) : MeshResponse, FirmwareDistributionStatusMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray
        get() {
            val initial = status.value.toByteArray() + phase.value.toByteArray()
            return if (progress != null && isOob != null && firmwareId != null) {
                initial + progress.toByteArray() +
                        when {
                            isOob -> 0x80.toByteArray()
                            else -> 0x00.toByteArray()
                        } + firmwareId.bytes
            } else {
                initial
            }
        }

    /**
     * Convenience constructor to create a FirmwareDistributionUploadStatus message.
     *
     * @param status   Status of the firmware image distribution.
     */
    constructor(status: FirmwareDistributionMessageStatus) : this(
        status = status,
        phase = FirmwareDistributionPhase.IDLE,
        progress = null,
        isOob = null,
        firmwareId = null
    )

    /**
     * Convenience constructor to create a FirmwareDistributionUploadStatus message.
     *
     * @param status           Status of the firmware image distribution.
     * @param phase            Phase of the firmware image distribution.
     * @param uploadProgress   Progress of the firmware image upload.
     * @param isOob            Whether the firmware image is out-of-band.
     * @param firmwareId       Firmware ID
     *
     * @throws IllegalArgumentException If [uploadProgress] is greater than 127.
     */
    constructor(
        status: FirmwareDistributionMessageStatus,
        phase: FirmwareDistributionPhase,
        uploadProgress: UByte,
        isOob: Boolean,
        firmwareId: FirmwareId,
    ) : this(
        status = status,
        phase = phase,
        progress = uploadProgress,
        isOob = isOob,
        firmwareId = firmwareId
    )

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8322u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 2 || parameters.size >= 5
        }?.let { params ->
            FirmwareDistributionMessageStatus.from(value = params[0].toUByte())?.let { status ->
                val phase =
                    FirmwareDistributionPhase.from(value = params[1].toUByte()) ?: return null
                if (params.size >= 5) {
                    val progress = params[2].toUByte() and 0x7Fu
                    val isOob = (params[2].toInt() and 0x80) != 0
                    val firmwareId = params.copyOfRange(4, params.size).takeIf { it.isNotEmpty() }
                        ?.let { FirmwareId(it) }
                    FirmwareDistributionUploadStatus(
                        status = status,
                        phase = phase,
                        progress = progress,
                        isOob = isOob,
                        firmwareId = firmwareId
                    )
                } else {
                    FirmwareDistributionUploadStatus(
                        status = status,
                        phase = phase,
                        progress = null,
                        isOob = null,
                        firmwareId = null
                    )
                }
            }
        }

    }
}