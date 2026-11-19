@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.provisioning

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.mesh.core.oob.OobInformation
import no.nordicsemi.kotlin.mesh.core.util.Utils
import kotlin.uuid.Uuid

data class UnprovisionedDevice(
    var name: String,
    val uuid: Uuid,
    val oobInformation: OobInformation = OobInformation.None,
    val uriHash: ByteArray? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnprovisionedDevice) return false
        return name == other.name &&
                uuid == other.uuid &&
                oobInformation == other.oobInformation &&
                uriHash.contentEquals(other.uriHash)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + uuid.hashCode()
        result = 31 * result + oobInformation.hashCode()
        result = 31 * result + (uriHash?.contentHashCode() ?: 0)
        return result
    }

    companion object {
        private const val SHORTENED_LOCAL_NAME = 0x08
        private const val SERVICE_DATA_16_BIT_UUID = 0x16
        private const val COMPLETE_LOCAL_NAME = 0x09

        /**
         * Creates an UnprovisionedDevice from the advertisement data.
         *
         * @param advertisementData The advertisement data.
         * @return UnprovisionedDevice from the given advertisement data.
         */
        fun from(advertisementData: ByteArray): UnprovisionedDevice {
            var deviceUuid: Uuid? = null
            var oobInformation: OobInformation = OobInformation.Other
            var uriHash: ByteArray? = null
            var localName = ""
            var i = 0
            while (i < advertisementData.size) {
                val length = advertisementData[i].toInt() and 0xFF
                if (length == 0 || i + 1 + length > advertisementData.size) break
                val type = advertisementData[i + 1].toInt() and 0xFF
                // Data of the AD structure is between i + 2 and i + 1 + length (exclusive).
                val start = i + 2
                val end = i + 1 + length
                when (type) {
                    SERVICE_DATA_16_BIT_UUID -> {
                        // 2 bytes service UUID, 16 bytes device UUID and optionally 2 bytes OOB.
                        if (end - start >= 18 &&
                            advertisementData[start] == 0x27.toByte() &&
                            advertisementData[start + 1] == 0x18.toByte()
                        ) {
                            val hexUuid = advertisementData
                                .copyOfRange(start + 2, start + 18)
                                .toHexString()
                            deviceUuid = Utils.decode(hexUuid)
                            if (end - start >= 20) {
                                oobInformation = OobInformation.from(
                                    advertisementData.getUShort(start + 18)
                                )
                            }
                            // The optional URI hash follows the OOB information.
                            if (end - start >= 24) {
                                uriHash = advertisementData.copyOfRange(start + 20, start + 24)
                            }
                        }
                    }
                    SHORTENED_LOCAL_NAME, COMPLETE_LOCAL_NAME -> {
                        // A complete name is never replaced by a shortened one.
                        if (type == COMPLETE_LOCAL_NAME || localName.isEmpty()) {
                            localName = advertisementData
                                .copyOfRange(start, end)
                                .toString(Charsets.UTF_8)
                        }
                    }
                    else -> {
                        /* Other ADs, e.g. flags, UUID lists, TX power or manufacturer data are
                           not relevant and are ignored. */
                    }
                }
                i = end
            }
            require(deviceUuid != null) {
                "Unprovisioned device UUID cannot be null"
            }
            return UnprovisionedDevice(
                name = localName,
                uuid = deviceUuid,
                oobInformation = oobInformation,
                uriHash = uriHash
            )
        }
    }
}