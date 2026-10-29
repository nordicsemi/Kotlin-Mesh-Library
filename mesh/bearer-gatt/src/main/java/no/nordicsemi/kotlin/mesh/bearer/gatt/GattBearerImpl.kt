@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.bearer.gatt

import kotlinx.coroutines.CoroutineDispatcher
import no.nordicsemi.kotlin.ble.client.CentralManager
import no.nordicsemi.kotlin.ble.client.Peripheral
import no.nordicsemi.kotlin.ble.client.ScanResult
import no.nordicsemi.kotlin.mesh.bearer.MeshBearer
import no.nordicsemi.kotlin.mesh.bearer.PduTypes

/**
 * Responsible for receiving and sending mesh messages to and from the GATT Proxy Node.
 *
 * @param peripheral     Peripheral to connect to.
 * @param centralManager Central manager used to connect to the peripheral.
 * @param ioDispatcher   Coroutine dispatcher used by the bearer.
 */
open class GattBearerImpl<
        ID : Any,
        C : CentralManager<ID, P, EX, F, SR>,
        P : Peripheral<ID, EX>,
        EX : Peripheral.Executor<ID>,
        F : CentralManager.ScanFilterScope,
        SR : ScanResult<*, *>,
        >(
    peripheral: P,
    centralManager: C,
    ioDispatcher: CoroutineDispatcher,
) : BaseGattBearer<ID, C, P, EX, F, SR>(
    centralManager = centralManager,
    peripheral = peripheral,
    ioDispatcher = ioDispatcher
), MeshBearer {
    override val supportedTypes: Array<PduTypes> =
        arrayOf(PduTypes.NetworkPdu, PduTypes.MeshBeacon, PduTypes.ProxyConfiguration)
    override val profile: MeshServiceProfile = MeshProxyProfile()
}
