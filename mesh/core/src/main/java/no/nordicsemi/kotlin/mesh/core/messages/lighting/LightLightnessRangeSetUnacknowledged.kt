package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Sets the Light Lightness Range state of a Light Lightness Setup Server without requesting a response.
 *
 * This is an unacknowledged message, so no response is expected.
 *
 * @property min   Minimum value of the Light Lightness Range state.
 * @property max   Maximum value of the Light Lightness Range state.
 * @property range Range between [min] and [max], inclusive, as unsigned integers.
 */
class LightLightnessRangeSetUnacknowledged(val min: UShort, val max: UShort) : UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = min.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            max.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    val range: UIntRange
        get() = min.toUInt()..max.toUInt()

    /**
     * Convenience constructor to create a LightLightnessRangeSetUnacknowledged message from a range.
     *
     * @param range Range of the Light Lightness Range state.
     */
    @Suppress("unused")
    constructor(range: UIntRange) : this(
        min = range.first.toUShort(),
        max = range.last.toUShort()
    )

    override fun toString() = "LightLightnessRangeSetUnacknowledged(min: $min, max: $max)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x825Cu

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 4 }?.let {
            LightLightnessRangeSetUnacknowledged(
                min = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                max = it.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}
