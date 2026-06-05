package no.nordicsemi.android.nrfmesh.core.data

import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId


/**
 * Returns a human-readable name for the Model.
 *
 * @return String model name
 */
fun Model.name(): String {
    val vendorModelId = modelId as? VendorModelId
        ?: return name
            ?: "Unknown Model ID: ${modelId.toHex()}"
    return when (vendorModelId.companyIdentifier) {
        NORDIC_SEMICONDUCTOR_COMPANY_ID -> when (vendorModelId.modelIdentifier) {
            VendorModelIds.SIMPLE_ON_OFF_SERVER_MODEL_ID -> "Simple OnOff Server"
            VendorModelIds.SIMPLE_ON_OFF_CLIENT_MODEL_ID -> "Simple OnOff Client"
            VendorModelIds.RSSI_SERVER -> "RSSI Server"
            VendorModelIds.RSSI_CLIENT -> "RSSI Client"
            VendorModelIds.RSSI_UTIL -> "RSSI Util"
            VendorModelIds.THINGY52_SERVER -> "Thingy52 Server"
            VendorModelIds.THINGY52_CLIENT -> "Thingy52 Client"
            VendorModelIds.CHAT_CLIENT -> "Chat Client"
            VendorModelIds.DISTANCE_MEASUREMENT_SERVER -> "Distance Measurement Server"
            VendorModelIds.DISTANCE_MEASUREMENT_CLIENT -> "Distance Measurement Client"
            VendorModelIds.LE_PAIRING_INITIATOR -> "LE Pairing Initiator"
            VendorModelIds.LE_PAIRING_RESPONDER -> "LE Pairing Responder"
            else -> "Vendor Model ID: ${modelId.toHex()}"
        }
        else -> "Vendor Model ID: ${modelId.toHex()}"
    }
}