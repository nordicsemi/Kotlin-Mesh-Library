package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import java.nio.ByteOrder

/**
 * This message is used to request the Sensor Cadence state of a sensor. The response received to
 * the message is [SensorCadenceStatus].
 *
 * @property propertyId The Property ID of the sensor to get the cadence of.
 */
class SensorCadenceGet(
    override val propertyId: UShort,
) : AcknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SensorCadenceStatus.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Convenience constructor.
     *
     * @param property The Device Property of the sensor.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty) : this(propertyId = property.id)

    override fun toString() = "SensorCadenceGet(property: ${property ?: propertyId})"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x8234u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let {
                SensorCadenceGet(
                    propertyId = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                )
            }
    }
}
