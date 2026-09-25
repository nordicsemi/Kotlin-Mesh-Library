package no.nordicsemi.android.nrfmesh.core.data.bearer

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.ConjunctionFilterScope
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.client.android.ScanResult
import no.nordicsemi.kotlin.ble.core.WriteType
import no.nordicsemi.kotlin.mesh.bearer.gatt.GattBearerImpl
import kotlin.uuid.ExperimentalUuidApi

/**
 * Android implementation of GattBearer.
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

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun configurePeripheral(peripheral: Peripheral) {
        // Request the highest connection parameters after connect in the super.open()
        peripheral.requestHighestValueLength()
        mtu = peripheral.maximumWriteValueLength(WriteType.WITHOUT_RESPONSE)
    }
}