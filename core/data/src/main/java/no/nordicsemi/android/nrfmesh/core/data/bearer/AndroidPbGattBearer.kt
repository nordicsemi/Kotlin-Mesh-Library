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
import no.nordicsemi.kotlin.mesh.bearer.pbgatt.PbGattBearerImpl

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
) : PbGattBearerImpl<
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
    init {
        // Feeds the nRF Mesh Sniffer app, if installed.
        pduObserver = SnifferPduObserver(
            sink = Sniffer.sink,
            bearer = SnifferBearer.PB_GATT,
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
