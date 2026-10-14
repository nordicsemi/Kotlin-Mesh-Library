package no.nordicsemi.android.nrfmesh.core.data.bearer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.mesh.bearer.MeshBearer
import no.nordicsemi.kotlin.mesh.bearer.PduTypes

/**
 * Android implementation of the GATT bearer, used to communicate with a GATT proxy node.
 *
 * @param centralManager CentralManager instance.
 * @param peripheral     Peripheral instance.
 * @param ioDispatcher   Coroutine dispatcher for IO operations.
 * @property identifier  Identifier of the peripheral.
 */
class AndroidGattBearer(
    centralManager: CentralManager,
    peripheral: Peripheral,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : MeshProfileBearer(
    centralManager = centralManager,
    peripheral = peripheral,
    profileFactory = ::MeshProxyProfile,
    supportedTypes = arrayOf(
        PduTypes.NetworkPdu,
        PduTypes.MeshBeacon,
        PduTypes.ProxyConfiguration
    ),
    ioDispatcher = ioDispatcher
), MeshBearer {
    val identifier
        get() = peripheral.identifier
}
