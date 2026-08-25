package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.SensorCadence
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import java.nio.ByteOrder

/**
 * This message is a response to a [SensorCadenceGet], [SensorCadenceSet] and
 * [SensorCadenceSetUnacknowledged] and contains the Sensor Cadence state of a sensor.
 *
 * @property propertyId Property ID identifying a sensor.
 * @property cadence    Current Sensor Cadence, or `null` when the sensor does not support the
 *                      Sensor Cadence state.
 */
class SensorCadenceStatus(
    override val propertyId: UShort,
    val cadence: SensorCadence?,
) : MeshResponse, UnacknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val parameters = propertyId
        .toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            (cadence?.data ?: byteArrayOf())

    /**
     * Convenience constructor.
     *
     * @param property The Device Property identifying a sensor.
     * @param cadence  The Sensor Cadence to be returned, or `null` when the sensor does not
     *                 support the Sensor Cadence state.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty, cadence: SensorCadence?) : this(
        propertyId = property.id,
        cadence = cadence,
    )

    override fun toString() = "SensorCadenceStatus(property: ${property ?: propertyId}, " +
            "cadence: ${cadence ?: "not supported"})"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x57u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 || it.size >= 8 }
            ?.let { params ->
                val propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                when (params.size) {
                    2 -> SensorCadenceStatus(propertyId = propertyId, cadence = null)
                    else -> SensorCadence
                        .from(DeviceProperty.from(propertyId), params, offset = 2)
                        ?.let { SensorCadenceStatus(propertyId = propertyId, cadence = it) }
                }
            }
    }
}
