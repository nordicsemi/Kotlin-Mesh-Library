package no.nordicsemi.android.nrfmesh.core.data.bearer.sniffer

import no.nordicsemi.kotlin.mesh.bearer.PduType

/**
 * Bearer over which a PDU was sent or received, as understood by the nRF Mesh Sniffer.
 *
 * @property value Value of the `bearer` column.
 */
enum class SnifferBearer(val value: Int) {
    /** GATT proxy bearer. */
    GATT(0),

    /** PB-GATT provisioning bearer. */
    PB_GATT(1),
}

/**
 * Direction of a PDU, as understood by the nRF Mesh Sniffer.
 *
 * @property value Value of the `direction` column.
 */
enum class SnifferDirection(val value: Int) {
    /** Received by nRF Mesh. */
    RECEIVED(0),

    /** Sent by nRF Mesh. */
    SENT(1),
}

/**
 * A Proxy PDU to be handed over to the nRF Mesh Sniffer.
 *
 * @property timestamp Epoch milliseconds taken when the bearer sent or received the PDU.
 * @property data      Proxy PDU payload without the Proxy header.
 * @property type      Proxy PDU type, see [proxyType].
 * @property direction Direction of the PDU.
 * @property bearer    Bearer used.
 * @property address   Identifier of the peripheral, set only for received PDUs.
 * @property name      Name of the peripheral, if known.
 */
@Suppress("ArrayInDataClass")
data class SnifferPdu(
    val timestamp: Long,
    val data: ByteArray,
    val type: Int,
    val direction: SnifferDirection,
    val bearer: SnifferBearer,
    val address: String?,
    val name: String?,
) {
    /**
     * The values stored in the Sniffer's `pdus` table. Optional values are omitted when null.
     */
    fun toValues(): Map<String, Any> = buildMap {
        put(TIMESTAMP, timestamp)
        put(DATA, data)
        put(TYPE, type)
        put(DIRECTION, direction.value)
        put(BEARER, bearer.value)
        address?.let { put(ADDRESS, it) }
        name?.let { put(NAME, it) }
    }

    companion object {
        const val TIMESTAMP = "timestamp"
        const val DATA = "data"
        const val TYPE = "type"
        const val DIRECTION = "direction"
        const val BEARER = "bearer"
        const val ADDRESS = "address"
        const val NAME = "name"

        /**
         * Maps [PduType] to the Proxy PDU type of Mesh Protocol 6.3, explicitly.
         */
        fun proxyType(type: PduType): Int = when (type) {
            PduType.NETWORK_PDU -> 0
            PduType.MESH_BEACON -> 1
            PduType.PROXY_CONFIGURATION -> 2
            PduType.PROVISIONING_PDU -> 3
        }
    }
}
