package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Sets the Generic Power Range state of a Generic Power Setup Server.
 *
 * The response to this message is a [GenericPowerRangeStatus].
 *
 * @property min   Minimum value of the Generic Power Range state.
 * @property max   Maximum value of the Generic Power Range state.
 * @property range Range between [min] and [max], inclusive, as unsigned integers.
 */
class GenericPowerRangeSet(val min: UShort, val max: UShort) : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericPowerRangeStatus.opCode
    override val parameters: ByteArray = min.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            max.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    val range: UIntRange
        get() = min.toUInt()..max.toUInt()

    /**
     * Convenience constructor to create a GenericPowerRangeSet message from a range.
     *
     * @param range Range of the Generic Power Range state.
     */
    @Suppress("unused")
    constructor(range: UIntRange) : this(
        min = range.first.toUShort(),
        max = range.last.toUShort()
    )

    override fun toString() = "GenericPowerRangeSet(min: $min, max: $max)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8221u

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 4 }?.let {
            GenericPowerRangeSet(
                min = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                max = it.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}
