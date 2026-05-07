package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * This message is a response to a [GenericOnOffGet] and [GenericOnOffSet] that contains the current
 * status of a GenericOnOffServer.
 *
 * @property isOn          Current value of the Generic OnOff state.
 * @property targetState   Target value of the Generic OnOff state.
 * @property remainingTime The time that an element will take to transition to the target state from
 *                         the present state.
 */
class GenericOnOffStatus(
    val isOn: Boolean,
    override val remainingTime: TransitionTime?,
    val targetState: Boolean?,
) : MeshResponse, UnacknowledgedMeshMessage, TransitionStatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() {
            val data = byteArrayOf(if (isOn) 0x01 else 0x00)
            return when (targetState != null && remainingTime != null) {
                true -> data + byteArrayOf(
                    if (targetState) 0x01 else 0x00,
                    remainingTime.rawValue.toByte()
                )

                else -> data
            }
        }

    /**
     * Convenience constructor to create a GenericOnOffStatus
     *
     * @param isOn True if the GenericServer state is on or false otherwise.
     */
    @Suppress("unused")
    constructor(isOn: Boolean) : this(remainingTime = null, isOn = isOn, targetState = null)

    override fun toString() = "GenericOnOffStatus(isOn: $isOn" +
            if (targetState != null && remainingTime != null) {
                ", targetState: $targetState, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8204u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 || it.size == 3 }
            ?.let { params ->
                GenericOnOffStatus(
                    isOn = params[0] == 0x01.toByte(),
                    targetState = if (params.size == 3) params[1] == 0x01.toByte() else null,
                    remainingTime = if (params.size == 3)
                        TransitionTime(rawValue = params[2].toUByte())
                    else null,
                )
            }
    }
}