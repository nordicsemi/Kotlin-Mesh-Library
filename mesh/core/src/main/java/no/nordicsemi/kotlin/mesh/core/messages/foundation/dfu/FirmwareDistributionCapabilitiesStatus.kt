package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getUInt
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.model.UriScheme
import java.nio.ByteOrder
import kotlin.collections.emptyList

/**
 * Firmware Distribution Capabilities Status message is an unacknowledged message sent by a Firmware
 * Distribution Server to report Distributor capabilities.
 *
 * This message is sent as a response to a [FirmwareDistributionCapabilitiesGet] message.
 *
 * @property maxReceiversCount          Maximum number of entries in the Distribution Receivers List
 *                                      state.
 * @property maxFirmwareImagesListSize  Maximum number of entries in the Firmware Images List state.
 * @property maxFirmwareImageSize       Maximum size of a firmware image in octets.
 * @property maxUploadSpace             Total space dedicated to storage of firmware images in
 *                                      octets.
 * @property remainingUploadSpace       Remaining available space in firmware image storage in octets.
 * @property supportedUriSchemes        Supported Out-of-Band URI schemes. An empty array means,
 *                                      OOB Retrieval is not supported.
 */
class FirmwareDistributionCapabilitiesStatus(
    val maxReceiversCount: UShort,
    val maxFirmwareImagesListSize: UShort,
    val maxFirmwareImageSize: UShort,
    val maxUploadSpace: UInt,
    val remainingUploadSpace: UInt,
    val supportedUriSchemes: List<UriScheme>,
) : MeshResponse {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray
        get() {
            val data = maxReceiversCount.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    maxFirmwareImagesListSize.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    maxFirmwareImageSize.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    maxUploadSpace.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    remainingUploadSpace.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

            return when (supportedUriSchemes.isEmpty()) {
                true -> data + 0x00u.toByteArray()
                false -> data + supportedUriSchemes.fold(initial = 0x01u.toByteArray()) { acc, uri ->
                    acc + uri.rawValue.toByteArray()
                }
            }
        }

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8317u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size >= 17
        }?.let { params ->
            params
            FirmwareDistributionCapabilitiesStatus(
                maxReceiversCount = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                maxFirmwareImagesListSize = params.getUShort(
                    offset = 2,
                    order = ByteOrder.LITTLE_ENDIAN
                ),
                maxFirmwareImageSize = params.getUShort(
                    offset = 4,
                    order = ByteOrder.LITTLE_ENDIAN
                ),
                maxUploadSpace = params.getUInt(offset = 8, order = ByteOrder.LITTLE_ENDIAN),
                remainingUploadSpace = params.getUInt(offset = 12, order = ByteOrder.LITTLE_ENDIAN),
                supportedUriSchemes = when (params[16].toUByte() == 0x01.toUByte()) {
                    true -> if (params.size >= 18) {
                        params
                            .slice(indices = 17 until parameters.size)
                            .mapNotNull { UriScheme.from(rawValue = it.toUByte()) }
                    } else return@let null

                    else -> emptyList()
                }
            )
        }
    }
}