@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.provisioning

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.mesh.core.oob.OobInformation
import no.nordicsemi.kotlin.mesh.core.util.Utils
import kotlin.uuid.Uuid

data class UnprovisionedDevice(
    var name: String,
    val uuid: Uuid,
    val oobInformation: OobInformation = OobInformation.None
) {

    companion object {
        private const val FLAGS = 0x01
        private const val COMPLETE_LIST_16_BIT_UUID = 0x03
        private const val SERVICE_DATA_16_BIT_UUID = 0x16
        private const val COMPLETE_LOCAL_NAME = 0x09

        /**
         * Creates an UnprovisionedDevice from the advertisement data.
         *
         * @param advertisementData The advertisement data.
         * @return UnprovisionedDevice from the given advertisement data.
         */
        @OptIn(ExperimentalStdlibApi::class)
        fun from(advertisementData: ByteArray): UnprovisionedDevice {
            var length: Int
            var type: Int
            var deviceUuid: Uuid? = null
            var oobInformation: OobInformation = OobInformation.Other
            var localName = ""
            var i = 0
            while (i < advertisementData.size) {
                length = advertisementData[i++].toInt()
                if (length == 0) break
                type = advertisementData[i++].toInt()
                when (type) {
                    FLAGS, COMPLETE_LIST_16_BIT_UUID -> {
                        /* Do nothing */
                    }
                    SERVICE_DATA_16_BIT_UUID -> {
                        val deviceUuidIndex = i + 2
                        val hexUuid = advertisementData
                            .copyOfRange(deviceUuidIndex, deviceUuidIndex + 16)
                            .toHexString()
                        deviceUuid = Utils.decode(hexUuid)

                        val oobInformationIndex = deviceUuidIndex + 16
                        oobInformation = OobInformation.from(
                            advertisementData.getUShort(oobInformationIndex)
                        )
                    }
                    COMPLETE_LOCAL_NAME -> {
                        localName = advertisementData
                            .copyOfRange(i, i + length - 1)
                            .toString(Charsets.UTF_8)
                    }
                    else -> {
                        throw IllegalArgumentException(
                            "Cannot create an Unprovisioned device with the given advertisement data"
                        )
                    }
                }
                i += length - 1
            }
            require(deviceUuid != null) {
                "Unprovisioned device UUID cannot be null"
            }
            return UnprovisionedDevice(
                name = localName,
                uuid = deviceUuid,
                oobInformation = oobInformation
            )
        }
    }
}