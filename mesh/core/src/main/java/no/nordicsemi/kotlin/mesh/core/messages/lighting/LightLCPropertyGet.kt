package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage

/**
 * This message is used to get the Light LC Property state of a Light LC Server. The response
 * received to the message is [LightLCPropertyStatus].
 *
 * @property propertyId Property ID identifying a Light LC Property.
 */
class LightLCPropertyGet(
    override val propertyId: UShort,
) : AcknowledgedApplicationMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLCPropertyStatus.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Convenience constructor.
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
     */
    @Suppress("unused")
    constructor(property: DeviceProperty) : this(propertyId = property.id)

    override fun toString() = "LightLCPropertyGet(property: ${
        property ?: propertyId.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    })"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x829Du

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let { LightLCPropertyGet(it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)) }
    }
}
