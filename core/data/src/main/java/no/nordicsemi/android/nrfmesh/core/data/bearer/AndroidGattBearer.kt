package no.nordicsemi.android.nrfmesh.core.data.bearer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import no.nordicsemi.android.nrfmesh.core.data.bearer.sniffer.Sniffer
import no.nordicsemi.android.nrfmesh.core.data.bearer.sniffer.SnifferBearer
import no.nordicsemi.android.nrfmesh.core.data.bearer.sniffer.SnifferPduObserver
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.ConjunctionFilterScope
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.client.android.ScanResult
import no.nordicsemi.kotlin.mesh.bearer.gatt.GattBearerImpl

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
) : GattBearerImpl<
        String,
        CentralManager,
        Peripheral,
        Peripheral.Executor,
        ConjunctionFilterScope,
        ScanResult
        >(
    centralManager = centralManager,
    peripheral = peripheral,
    ioDispatcher = ioDispatcher
) {
    val identifier
        get() = peripheral.identifier

    init {
        // Feeds the nRF Mesh Sniffer app, if installed.
        pduObserver = SnifferPduObserver(
            sink = Sniffer.sink,
            bearer = SnifferBearer.GATT,
            address = { peripheral.identifier },
            name = { peripheral.name }
        )
    }

    override suspend fun connect() = centralManager.connect(
        peripheral = peripheral,
        options = CentralManager.ConnectionOptions.Direct(
            automaticallyRequestHighestValueLength = true
        )
    )
}
