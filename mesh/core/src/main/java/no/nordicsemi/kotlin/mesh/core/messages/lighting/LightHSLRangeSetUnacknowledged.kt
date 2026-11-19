package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage

/**
 * Sets the Light HSL Range state of a Light HSL Setup Server without requesting a response.
 *
 * This is an unacknowledged message, so no response is expected.
 *
 * @property minHue          Minimum value of the Light HSL Hue Range state.
 * @property maxHue          Maximum value of the Light HSL Hue Range state.
 * @property minSaturation   Minimum value of the Light HSL Saturation Range state.
 * @property maxSaturation   Maximum value of the Light HSL Saturation Range state.
 * @property hueRange        Hue range between [minHue] and [maxHue], inclusive, as unsigned
 *                           integers.
 * @property saturationRange Saturation range between [minSaturation] and [maxSaturation],
 *                           inclusive, as unsigned integers.
 */
class LightHSLRangeSetUnacknowledged(
    val minHue: UShort,
    val maxHue: UShort,
    val minSaturation: UShort,
    val maxSaturation: UShort,
) : UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = minHue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            maxHue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            minSaturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            maxSaturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    val hueRange: UIntRange
        get() = minHue.toUInt()..maxHue.toUInt()

    val saturationRange: UIntRange
        get() = minSaturation.toUInt()..maxSaturation.toUInt()

    /**
     * Convenience constructor to create a LightHSLRangeSetUnacknowledged message from ranges.
     *
     * @param hueRange        Range of the Light HSL Hue Range state.
     * @param saturationRange Range of the Light HSL Saturation Range state.
     */
    @Suppress("unused")
    constructor(hueRange: UIntRange, saturationRange: UIntRange) : this(
        minHue = hueRange.first.toUShort(),
        maxHue = hueRange.last.toUShort(),
        minSaturation = saturationRange.first.toUShort(),
        maxSaturation = saturationRange.last.toUShort()
    )

    override fun toString() = "LightHSLRangeSetUnacknowledged(minHue: $minHue, maxHue: $maxHue, " +
            "minSaturation: $minSaturation, maxSaturation: $maxSaturation)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8282u

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 8 }?.let {
            LightHSLRangeSetUnacknowledged(
                minHue = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                maxHue = it.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                minSaturation = it.getUShort(offset = 4, order = ByteOrder.LITTLE_ENDIAN),
                maxSaturation = it.getUShort(offset = 6, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}
