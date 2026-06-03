package no.nordicsemi.android.nrfmesh.feature.model.dfu

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId

/**
 * Defines firmware information specified in Mesh DFU specification.
 *
 * This object is parsed from a JSON returned from an online resource pointed by a Node in its
 * Firmware Image List state.
 */
@Serializable
data class UpdatedFirmwareInformation(val manifest: Manifest) {

    /**
     * Firmware information.
     *
     * @property firmware Firmware information.
     */
    @Serializable
    data class Manifest(val firmware: Firmware) {

        /**
         * Firmware information.
         *
         * @property firmwareIdString      Firmware ID of the firmware image.
         * @property dfuChainSize          Size of the DFU chain.
         * @property firmwareImageFileSize Size of the firmware image.
         * @property firmwareId            Firmware ID of the firmware image.
         */
        @Serializable
        data class Firmware(
            @SerialName("firmware_id")
            val firmwareIdString: String,
            @SerialName("dfu_chain_size")
            val dfuChainSize: Int,
            @SerialName("firmware_image_file_size")
            val firmwareImageFileSize: Int
        ) {

            /**
             * The Firmware ID of the firmware image.
             */
            val firmwareId: FirmwareId
                get() {
                    val data = firmwareIdString.hexToByteArray()
                    return FirmwareId(data)
                }
        }
    }
}