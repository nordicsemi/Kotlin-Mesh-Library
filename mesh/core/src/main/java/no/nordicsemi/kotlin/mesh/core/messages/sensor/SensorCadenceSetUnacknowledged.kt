package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.SensorCadence
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import java.nio.ByteOrder

/**
 * This message is used to set the Sensor Cadence state of a sensor. This message is not acknowledged.
 *
 * @property propertyId Property ID identifying a sensor.
 * @property cadence    Sensor Cadence value.
 */
class SensorCadenceSetUnacknowledged(
    override val propertyId: UShort,
    val cadence: SensorCadence,
) : UnacknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode

    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) + cadence.data

    /**
     * Convenience constructor.
     *
     * @param property The Device Property identifying a sensor.
     * @param cadence  The Sensor Cadence to set.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty, cadence: SensorCadence) : this(
        propertyId = property.id,
        cadence = cadence,
    )

    override fun toString() = "SensorCadenceSetUnacknowledged(property: ${
        property ?: propertyId
            .toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )
    }, cadence: $cadence)"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x56u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 8 }
            ?.let { params ->
                val propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                SensorCadence
                    .from(
                        property = DeviceProperty.from(id = propertyId),
                        parameters = params,
                        offset = 2
                    )
                    ?.let { SensorCadenceSetUnacknowledged(propertyId = propertyId, cadence = it) }
            }
    }
}
