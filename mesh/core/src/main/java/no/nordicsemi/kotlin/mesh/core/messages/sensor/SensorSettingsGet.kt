package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import java.nio.ByteOrder

/**
 * This message is used to request the list of all Sensor Setting Property states of a sensor. The
 * response received to the message is [SensorSettingsStatus].
 *
 * @property propertyId The Property ID of the sensor to get the settings of.
 */
class SensorSettingsGet(
    override val propertyId: UShort,
) : AcknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SensorSettingsStatus.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Convenience constructor.
     *
     * @param property Device Property of the sensor.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty) : this(propertyId = property.id)

    override fun toString() = "SensorSettingsGet(property: ${
        property ?: propertyId.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    })"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x8235u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let {
                SensorSettingsGet(
                    propertyId = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                )
            }
    }
}
