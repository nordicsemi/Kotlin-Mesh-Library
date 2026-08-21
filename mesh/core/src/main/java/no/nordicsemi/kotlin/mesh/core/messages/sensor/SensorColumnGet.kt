package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import java.nio.ByteOrder

/**
 * This message is used to request the Sensor Series Column state of a sensor. The response
 * received to the message is [SensorColumnStatus].
 *
 * @property propertyId The Property ID identifying a sensor.
 * @property rawValueX  Raw value identifying a column. Little endian is to be used for
 *                      multi-octet values.
 */
class SensorColumnGet(
    override val propertyId: UShort,
    val rawValueX: ByteArray,
) : AcknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SensorColumnStatus.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) + rawValueX

    init {
        require(rawValueX.isNotEmpty()) { "Raw Value X must not be empty" }
    }

    /**
     * Convenience constructor.
     *
     * @param property  The Device Property identifying a sensor.
     * @param rawValueX Raw value identifying a column.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty, rawValueX: ByteArray) : this(
        propertyId = property.id,
        rawValueX = rawValueX,
    )

    override fun toString() = "SensorColumnGet(property: ${property ?: propertyId}, " +
            "rawValueX: ${rawValueX.toHex()})"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x8232u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size > 2 }
            ?.let {
                SensorColumnGet(
                    propertyId = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    rawValueX = it.copyOfRange(fromIndex = 2, toIndex = it.size),
                )
            }
    }
}
