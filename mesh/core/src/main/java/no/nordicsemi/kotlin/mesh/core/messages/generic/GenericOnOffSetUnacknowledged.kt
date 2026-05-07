package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransitionMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters

/**
 * This message is used to set the current status of a GenericOnOffServer model. Response received
 * to the message is [GenericOnOffStatus].
 *
 * @property tid              Defines a unique Transaction identifier that each message must have.
 * @property transitionTime   Defines the time interval an element will take to transition to the
 *                            target state from the present state.
 * @property delay            Message execution delay in 5 millisecond steps.
 * @property on               Defines the desired state of the Generic OnOff Server.
 */
class GenericOnOffSetUnacknowledged(
    override var tid: UByte?,
    val on: Boolean,
    transitionParams: TransitionParameters? = null,
) : UnacknowledgedMeshMessage, TransactionMessage, TransitionMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() {
            val data = byteArrayOf(if (on) 0x01 else 0x00, tid!!.toByte())
            return when (transitionTime != null && delay != null) {
                true -> data + byteArrayOf(transitionTime.rawValue.toByte(), delay.toByte())

                else -> data
            }
        }
    override val transitionTime = transitionParams?.transitionTime
    override val delay = transitionParams?.delay

    /**
     * Convenience constructor to create a GenericOnOffSet message without any transition time,
     * tid or delay.
     *
     * @param on Desired state of Generic OnOff Server.
     */
    @Suppress("unused")
    constructor(on: Boolean) : this(on = on, tid = null)

    /**
     * Convenience constructor to create a GenericOnOffSet message with a transaction ID without any
     * transition time or delay.
     *
     * @param on  Desired state of Generic OnOff Server.
     * @param tid Transaction ID.
     */
    @Suppress("unused")
    constructor(on: Boolean, tid: UByte) : this(on = on, tid = tid, transitionParams = null)

    override fun toString() = "GenericOnOffSetUnacknowledged(tid: $tid, on: $on" +
            if (transitionTime != null && delay != null) {
                ", transitionTime: $transitionTime, delay: ${delay.toInt() * 5} ms)"
            } else ")"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8203u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 4 || it.size == 2
        }?.let { params ->
            GenericOnOffSetUnacknowledged(
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