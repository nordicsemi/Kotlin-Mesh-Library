package no.nordicsemi.android.nrfmesh.core.common

import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId

fun Model.isGenericOnOffServer() =
    (modelId as? SigModelId)?.modelIdentifier == Model.GENERIC_ON_OFF_SERVER_MODEL_ID

fun Model.isGenericLevelServer() =
    (modelId as? SigModelId)?.modelIdentifier == Model.GENERIC_LEVEL_SERVER_MODEL_ID

fun Model.isSceneServer() =
    (modelId as? SigModelId)?.modelIdentifier == Model.SCENE_SERVER_MODEL_ID

fun Model.isSceneSetupServer() =
    (modelId as? SigModelId)?.modelIdentifier == Model.SCENE_SETUP_SERVER_MODEL_ID

fun Model.isLightLCServer() =
    (modelId as? SigModelId)?.modelIdentifier == Model.LIGHT_LC_SERVER_MODEL_ID

fun Model.isFirmwareDistributionServer() =
    (modelId as? SigModelId)?.modelIdentifier == Model.FIRMWARE_DISTRIBUTION_SERVER_MODEL_ID

fun Model.isFirmwareUpdateServer() =
    (modelId as? SigModelId)?.modelIdentifier == Model.FIRMWARE_UPDATE_SERVER_MODEL_ID

fun Model.isLePairingResponderServer() =
    (modelId as? VendorModelId)?.modelIdentifier == VendorModelIds.LE_PAIRING_RESPONDER

fun Model.isVendorModel() = !isBluetoothSigAssigned

fun Model.isSensorServer() =
    (modelId as? SigModelId)?.modelIdentifier == Model.SENSOR_SERVER_MODEL_ID

fun isSupportedGroupItem(model: Model) = model.isGenericOnOffServer() ||
        model.isGenericLevelServer() ||
        model.isLightLCServer() ||
        model.isSceneServer() ||
        model.isSceneSetupServer()

fun Model.isFirmwareDistributionServer() = isBluetoothSigAssigned
        && (modelId as SigModelId).modelIdentifier == Model.FIRMWARE_DISTRIBUTION_SERVER_MODEL_ID

fun Model.isFirmwareUpdateServer() = isBluetoothSigAssigned
        && (modelId as SigModelId).modelIdentifier == Model.FIRMWARE_UPDATE_SERVER_MODEL_ID

fun Model.isLePairingResponderServer() =
    (modelId as VendorModelId).modelIdentifier == VendorModelIds.LE_PAIRING_RESPONDER