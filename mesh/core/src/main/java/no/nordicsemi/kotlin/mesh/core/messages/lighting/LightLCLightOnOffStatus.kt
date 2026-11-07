package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Reports the current Light LC Light OnOff state of a Light LC Server.
 *
 * This message is a response to a [LightLCLightOnOffGet], [LightLCLightOnOffSet] and
 * [LightLCLightOnOffSetUnacknowledged].
 *
 * @property isOn          Current value of the Light LC Light OnOff state.
 * @property targetState   Target value of the Light LC Light OnOff state.
 * @property remainingTime The time that an element will take to transition to the target state from
 *                         the present state.
 */
class LightLCLightOnOffStatus(
    val isOn: Boolean,
    override val remainingTime: TransitionTime?,
    val targetState: Boolean?,
) : ApplicationResponse, UnacknowledgedApplicationMessage, TransitionStatusMessage {
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
     * Creates a Light LC Light OnOff Status message with the current state only.
     *
     * The target state and remaining time are not included.
     *
     * @param isOn True if the Light LC Light OnOff state is on or false otherwise.
     */
    @Suppress("unused")
    constructor(isOn: Boolean) : this(remainingTime = null, isOn = isOn, targetState = null)

    override fun toString() = "LightLCLightOnOffStatus(isOn: $isOn" +
            if (targetState != null && remainingTime != null) {
                ", targetState: $targetState, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x829Cu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 || it.size == 3 }
            ?.let { params ->
                LightLCLightOnOffStatus(
                    isOn = params[0] == 0x01.toByte(),
                    targetState = if (params.size == 3) params[1] == 0x01.toByte() else null,
                    remainingTime = if (params.size == 3)
                        TransitionTime(rawValue = params[2].toUByte())
                    else null,
                )
            }
    }
}