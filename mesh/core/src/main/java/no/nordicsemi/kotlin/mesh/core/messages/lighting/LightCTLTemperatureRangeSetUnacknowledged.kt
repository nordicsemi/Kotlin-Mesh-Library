package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage

/**
 * Sets the Light CTL Temperature Range state of a Light CTL Setup Server without requesting a
 * response.
 *
 * This is an unacknowledged message, so no response is expected.
 *
 * @property min   Minimum temperature of the range in Kelvin.
 * @property max   Maximum temperature of the range in Kelvin.
 * @property range Range between [min] and [max], inclusive, as unsigned integers.
 */
class LightCTLTemperatureRangeSetUnacknowledged(
    val min: UShort,
    val max: UShort,
) : UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = min.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            max.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    val range: UIntRange
        get() = min.toUInt()..max.toUInt()

    /**
     * Convenience constructor to create a LightCTLTemperatureRangeSetUnacknowledged message from a
     * range.
     *
     * @param range Temperature range in Kelvin.
     */
    @Suppress("unused")
    constructor(range: UIntRange) : this(
        min = range.first.toUShort(),
        max = range.last.toUShort()
    )

    override fun toString() = "LightCTLTemperatureRangeSetUnacknowledged(min: $min, max: $max)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x826Cu

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 4 }?.let {
            LightCTLTemperatureRangeSetUnacknowledged(
                min = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                max = it.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}
