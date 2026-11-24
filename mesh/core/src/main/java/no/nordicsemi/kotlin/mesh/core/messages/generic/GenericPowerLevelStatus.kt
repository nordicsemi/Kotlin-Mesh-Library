package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Reports the current power state of a Generic Power Server.
 *
 * This message is sent in response to a [GenericPowerLevelGet], [GenericPowerLevelSet] or
 * [GenericPowerLevelSetUnacknowledged] message, or it may be sent unsolicited when the state
 * changes.
 *
 * @property power         Present value of the Generic Power state.
 * @property targetPower   Target value of the Generic Power state, or null if no transition is in
 *                         progress.
 * @property remainingTime Time that an element will take to transition to the target state from
 *                         the present state, or null if no transition is in progress.
 */
class GenericPowerLevelStatus(
    val power: UShort,
    val targetPower: UShort?,
    override val remainingTime: TransitionTime?,
) : ApplicationResponse, TransitionStatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = when (targetPower != null && remainingTime != null) {
        true -> power.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                targetPower.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                remainingTime.rawValue.toByteArray()

        else -> power.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Convenience constructor to create a GenericPowerLevelStatus message without a target state.
     *
     * @param power Present value of the Generic Power state.
     */
    @Suppress("unused")
    constructor(power: UShort) : this(
        power = power,
        targetPower = null,
        remainingTime = null
    )

    override fun toString() = "GenericPowerLevelStatus(power: $power" +
            if (targetPower != null && remainingTime != null) {
                ", targetPower: $targetPower, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8218u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 || it.size == 5 }
            ?.let { params ->
                GenericPowerLevelStatus(
                    power = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    targetPower = when (params.size) {
                        5 -> params.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
                        else -> null
                    },
                    remainingTime = when (params.size) {
                        5 -> TransitionTime(rawValue = params[4].toUByte())
                        else -> null
                    }
                )
            }
    }
}
