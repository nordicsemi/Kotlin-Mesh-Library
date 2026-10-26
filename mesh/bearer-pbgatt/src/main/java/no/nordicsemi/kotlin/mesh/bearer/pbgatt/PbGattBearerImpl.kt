@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.bearer.pbgatt

import kotlinx.coroutines.CoroutineDispatcher
import no.nordicsemi.kotlin.ble.client.CentralManager
import no.nordicsemi.kotlin.ble.client.Peripheral
import no.nordicsemi.kotlin.ble.client.ScanResult
import no.nordicsemi.kotlin.mesh.bearer.PduTypes
import no.nordicsemi.kotlin.mesh.bearer.gatt.BaseGattBearer
import no.nordicsemi.kotlin.mesh.bearer.gatt.MeshProvisioningProfile
import no.nordicsemi.kotlin.mesh.bearer.gatt.MeshServiceProfile
import no.nordicsemi.kotlin.mesh.bearer.provisioning.ProvisioningBearer

/**
 * Responsible for receiving and sending mesh provisioning messages to and from the GATT Proxy Node.
 *
 * @param centralManager Central manager used to connect to the peripheral.
 * @param peripheral     Peripheral to connect to.
 * @param ioDispatcher   Coroutine dispatcher used by the bearer.
 */
open class PbGattBearerImpl<
        ID : Any,
        C : CentralManager<ID, P, EX, F, SR>,
        P : Peripheral<ID, EX>,
        EX : Peripheral.Executor<ID>,
        F : CentralManager.ScanFilterScope,
        SR : ScanResult<*, *>,
        >(
    centralManager: C,
    peripheral: P,
    ioDispatcher: CoroutineDispatcher,
) : BaseGattBearer<ID, C, P, EX, F, SR>(
    centralManager = centralManager,
    peripheral = peripheral,
    ioDispatcher = ioDispatcher
), ProvisioningBearer {
    final override val supportedTypes: Array<PduTypes> = arrayOf(PduTypes.ProvisioningPdu)
    final override val profile: MeshServiceProfile = MeshProvisioningProfile()
}
