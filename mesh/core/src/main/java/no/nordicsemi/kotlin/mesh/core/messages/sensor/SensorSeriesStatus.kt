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
 * This message is a response to a [SensorSeriesGet] and contains a sequence of Sensor Series
 * Column states of a sensor.
 *
 * Each series consists of Raw Value X, Column Width and Raw Value Y. The series are returned as
 * a single byte array, as their lengths and types may differ depending on the sensor
 * implementation.
 *
 * @property propertyId    The Property ID identifying a sensor and the Y axis.
 * @property seriesRawData A sequence of Raw Value X, Column Width and Raw Value Y, or `null` when
 *                         no series were returned. Little endian is to be used for multi-octet
 *                         values.
 */
class SensorSeriesStatus(
    override val propertyId: UShort,
    val seriesRawData: ByteArray?,
) : MeshResponse, UnacknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            (seriesRawData ?: byteArrayOf())

    /**
     * Convenience constructor.
     *
     * @param property      The Device Property identifying a sensor and the Y axis.
     * @param seriesRawData A sequence of Raw Value X, Column Width and Raw Value Y.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty, seriesRawData: ByteArray?) : this(
        propertyId = property.id,
        seriesRawData = seriesRawData,
    )

    override fun toString() = "SensorSeriesStatus(property: ${
        property ?: propertyId.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    }, seriesRawData: ${
        seriesRawData?.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        ) ?: "none"
    })"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x54u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 2 }
            ?.let {
                SensorSeriesStatus(
                    propertyId = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    seriesRawData = if (it.size > 2) it.copyOfRange(
                        fromIndex = 2,
                        toIndex = it.size
                    ) else null,
                )
            }
    }
}
