@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import java.nio.ByteOrder

/**
 * Firmware Distribution Firmware Status message is an unacknowledged message sent by a Firmware
 * Distribution Server to report the status of an operation on a stored firmware image.
 *
 * Firmware Distribution Firmware Status message is sent in response to any of:
 * [FirmwareDistributionFirmwareGet], [FirmwareDistributionFirmwareGetByIndex],
 * [FirmwareDistributionFirmwareDelete], [FirmwareDistributionFirmwareDeleteAll] message.
 *
 * @property entryCount Number of firmware images stored on the Firmware Distribution Server.
 * @property imageIndex Index of the firmware image.
 * @property firmwareId Firmware ID.
 */
class FirmwareDistributionFirmwareStatus(
    override val status: FirmwareDistributionMessageStatus,
    val entryCount: UShort,
    val imageIndex: UShort?,
    val firmwareId: FirmwareId?,
) : MeshResponse, FirmwareDistributionStatusMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray
        get() = status.value.toByteArray() +
                entryCount.toByteArray(order = java.nio.ByteOrder.LITTLE_ENDIAN) +
                (imageIndex ?: 0xFFFFu).toByteArray(order = java.nio.ByteOrder.LITTLE_ENDIAN) +
                (firmwareId?.bytes ?: byteArrayOf())

    /**
     * Convenience constructor to create a FirmwareDistributionFirmwareStatus message.
     *
     * @param request           [FirmwareDistributionFirmwareGet] message.
     * @param firmwareImages    List of firmware images.
     */
    constructor(
        request: FirmwareDistributionFirmwareGet,
        firmwareImages: List<FirmwareId>,
    ) : this(
        status = FirmwareDistributionMessageStatus.SUCCESS,
        entryCount = firmwareImages.size.toUShort(),
        imageIndex = firmwareImages
            .indexOfFirst { it == request.firmwareId }
            .takeIf { it > -1 }
            ?.toUShort(),
        firmwareId = firmwareImages
            .firstOrNull { it == request.firmwareId }
            ?.let { request.firmwareId }
    )

    /**
     * Convenience constructor to create a FirmwareDistributionFirmwareStatus message.
     *
     * @param request           [FirmwareDistributionFirmwareGetByIndex] message.
     * @param firmwareImages    List of firmware images.
     */
    constructor(
        request: FirmwareDistributionFirmwareGetByIndex,
        firmwareImages: List<FirmwareId>,
    ) : this(
        status = FirmwareDistributionMessageStatus.SUCCESS,
        entryCount = firmwareImages.size.toUShort(),
        imageIndex = request.imageIndex,
        firmwareId = when (firmwareImages.size > request.imageIndex.toInt()) {
            true -> firmwareImages[request.imageIndex.toInt()]
            else -> null
        }
    )

    /**
     * Convenience constructor to create a [FirmwareDistributionFirmwareStatus] message.
     *
     * @param request           [FirmwareDistributionFirmwareDelete] message.
     * @param firmwareImages    List of firmware images.
     */
    constructor(
        request: FirmwareDistributionFirmwareDelete,
        firmwareImages: List<FirmwareId>,
    ) : this(
        status = FirmwareDistributionMessageStatus.SUCCESS,
        entryCount = firmwareImages.size.toUShort(),
        imageIndex = null,
        firmwareId = request.firmwareId
    )

    /**
     * Convenience constructor to create a FirmwareDistributionFirmwareStatus message.
     *
     * @param request           [FirmwareDistributionFirmwareDeleteAll] message.
     * @param firmwareImages    List of firmware images.
     */
    constructor(
        request: FirmwareDistributionFirmwareDeleteAll,
        firmwareImages: List<FirmwareId>,
    ) : this(
        status = FirmwareDistributionMessageStatus.SUCCESS,
        entryCount = 0u,
        imageIndex = null,
        firmwareId = null
    )

    /**
     * Convenience constructor to create a FirmwareDistributionFirmwareStatus message.
     *
     * @param status            [FirmwareDistributionFirmwareStatus] message.
     * @param firmwareImages    List of firmware images.
     */
    constructor(
        status: FirmwareDistributionMessageStatus,
        firmwareImages: List<FirmwareId>,
    ) : this(
        status = status,
        entryCount = firmwareImages.size.toUShort(),
        imageIndex = null,
        firmwareId = null
    )

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8319u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 5 || it.size >= 7 }
            ?.let { params ->
                FirmwareDistributionMessageStatus.from(value = params[0].toUByte())?.let { status ->
                    FirmwareDistributionFirmwareStatus(
                        status = status,
                        entryCount = params.getUShort(offset = 1, order = ByteOrder.LITTLE_ENDIAN),
                        imageIndex = params.getUShort(offset = 3, order = ByteOrder.LITTLE_ENDIAN)
                            .let { imageIndex ->
                                when (params.getUShort(
                                    offset = 3,
                                    order = ByteOrder.LITTLE_ENDIAN
                                ) == 0xFFFF.toUShort()) {
                                    true -> null
                                    else -> imageIndex
                                }
                            },
                        firmwareId = when (parameters.size) {
                            5 -> null
                            else -> {
                                val companyIdentifier: UShort = params
                                    .getUShort(offset = 5, order = ByteOrder.LITTLE_ENDIAN)
                                when (parameters.size) {
                                    7 ->
                                        FirmwareId(
                                            companyIdentifier = params
                                                .getUShort(offset = 5, order = ByteOrder.LITTLE_ENDIAN),
                                            version = byteArrayOf()
                                        )
                                    else ->
                                        FirmwareId(
                                            companyIdentifier = companyIdentifier,
                                            version = parameters.copyOfRange(7, parameters.size)
                                        )
                                }
                            }
                        }
                    )
                }
            }
    }
}