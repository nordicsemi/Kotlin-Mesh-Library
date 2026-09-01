package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import java.nio.ByteOrder

/**
 * This message is used to request the Sensor Setting state of a sensor setting. The response
 * received to the message is [SensorSettingStatus].
 *
 * @property propertyId        Property ID of the sensor.
 * @property settingPropertyId Property ID identifying a setting within the sensor.
 * @property settingProperty   Device Property of the setting, or `null` when the Property ID
 *                             is not known.
 */
class SensorSettingGet(
    override val propertyId: UShort,
    val settingPropertyId: UShort,
) : AcknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SensorSettingStatus.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            settingPropertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    val settingProperty: DeviceProperty?
        get() = DeviceProperty.from(id = settingPropertyId)

    /**
     * Convenience constructor.
     *
     * @param setting  Device Property identifying a setting within the sensor.
     * @param property Device Property of the sensor.
     */
    @Suppress("unused")
    constructor(setting: DeviceProperty, property: DeviceProperty) : this(
        propertyId = property.id,
        settingPropertyId = setting.id,
    )

    override fun toString() = "SensorSettingGet(property: ${
        property ?: propertyId.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    }, settingProperty: ${
        settingProperty ?: settingPropertyId.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    })"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x8236u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 4 }
            ?.let {
                SensorSettingGet(
                    propertyId = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    settingPropertyId = it.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                )
            }
    }
}
