@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.bearer.gatt.utils

import kotlin.uuid.Uuid

/**
 * Base interface for Mesh service objects.
 *
 * @property uuid            UUID of the service.
 * @property dataInUuid      UUID of the Data In characteristic
 * @property dataOutUuid     UUID of the Data Out characteristic.
 */
sealed interface MeshService {
    val uuid: Uuid
    val dataInUuid: Uuid
    val dataOutUuid: Uuid
}

/**
 * Mesh Provisioning Service object defines the mesh provisioning service. This service is present
 * on the unprovisioned devices.
 *
 * This is used to send provisioning messages over gatt.
 */
object MeshProvisioningService : MeshService {
    override val uuid: Uuid = Uuid.parse("00001827-0000-1000-8000-00805f9b34fb")
    override val dataInUuid: Uuid = Uuid.parse("00002adb-0000-1000-8000-00805f9b34fb")
    override val dataOutUuid: Uuid = Uuid.parse("00002adc-0000-1000-8000-00805f9b34fb")
}

/**
 * Mesh Proxy Service object defines the mesh proxy service. This service is present
 * on the provisioned devices.
 *
 * This is used to send mesh messages over gatt.
 */
object MeshProxyService : MeshService {
    override val uuid: Uuid = Uuid.parse("00001828-0000-1000-8000-00805f9b34fb")
    override val dataInUuid: Uuid = Uuid.parse("00002add-0000-1000-8000-00805f9b34fb")
    override val dataOutUuid: Uuid = Uuid.parse("00002ade-0000-1000-8000-00805f9b34fb")
}