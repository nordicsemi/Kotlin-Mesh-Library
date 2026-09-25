package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import java.net.URI
import java.net.URL
import java.nio.ByteOrder

/**
 * Firmware Distribution Upload OOB Start message is an acknowledged message sent by a Firmware
 * Distribution Client to start a firmware image upload to a Firmware Distribution Server using an
 * Out of Band (OOB) mechanism.
 *
 * @property url               URI for the firmware image check and retrieval. The URI shall point
 *                             to a location where the Distributor can check for a newer firmware
 *                             image and its retrieval. Maximum length of the URI is 255 bytes
 *                             using UTF-8 encoding.
 * @property currentFirmwareId Current Firmware ID of a Target Node(s). The Firmware ID, together
 *                             with the URI, will be used to check whether there is a newer firmware
 *                             for the device. The result will be returned using the
 *                             [FirmwareDistributionUploadStatus] message. If the URI scheme is
 *                             [https://], the FWID will be appended to the URI
 *                             using [/check?cfwid=<FWID as hex>] to check the availability of a new
 *                             firmware image over HTTP and [/get?cfwid=<FWID as hex>] to retrieve
 *                             the firmware.
 */
class FirmwareDistributionUploadOOBStart(
    val url: URL,
    val currentFirmwareId: FirmwareId,
) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = FirmwareDistributionUploadStatus.opCode
    override val parameters = url.toString().encodeToByteArray() + currentFirmwareId.bytes

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode = 0x8320u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 4 }
            ?.let { params ->
                val uriLength = params[0].toInt()
                if (uriLength >= 1 && params.count() >= (3 + uriLength)) {
                    val urlString =
                        params.decodeToString(startIndex = 1, endIndex = (uriLength + 1))
                    val uri = URI(urlString)
                    val url = uri.toURL()
                    val companyIdentifier =
                        params.getUShort(offset = 1 + uriLength, order = ByteOrder.LITTLE_ENDIAN)
                    FirmwareDistributionUploadOOBStart(
                        url = url,
                        currentFirmwareId = if (params.size > 3 + uriLength) {
                            FirmwareId(
                                companyIdentifier = companyIdentifier,
                                version = params.copyOfRange(
                                    fromIndex = 3 + uriLength,
                                    toIndex = params.size
                                )
                            )
                        } else {
                            FirmwareId(companyIdentifier = companyIdentifier)
                        }
                    )
                } else null
            }
    }
}