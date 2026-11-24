package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransitionMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters

/**
 * Sets the power state of a Generic Power Server.
 *
 * The response to this message is a [GenericPowerLevelStatus].
 *
 * @property tid              Defines a unique Transaction identifier that each message must have.
 * @property power            Target value of the Generic Power state.
 * @property transitionTime   Defines the time interval an element will take to transition to the
 *                            target state from the present state.
 * @property delay            Message execution delay in 5 millisecond steps.
 */
class GenericPowerLevelSet(
    override var tid: UByte?,
    val power: UShort,
    transitionParams: TransitionParameters? = null,
) : AcknowledgedApplicationMessage, TransactionMessage, TransitionMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericPowerLevelStatus.opCode
    override val transitionTime = transitionParams?.transitionTime
    override val delay = transitionParams?.delay
    override val continueTransaction = false
    override val parameters: ByteArray
        get() {
            val data = power.toByteArray(order = ByteOrder.LITTLE_ENDIAN) + tid!!.toByteArray()
            return when (transitionTime != null && delay != null) {
                true -> data + transitionTime.rawValue.toByteArray() + delay.toByteArray()
                else -> data
            }
        }

    /**
     * Convenience constructor to create a GenericPowerLevelSet message without a transaction ID,
     * transition time or delay.
     *
     * @param power Target value of the Generic Power state.
     */
    @Suppress("unused")
    constructor(power: UShort) : this(tid = null, power = power)

    override fun toString() = "GenericPowerLevelSet(tid: $tid, power: $power" +
            if (transitionTime != null && delay != null) {
                ", transitionTime: $transitionTime, delay: ${delay.toInt() * 5} ms)"
            } else ")"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8216u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 5 || it.size == 3
        }?.let { params ->
            GenericPowerLevelSet(
                power = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                tid = params[2].toUByte(),
                transitionParams = if (params.size == 5) TransitionParameters(
                    transitionTime = TransitionTime(rawValue = params[3].toUByte()),
                    delay = params[4].toUByte()
                ) else null
            )
        }
    }
}
