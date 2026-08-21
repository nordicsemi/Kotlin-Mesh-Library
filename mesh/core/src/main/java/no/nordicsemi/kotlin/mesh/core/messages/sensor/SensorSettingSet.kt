package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.read
import java.nio.ByteOrder

/**
 * This message is used to set the Sensor Setting state of a sensor setting. The response received to the message is [SensorSettingStatus].
 *
 * @property propertyId        The Property ID identifying a sensor.
 * @property settingPropertyId The Property ID identifying a setting within the sensor.
 * @property settingValue      The new value of the setting.
 * @property settingProperty   The Device Property of the setting, or `null` when the Property ID
 *                             is not known.
 */
class SensorSettingSet(
    override val propertyId: UShort,
    val settingPropertyId: UShort,
    val settingValue: DevicePropertyCharacteristic,
) : AcknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SensorSettingStatus.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            settingPropertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            settingValue.data

    val settingProperty: DeviceProperty?
        get() = DeviceProperty.from(id = settingPropertyId)

    /**
     * Convenience constructor.
     *
     * @param setting  The Device Property identifying a setting within the sensor.
     * @param property The Device Property identifying a sensor.
     * @param value    The new value of the setting.
     */
    @Suppress("unused")
    constructor(
        setting: DeviceProperty,
        property: DeviceProperty,
        value: DevicePropertyCharacteristic,
    ) : this(
        propertyId = property.id,
        settingPropertyId = setting.id,
        settingValue = value,
    )

    override fun toString() = "SensorSettingSet(property: ${
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
    }, settingValue: ${settingValue.description})"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x59u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 5 }
            ?.let { params ->
                val settingPropertyId = params
                    .getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
                val settingProperty = DeviceProperty.from(id = settingPropertyId)
                val length = params.size - 4
                // For known properties, make sure the value has the expected length.
                when {
                    settingProperty?.valueLength?.let { it != length } == true -> null
                    else -> SensorSettingSet(
                        propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                        settingPropertyId = settingPropertyId,
                        settingValue = settingProperty.read(
                            data = params,
                            offset = 4,
                            length = length
                        ),
                    )
                }
            }
    }
}
