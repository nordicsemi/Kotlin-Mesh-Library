package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SensorPropertyMessage
import java.nio.ByteOrder

/**
 * This message is used to request a sequence of Sensor Series Column states of a sensor. The
 * response received to the message is [SensorSeriesStatus].
 *
 * @property propertyId The Property ID identifying a sensor.
 * @property rawValueX1 Raw value identifying a starting column.
 * @property rawValueX2 Raw value identifying an ending column.
 */
class SensorSeriesGet(
    override val propertyId: UShort,
    val rawValueX1: ByteArray,
    val rawValueX2: ByteArray,
) : AcknowledgedMeshMessage, SensorPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SensorSeriesStatus.opCode
    override val parameters = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            rawValueX1 + rawValueX2

    init {
        require(rawValueX1.isNotEmpty()) { "Raw Value X1 must not be empty" }
        require(rawValueX1.size == rawValueX2.size) {
            "Raw Value X1 and X2 must have the same length"
        }
    }

    /**
     * Convenience constructor.
     *
     * @param property   Device Property identifying a sensor.
     * @param rawValueX1 Raw value identifying a starting column.
     * @param rawValueX2 Raw value identifying an ending column.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty, rawValueX1: ByteArray, rawValueX2: ByteArray) : this(
        propertyId = property.id,
        rawValueX1 = rawValueX1,
        rawValueX2 = rawValueX2,
    )

    override fun toString() = "SensorSeriesGet(property: ${
        property ?: propertyId.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    }, rawValueX1: ${
        rawValueX1.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    }, rawValueX2: ${
        rawValueX2.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    })"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x8233u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size > 2 && (it.size - 2) % 2 == 0 }
            ?.let {
                val length = (it.size - 2) / 2
                SensorSeriesGet(
                    propertyId = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    rawValueX1 = it.copyOfRange(fromIndex = 2, toIndex = 2 + length),
                    rawValueX2 = it.copyOfRange(fromIndex = 2 + length, toIndex = it.size),
                )
            }
    }
}
