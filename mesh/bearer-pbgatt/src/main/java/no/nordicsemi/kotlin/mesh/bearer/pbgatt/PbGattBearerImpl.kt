@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.bearer.pbgatt

import kotlinx.coroutines.CoroutineDispatcher
import no.nordicsemi.kotlin.ble.client.CentralManager
import no.nordicsemi.kotlin.ble.client.Peripheral
import no.nordicsemi.kotlin.ble.client.ScanResult
import no.nordicsemi.kotlin.mesh.bearer.PduTypes
import no.nordicsemi.kotlin.mesh.bearer.gatt.BaseGattBearer
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshProvisioningService
import no.nordicsemi.kotlin.mesh.bearer.provisioning.ProvisioningBearer

/**
 * Responsible for receiving and sending mesh provisioning messages to and from the GATT Proxy Node.
 */
open class PbGattBearerImpl<
        ID : Any,
        C : CentralManager<ID, P, EX, F, SR>,
        P : Peripheral<ID, EX>,
        EX : Peripheral.Executor<ID>,
        F : CentralManager.ScanFilterScope,
        SR : ScanResult<*, *>,z
        >(
    centralManager: C,
    peripheral: P,
    ioDispatcher: CoroutineDispatcher,
) : BaseGattBearer<ID, C, P, EX, F, SR>(
    centralManager = centralManager,
    peripheral = peripheral,
    ioDispatcher = ioDispatcher
), ProvisioningBearer {
    final override val meshService: MeshProvisioningService = MeshProvisioningService
    final override val supportedTypes: Array<PduTypes> = arrayOf(PduTypes.ProvisioningPdu)
}