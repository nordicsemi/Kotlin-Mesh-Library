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
 * This message is a response to a [SensorSettingsGet] and contains the list of all Sensor Setting
 * Property states of a sensor.
 *
 * @property propertyId         The Property ID identifying a sensor.
 * @property settingPropertyIds The Property IDs of all settings of the sensor, or `null` when the
 *                              field was not present in the message.
 * @property settingProperties  The Device Properties of all settings of the sensor, where unknown
 *                              Property IDs are reported as `null`.
 */
class SensorSettingsStatus(
    override val propertyId: UShort,
    val settingPropertyIds: List<UShort>?,
) : MeshResponse, UnacknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            (settingPropertyIds ?: emptyList())
                .fold(byteArrayOf()) { data, id ->
                    data + id.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
                }

    val settingProperties: List<DeviceProperty?>?
        get() = settingPropertyIds?.map { DeviceProperty.from(id = it) }

    /**
     * Convenience constructor.
     *
     * @param property           The Device Property identifying a sensor.
     * @param settingProperties  The Device Properties of all settings of the sensor.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty, settingProperties: List<DeviceProperty>?) : this(
        propertyId = property.id,
        settingPropertyIds = settingProperties?.map { it.id },
    )

    override fun toString() = "SensorSettingsStatus(property: ${property ?: propertyId.toHexString(
        format = HexFormat {
            number.prefix = "0x"
            upperCase = true
        }
    )}, settingProperties: ${settingProperties?.joinToString() ?: "none"})"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x58u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 2 && it.size % 2 == 0 }
            ?.let { params ->
                SensorSettingsStatus(
                    propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    settingPropertyIds = when (params.size) {
                        2 -> null
                        else -> (2 until params.size step 2).map {
                            params.getUShort(offset = it, order = ByteOrder.LITTLE_ENDIAN)
                        }
                    },
                )
            }
    }
}
