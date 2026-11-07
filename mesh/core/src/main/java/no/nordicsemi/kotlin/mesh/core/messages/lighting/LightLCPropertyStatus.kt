package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.read

/**
 * Reports the value of a Light LC Property of a Light LC Server.
 *
 * This message is a response to a [LightLCPropertyGet], [LightLCPropertySet] and
 * [LightLCPropertySetUnacknowledged].
 *
 * @property propertyId    Property ID identifying a Light LC Property.
 * @property propertyValue The value of the property.
 */
class LightLCPropertyStatus(
    override val propertyId: UShort,
    val propertyValue: DevicePropertyCharacteristic,
) : ApplicationResponse, UnacknowledgedApplicationMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            propertyValue.data

    /**
     * Creates a Light LC Property Status message from a [DeviceProperty].
     *
     * The property can be one of:
     * - [DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_ON]
     * - [DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_PROLONG]
     * - [DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_STANDBY]
     * - [DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON]
     * - [DeviceProperty.LIGHT_CONTROL_LIGHTNESS_PROLONG]
     * - [DeviceProperty.LIGHT_CONTROL_LIGHTNESS_STANDBY]
     * - [DeviceProperty.LIGHT_CONTROL_REGULATOR_ACCURACY]
     * - [DeviceProperty.LIGHT_CONTROL_REGULATOR_KID]
     * - [DeviceProperty.LIGHT_CONTROL_REGULATOR_KIU]
     * - [DeviceProperty.LIGHT_CONTROL_REGULATOR_KPD]
     * - [DeviceProperty.LIGHT_CONTROL_REGULATOR_KPU]
     * - [DeviceProperty.LIGHT_CONTROL_TIME_FADE]
     * - [DeviceProperty.LIGHT_CONTROL_TIME_FADE_ON]
     * - [DeviceProperty.LIGHT_CONTROL_TIME_FADE_STANDBY_AUTO]
     * - [DeviceProperty.LIGHT_CONTROL_TIME_FADE_STANDBY_MANUAL]
     * - [DeviceProperty.LIGHT_CONTROL_TIME_OCCUPANCY_DELAY]
     * - [DeviceProperty.LIGHT_CONTROL_TIME_PROLONG]
     * - [DeviceProperty.LIGHT_CONTROL_TIME_RUN_ON]
     *
     * @param property Device Property identifying a Light LC Property.
     * @param value    The value of the property.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty, value: DevicePropertyCharacteristic) : this(
        propertyId = property.id,
        propertyValue = value,
    )

    override fun toString() = "LightLCPropertyStatus(property: ${
        property ?: propertyId.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    }, value: ${propertyValue.description})"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x64u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 3 }
            ?.let { params ->
                val propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                val property = DeviceProperty.from(id = propertyId)
                val length = params.size - 2
                // For known properties, make sure the value has the expected length.
                when {
                    property?.valueLength?.let { it != length } == true -> null
                    else -> LightLCPropertyStatus(
                        propertyId = propertyId,
                        propertyValue = property.read(data = params, offset = 2, length = length),
                    )
                }
            }
    }
}
