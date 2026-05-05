package no.nordicsemi.kotlin.mesh.bearer.gatt

import no.nordicsemi.kotlin.mesh.bearer.Bearer

/**
 * A base interface for a GATT Bearer.
 *
 * @property name    The name of the Bluetooth LE peripheral.
 */
interface GattBearer : Bearer {
    val name: String?
}