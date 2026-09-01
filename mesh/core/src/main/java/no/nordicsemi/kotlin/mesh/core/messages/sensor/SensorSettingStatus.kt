package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.read
import java.nio.ByteOrder

/**
 * This message is a response to a [SensorSettingGet], [SensorSettingSet] and
 * [SensorSettingSetUnacknowledged] and contains the Sensor Setting state of a sensor setting.
 *
 * @property propertyId        Property ID identifying a sensor.
 * @property settingPropertyId Property ID identifying a setting within the sensor.
 * @property settingAccess     Read / write access rights of the setting, or `null` when the
 *                             requested setting property was not found.
 * @property settingValue      Value of the setting, or `null` when the requested setting property
 *                             was not found, or when a read-only setting was attempted to be set.
 * @property settingProperty   Device Property of the setting, or `null` when the Property ID is not
 *                             known.
 */
class SensorSettingStatus(
    override val propertyId: UShort,
    val settingPropertyId: UShort,
    val settingAccess: SensorSettingAccess?,
    val settingValue: DevicePropertyCharacteristic?,
) : MeshResponse, UnacknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            settingPropertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            when {
                settingAccess == null -> byteArrayOf()
                settingValue == null -> byteArrayOf(settingAccess.value.toByte())
                else -> byteArrayOf(settingAccess.value.toByte()) + settingValue.data
            }

    /**
     * The Sensor Setting Access state indicates whether the Device Property can be read or
     * written.
     *
     * @property value The raw value of the access rights.
     */
    enum class SensorSettingAccess(val value: UByte) {

        /** The Device Property can be read. */
        READ_ONLY(0x01.toUByte()),

        /** The Device Property can be read and written. */
        READ_WRITE(0x03.toUByte());

        override fun toString() = when (this) {
            READ_ONLY -> "Read only"
            READ_WRITE -> "Read / write"
        }

        companion object {

            /**
             * Returns the Sensor Setting Access for the given value.
             *
             * @param value The raw value of the access rights.
             * @return The Sensor Setting Access, or `null` when the value is not known.
             */
            fun from(value: UByte): SensorSettingAccess? = entries.find { it.value == value }
        }
    }

    val settingProperty: DeviceProperty?
        get() = DeviceProperty.from(settingPropertyId)

    /**
     * Convenience constructor for a setting which has not been found on the sensor.
     *
     * @param setting  Device Property identifying a setting within the sensor.
     * @param property Device Property identifying a sensor.
     */
    @Suppress("unused")
    constructor(setting: DeviceProperty, property: DeviceProperty) : this(
        propertyId = property.id,
        settingPropertyId = setting.id,
        settingAccess = null,
        settingValue = null,
    )

    /**
     * Convenience constructor.
     *
     * @param setting  Device Property identifying a setting within the sensor.
     * @param property Device Property identifying a sensor.
     * @param access   Read / write access rights of the setting.
     * @param value    Value of the setting.
     */
    @Suppress("unused")
    constructor(
        setting: DeviceProperty,
        property: DeviceProperty,
        access: SensorSettingAccess,
        value: DevicePropertyCharacteristic,
    ) : this(
        propertyId = property.id,
        settingPropertyId = setting.id,
        settingAccess = access,
        settingValue = value,
    )

    init {
        require(settingAccess != null || settingValue == null) {
            "Setting value requires the access rights to be set"
        }
    }

    override fun toString() = "SensorSettingStatus(property: ${
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
    }, settingAccess: ${settingAccess ?: "not found"}, " +
            "settingValue: ${settingValue?.description ?: "none"})"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x5Bu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 4 }
            ?.let { params ->
                val propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                val settingPropertyId = params
                    .getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)

                // The Sensor Setting Access and Sensor Setting Raw fields are optional.
                when (params.size) {
                    4 -> SensorSettingStatus(
                        propertyId = propertyId,
                        settingPropertyId = settingPropertyId,
                        settingAccess = null,
                        settingValue = null,
                    )
                    // When a Sensor Setting Set message was sent for a read-only setting, the
                    // status reports the access rights and omits the value.
                    else -> SensorSettingAccess.from(value = params[4].toUByte())?.let { access ->
                        SensorSettingStatus(
                            propertyId = propertyId,
                            settingPropertyId = settingPropertyId,
                            settingAccess = access,
                            settingValue = when (params.size) {
                                5 -> null
                                else -> DeviceProperty.from(id = settingPropertyId)
                                    .read(data = params, offset = 5, length = params.size - 5)
                            },
                        )
                    }
                }
            }
    }
}
