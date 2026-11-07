package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransitionMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters

/**
 * Sets the Light LC Light OnOff state of a Light LC Server.
 *
 * The response received to the message is [LightLCLightOnOffStatus].
 *
 * @property tid              Defines a unique Transaction identifier that each message must have.
 * @property transitionTime   Defines the time interval an element will take to transition to the
 *                            target state from the present state.
 * @property delay            Message execution delay in 5 millisecond steps.
 * @property on               Defines the desired Light LC Light OnOff state.
 */
class LightLCLightOnOffSet(
    override var tid: UByte?,
    val on: Boolean,
    transitionParams: TransitionParameters? = null
) : AcknowledgedApplicationMessage, TransactionMessage, TransitionMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLCLightOnOffStatus.opCode
    override val parameters: ByteArray
        get() {
            val data = byteArrayOf(if (on) 0x01 else 0x00, tid!!.toByte())
            return when (transitionTime != null && delay != null) {
                true -> data + byteArrayOf(
                    transitionTime.rawValue.toByte(),
                    delay.toByte()
                )

                else -> data
            }
        }
    override val transitionTime = transitionParams?.transitionTime
    override val delay = transitionParams?.delay

    /**
     * Creates a Light LC Light OnOff Set message without a transaction ID or transition.
     *
     * The transaction ID is assigned when the message is sent.
     *
     * @param on Desired Light LC Light OnOff state.
     */
    @Suppress("unused")
    constructor(on: Boolean) : this(on = on, tid = null)

    /**
     * Creates a Light LC Light OnOff Set message with a transaction ID.
     *
     * No transition time or delay is used.
     *
     * @param on  Desired Light LC Light OnOff state.
     * @param tid Transaction ID.
     */
    @Suppress("unused")
    constructor(on: Boolean, tid: UByte) : this(on = on, tid = tid, transitionParams = null)

    override fun toString() = "LightLCLightOnOffSet(tid: $tid, on: $on" +
            if (transitionTime != null && delay != null) {
                ", transitionTime: $transitionTime, delay: ${delay.toInt() * 5} ms)"
            } else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x829Au

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 4 || it.size == 2
        }?.let { params ->
            LightLCLightOnOffSet(
                on = params[0] == 0x01.toByte(),
                tid = params[1].toUByte(),
                transitionParams = if (params.size == 4) TransitionParameters(
                    transitionTime = TransitionTime(rawValue = params[2].toUByte()),
                    delay = params[3].toUByte()
                ) else null
            )
        }
    }
}