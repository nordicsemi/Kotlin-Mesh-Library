package no.nordicsemi.android.nrfmesh.core.common

import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId


/**
 * Nordic Semiconductor Company ID.
 *
 * The value is registered with Bluetooth SIG.
 */
@Suppress("unused")
const val NORDIC_SEMICONDUCTOR_COMPANY_ID: UShort = 0x0059u

/**
 * Supported vendor models for Nordic Semiconductor Company ID.
 * @see[Read more in the documentation] https://docs.nordicsemi.com/bundle/ncs-latest/page/nrf/protocols/bt/bt_mesh/overview/reserved_ids.html
 * for complete list of them.
 */
@Suppress("unused")
object VendorModelIds {
    const val SIMPLE_ON_OFF_SERVER_MODEL_ID: UShort = 0x0000u
    const val SIMPLE_ON_OFF_CLIENT_MODEL_ID: UShort = 0x0001u
    const val RSSI_SERVER: UShort = 0x0005u
    const val RSSI_CLIENT: UShort = 0x0006u
    const val RSSI_UTIL: UShort = 0x0007u
    const val THINGY52_SERVER: UShort = 0x0008u
    const val THINGY52_CLIENT: UShort = 0x0009u
    const val CHAT_CLIENT: UShort = 0x000Au
    const val DISTANCE_MEASUREMENT_SERVER: UShort = 0x000Bu
    const val DISTANCE_MEASUREMENT_CLIENT: UShort = 0x000Cu

    /**
     * The LE Pairing Initiator model is a vendor model that can be used to obtain
     * a passkey that will authenticate a Bluetooth LE connection over a mesh network
     * when it is not possible to use other pairing methods.
     */
    const val LE_PAIRING_INITIATOR: UShort = 0x000Du

    /**
     * The LE Pairing Responder model is a vendor model that can be used to hand over
     * a passkey that will authenticate a Bluetooth LE connection over a mesh network
     * when it is not possible to use other pairing methods.
     *
     * @see[Read more in the documentation](https://docs.nordicsemi.com/bundle/ncs-latest/page/nrf/libraries/bluetooth/mesh/vnd/le_pair_resp.html#bt-mesh-le-pair-resp-readme).
     */
    const val LE_PAIRING_RESPONDER: UShort = 0x000Eu
}

val lePairingResponder = VendorModelId(
    modelIdentifier = VendorModelIds.LE_PAIRING_RESPONDER,
    companyIdentifier = NORDIC_SEMICONDUCTOR_COMPANY_ID
)

val firmwareDistributionServer =
    SigModelId(modelIdentifier = Model.FIRMWARE_DISTRIBUTION_SERVER_MODEL_ID)

val firmwareUpdateServer = SigModelId(modelIdentifier = Model.FIRMWARE_UPDATE_SERVER_MODEL_ID)

val blobTransferServerModel = SigModelId(modelIdentifier = Model.BLOB_TRANSFER_SERVER_MODEL_ID)