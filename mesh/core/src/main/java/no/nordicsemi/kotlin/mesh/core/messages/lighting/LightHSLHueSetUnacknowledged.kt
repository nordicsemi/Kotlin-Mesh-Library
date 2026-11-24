package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransitionMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters

/**
 * Sets the Light HSL Hue state of a Light HSL Server without requesting a response.
 *
 * This is an unacknowledged message, so no response is expected.
 *
 * @property tid              Defines a unique Transaction identifier that each message must have.
 * @property hue              Target value of the Light HSL Hue state.
 * @property transitionTime   Defines the time interval an element will take to transition to the
 *                            target state from the present state.
 * @property delay            Message execution delay in 5 millisecond steps.
 */
class LightHSLHueSetUnacknowledged(
    override var tid: UByte?,
    val hue: UShort,
    transitionParams: TransitionParameters? = null,
) : UnacknowledgedApplicationMessage, TransactionMessage, TransitionMessage {
    override val opCode = Initializer.opCode
    override val transitionTime = transitionParams?.transitionTime
    override val delay = transitionParams?.delay
    override val continueTransaction = false
    override val parameters: ByteArray
        get() {
            val data = hue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    tid!!.toByteArray()
            return when (transitionTime != null && delay != null) {
                true -> data + transitionTime.rawValue.toByteArray() + delay.toByteArray()
                else -> data
            }
        }

    /**
     * Convenience constructor to create a LightHSLHueSetUnacknowledged message without a
     * transaction ID, transition time or delay.
     *
     * @param hue Target value of the Light HSL Hue state.
     */
    @Suppress("unused")
    constructor(hue: UShort) : this(tid = null, hue = hue)

    override fun toString() = "LightHSLHueSetUnacknowledged(tid: $tid, hue: $hue" +
            if (transitionTime != null && delay != null) {
                ", transitionTime: $transitionTime, delay: ${delay.toInt() * 5} ms)"
            } else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8270u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 5 || it.size == 3
        }?.let { params ->
            LightHSLHueSetUnacknowledged(
                hue = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                tid = params[2].toUByte(),
                transitionParams = if (params.size == 5) TransitionParameters(
                    transitionTime = TransitionTime(rawValue = params[3].toUByte()),
                    delay = params[4].toUByte()
                ) else null
            )
        }
    }
}
