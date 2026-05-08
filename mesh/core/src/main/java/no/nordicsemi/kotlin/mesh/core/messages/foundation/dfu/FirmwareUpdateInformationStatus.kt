package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareInformation
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import java.net.URI
import java.nio.ByteOrder


/**
 * Firmware Update Information Get message is an acknowledged message used to get information about
 * the firmware images installed on a Node.
 * @property list         Total number of entries in the Firmware Information List state.
 * @property firstIndex   Index of the first requested entry from the Firmware Information List
 *                         state.
 * @property totalCount   Total number of entries in the Firmware Information List State.
 */
class FirmwareUpdateInformationStatus(
    val list: List<FirmwareInformation>,
    val firstIndex: UByte,
    val totalCount: UByte,
) : MeshResponse {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray
        get() {
            var data = totalCount.toByteArray() + firstIndex.toByteArray()
            list.forEach {
                val idLength = (it.currentFirmwareId.version.size + 2).toByteArray()
                val uriLength = it.updateUri?.toString()?.toByteArray(charset = Charsets.UTF_8)?.size?.toByteArray() ?: 0.toByteArray()
                val udiData = it.updateUri?.toString()?.toByteArray(charset = Charsets.UTF_8) ?: byteArrayOf()
                data += idLength + it.currentFirmwareId.version + uriLength + udiData

            }
            return data
        }

    /**
     * Creates the Firmware Update Information Get message. This convenience constructor will only
     * request the total count of entries in the Firmware Information List state.
     */
    @Suppress("unused")
    constructor(
        request: FirmwareUpdateInformationGet,
        list: List<FirmwareInformation>,
    ) : this(
        list = list,
        firstIndex = request.firstIndex,
        totalCount = list.size.toUByte()
    )

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8309u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 2 }
            ?.let { params ->
                val firmwareList = mutableListOf<FirmwareInformation>()

                var offset = 2

                while (offset < params.size) {

                    // Decode Firmware ID
                    val currentFirmwareIdLength = params[offset].toInt()
                    offset += 1

                    require(
                        currentFirmwareIdLength >= 2 &&
                                currentFirmwareIdLength <= 2 + 106 &&
                                params.size >= offset + currentFirmwareIdLength
                    ) {
                        return@let null
                    }

                    val cid = params.getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)

                    val version = params.copyOfRange(
                        fromIndex = offset + 2,
                        toIndex = offset + currentFirmwareIdLength
                    )

                    val currentFirmwareId = FirmwareId(companyIdentifier = cid, version = version)

                    offset += currentFirmwareIdLength

                    // Decode Update URI
                    require(params.size >= offset + 1)

                    val updateUriLength = params[offset].toInt() and 0xFF
                    offset += 1

                    require(updateUriLength >= 0 && params.size >= offset + updateUriLength) {
                        return@let null
                    }

                    var updateUri: String? = null

                    if (updateUriLength > 0) {
                        updateUri = params
                            .copyOfRange(fromIndex = offset, toIndex = offset + updateUriLength)
                            .toString(Charsets.UTF_8)
                    }

                    offset += updateUriLength

                    val entry = FirmwareInformation(
                        currentFirmwareId = currentFirmwareId,
                        updateUri = updateUri
                            ?.let { URI.create(it).toURL() }
                    )
                    firmwareList.add(entry)
                }
                FirmwareUpdateInformationStatus(
                    firstIndex = params[0].toUByte(),
                    totalCount = params[1].toUByte(),
                    list = firmwareList.toList(),
                )
            }
    }
}