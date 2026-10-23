package no.nordicsemi.android.nrfmesh.core.data.bearer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.mesh.bearer.PduTypes
import no.nordicsemi.kotlin.mesh.bearer.provisioning.ProvisioningBearer

/**
 * Android implementation of the PB-GATT bearer, used to provision an unprovisioned device.
 *
 * @param centralManager CentralManager instance.
 * @param peripheral     Peripheral instance.
 * @param ioDispatcher   Coroutine dispatcher for IO operations.
 */
class AndroidPbGattBearer(
    centralManager: CentralManager,
    peripheral: Peripheral,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MeshProfileBearer(
    centralManager = centralManager,
    peripheral = peripheral,
    profileFactory = ::MeshProvisioningProfile,
    supportedTypes = arrayOf(PduTypes.ProvisioningPdu),
    ioDispatcher = ioDispatcher
), ProvisioningBearer
