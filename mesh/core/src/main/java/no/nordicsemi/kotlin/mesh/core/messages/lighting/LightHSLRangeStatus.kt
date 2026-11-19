package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.RangeMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.StatusMessage

/**
 * Reports the Light HSL Range state of a Light HSL Server.
 *
 * This message is sent in response to a [LightHSLRangeGet], [LightHSLRangeSet] or
 * [LightHSLRangeSetUnacknowledged] message.
 *
 * @property status          Status code of the last operation.
 * @property minHue          Minimum value of the Light HSL Hue Range state.
 * @property maxHue          Maximum value of the Light HSL Hue Range state.
 * @property minSaturation   Minimum value of the Light HSL Saturation Range state.
 * @property maxSaturation   Maximum value of the Light HSL Saturation Range state.
 * @property hueRange        Hue range between [minHue] and [maxHue], inclusive, as unsigned
 *                           integers.
 * @property saturationRange Saturation range between [minSaturation] and [maxSaturation],
 *                           inclusive, as unsigned integers.
 */
class LightHSLRangeStatus(
    val status: RangeMessageStatus,
    val minHue: UShort,
    val maxHue: UShort,
    val minSaturation: UShort,
    val maxSaturation: UShort,
) : ApplicationResponse, StatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = status.value.toByteArray() +
            minHue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            maxHue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            minSaturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            maxSaturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    override val isSuccess: Boolean
        get() = status == RangeMessageStatus.SUCCESS
    override val message: String
        get() = status.description

    val hueRange: UIntRange
        get() = minHue.toUInt()..maxHue.toUInt()

    val saturationRange: UIntRange
        get() = minSaturation.toUInt()..maxSaturation.toUInt()

    /**
     * Convenience constructor to report a successful operation with the given ranges.
     *
     * @param hueRange        Range of the Light HSL Hue Range state.
     * @param saturationRange Range of the Light HSL Saturation Range state.
     */
    @Suppress("unused")
    constructor(hueRange: UIntRange, saturationRange: UIntRange) : this(
        status = RangeMessageStatus.SUCCESS,
        minHue = hueRange.first.toUShort(),
        maxHue = hueRange.last.toUShort(),
        minSaturation = saturationRange.first.toUShort(),
        maxSaturation = saturationRange.last.toUShort()
    )

    /**
     * Convenience constructor to create a response to a [LightHSLRangeSet] request.
     *
     * @param status  Status code of the operation.
     * @param request Request the status is a response to.
     */
    @Suppress("unused")
    constructor(status: RangeMessageStatus, request: LightHSLRangeSet) : this(
        status = status,
        minHue = request.minHue,
        maxHue = request.maxHue,
        minSaturation = request.minSaturation,
        maxSaturation = request.maxSaturation
    )

    /**
     * Convenience constructor to create a response to a [LightHSLRangeSetUnacknowledged] request.
     *
     * @param status  Status code of the operation.
     * @param request Request the status is a response to.
     */
    @Suppress("unused")
    constructor(status: RangeMessageStatus, request: LightHSLRangeSetUnacknowledged) : this(
        status = status,
        minHue = request.minHue,
        maxHue = request.maxHue,
        minSaturation = request.minSaturation,
        maxSaturation = request.maxSaturation
    )

    override fun toString() = "LightHSLRangeStatus(status: $status, minHue: $minHue, " +
            "maxHue: $maxHue, " +
            "minSaturation: $minSaturation, maxSaturation: $maxSaturation)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x827Eu

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 9 }?.let {
            RangeMessageStatus.from(value = it[0].toUByte())?.let { status ->
                LightHSLRangeStatus(
                    status = status,
                    minHue = it.getUShort(offset = 1, order = ByteOrder.LITTLE_ENDIAN),
                    maxHue = it.getUShort(offset = 3, order = ByteOrder.LITTLE_ENDIAN),
                    minSaturation = it.getUShort(offset = 5, order = ByteOrder.LITTLE_ENDIAN),
                    maxSaturation = it.getUShort(offset = 7, order = ByteOrder.LITTLE_ENDIAN)
                )
            }
        }
    }
}
