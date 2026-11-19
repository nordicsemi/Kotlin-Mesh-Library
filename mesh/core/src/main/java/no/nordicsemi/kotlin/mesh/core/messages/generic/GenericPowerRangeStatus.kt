package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.RangeMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.StatusMessage

/**
 * Reports the Generic Power Range state of a Generic Power Server.
 *
 * This message is sent in response to a [GenericPowerRangeGet], [GenericPowerRangeSet] or
 * [GenericPowerRangeSetUnacknowledged] message.
 *
 * @property status Status code of the last operation.
 * @property min    Minimum value of the Generic Power Range state.
 * @property max    Maximum value of the Generic Power Range state.
 * @property range  Range between [min] and [max], inclusive, as unsigned integers.
 */
class GenericPowerRangeStatus(
    val status: RangeMessageStatus,
    val min: UShort,
    val max: UShort,
) : ApplicationResponse, StatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = status.value.toByteArray() +
            min.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            max.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    override val isSuccess: Boolean
        get() = status == RangeMessageStatus.SUCCESS
    override val message: String
        get() = status.description

    val range: UIntRange
        get() = min.toUInt()..max.toUInt()

    /**
     * Convenience constructor to report a successful operation with the given range.
     *
     * @param range Range of the Generic Power Range state.
     */
    @Suppress("unused")
    constructor(range: UIntRange) : this(
        status = RangeMessageStatus.SUCCESS,
        min = range.first.toUShort(),
        max = range.last.toUShort()
    )

    /**
     * Convenience constructor to create a response to a [GenericPowerRangeSet] request.
     *
     * @param status  Status code of the operation.
     * @param request Request the status is a response to.
     */
    @Suppress("unused")
    constructor(status: RangeMessageStatus, request: GenericPowerRangeSet) : this(
        status = status,
        min = request.min,
        max = request.max
    )

    /**
     * Convenience constructor to create a response to a [GenericPowerRangeSetUnacknowledged]
     * request.
     *
     * @param status  Status code of the operation.
     * @param request Request the status is a response to.
     */
    @Suppress("unused")
    constructor(status: RangeMessageStatus, request: GenericPowerRangeSetUnacknowledged) : this(
        status = status,
        min = request.min,
        max = request.max
    )

    override fun toString() = "GenericPowerRangeStatus(status: $status, min: $min, max: $max)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x821Eu

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 5 }?.let {
            RangeMessageStatus.from(value = it[0].toUByte())?.let { status ->
                GenericPowerRangeStatus(
                    status = status,
                    min = it.getUShort(offset = 1, order = ByteOrder.LITTLE_ENDIAN),
                    max = it.getUShort(offset = 3, order = ByteOrder.LITTLE_ENDIAN)
                )
            }
        }
    }
}
