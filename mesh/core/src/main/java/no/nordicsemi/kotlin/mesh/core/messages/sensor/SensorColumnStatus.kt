package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import java.nio.ByteOrder

/**
 * This message is a response to a [SensorColumnGet] and contains the Sensor Series Column state
 * of a sensor.
 *
 * The result consists of Raw Value X, an optional Column Width and, when the Column Width is
 * present, Raw Value Y. The values are returned as a single byte array, as their lengths and
 * types may differ depending on the sensor implementation.
 *
 * @property propertyId The Property ID identifying a sensor and the Y axis.
 * @property result     Raw Value X and, optionally, Column Width and Raw Value Y. Little endian
 *                      is to be used for multi-octet values.
 */
class SensorColumnStatus(
    override val propertyId: UShort,
    val result: ByteArray,
) : MeshResponse, UnacknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) + result

    init {
        require(result.isNotEmpty()) { "Result must not be empty" }
    }

    /**
     * Convenience constructor.
     *
     * @param property The Device Property identifying a sensor and the Y axis.
     * @param result   Raw Value X and, optionally, Column Width and Raw Value Y.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty, result: ByteArray) : this(
        propertyId = property.id,
        result = result,
    )

    override fun toString() = "SensorColumnStatus(property: ${
        property ?: propertyId.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    }, result: ${result.toHex()})"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x53u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size > 2 }
            ?.let {
                SensorColumnStatus(
                    propertyId = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    result = it.copyOfRange(fromIndex = 2, toIndex = it.size),
                )
            }
    }
}
