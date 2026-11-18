package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransitionMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters

/**
 * Sets the Light HSL Saturation state of a Light HSL Server.
 *
 * The response to this message is a [LightHSLSaturationStatus].
 *
 * @property tid              Defines a unique Transaction identifier that each message must have.
 * @property saturation       Target value of the Light HSL Saturation state.
 * @property transitionTime   Defines the time interval an element will take to transition to the
 *                            target state from the present state.
 * @property delay            Message execution delay in 5 millisecond steps.
 */
class LightHSLSaturationSet(
    override var tid: UByte?,
    val saturation: UShort,
    transitionParams: TransitionParameters? = null,
) : AcknowledgedApplicationMessage, TransactionMessage, TransitionMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightHSLSaturationStatus.opCode
    override val transitionTime = transitionParams?.transitionTime
    override val delay = transitionParams?.delay
    override val continueTransaction = false
    override val parameters: ByteArray
        get() {
            val data = saturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    tid!!.toByteArray()
            return when (transitionTime != null && delay != null) {
                true -> data + transitionTime.rawValue.toByteArray() + delay.toByteArray()
                else -> data
            }
        }

    /**
     * Convenience constructor to create a LightHSLSaturationSet message without a transaction ID,
     * transition time or delay.
     *
     * @param saturation Target value of the Light HSL Saturation state.
     */
    @Suppress("unused")
    constructor(saturation: UShort) : this(tid = null, saturation = saturation)

    override fun toString() = "LightHSLSaturationSet(tid: $tid, saturation: $saturation" +
            if (transitionTime != null && delay != null) {
                ", transitionTime: $transitionTime, delay: ${delay.toInt() * 5} ms)"
            } else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8273u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 5 || it.size == 3
        }?.let { params ->
            LightHSLSaturationSet(
                saturation = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                tid = params[2].toUByte(),
                transitionParams = if (params.size == 5) TransitionParameters(
                    transitionTime = TransitionTime(rawValue = params[3].toUByte()),
                    delay = params[4].toUByte()
                ) else null
            )
        }
    }
}
