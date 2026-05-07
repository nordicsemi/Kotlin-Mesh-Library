package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransitionMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * This as an unacknowledged message used to set the current status of a GenericLevelServer model.
 *
 * @property level            Defines a unique Transaction identifier that each message must have.
 * @property transitionTime   Defines the time interval an element will take to transition to the
 *                            target state from the present state.
 * @property delay            Message execution delay in 5 millisecond steps.
 */
class GenericLevelSetUnacknowledged(
    override var tid: UByte?,
    val level: Short,
    val transitionParams: TransitionParameters? = null,
) : UnacknowledgedMeshMessage, TransactionMessage, TransitionMessage {
    override val opCode = Initializer.opCode
    override val transitionTime = transitionParams?.transitionTime
    override val continueTransaction = true
    override val delay = transitionParams?.delay
    override val parameters: ByteArray
        get() = when (transitionTime != null && delay != null) {
            true -> level.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    tid!!.toByteArray() +
                    transitionTime.rawValue.toByteArray() +
                    delay.toByteArray()

            else -> level.toByteArray(order = ByteOrder.LITTLE_ENDIAN) + tid!!.toByteArray()
        }

    /**
     * Convenience constructor to create a GenericLevelSetUnacknowledged message without any
     * transition time, tid or delay.
     *
     * @param percent Level value in percent in the form of a float between 0 and 100 percent.
     */
    @Suppress("unused")
    constructor(percent: Float) : this(tid = null, percent = percent, transitionParams = null)

    /**
     * Convenience constructor to create a GenericLevelSetUnacknowledged message.
     *
     * @param tid              Defines a unique Transaction identifier that each message must have.
     * @param percent          Level value in percent in the form of a float between 0 and 1.
     * @param transitionParams Defines the transition parameters for the message.
     */
    constructor(
        tid: UByte?,
        percent: Float, //Level value in percent in the form of a float between 0 and 1
        transitionParams: TransitionParameters? = null,
    ) : this(
        tid = tid,
        level =  min(
            a = 32767,
            b = max(a = -32768, b = -32768 + (655.36 * percent).toInt())
        ).toShort(),
        transitionParams = transitionParams
    )

    /**
     * Convenience constructor to create a GenericLevelSetUnacknowledged message without any
     * transition time, tid or delay.
     *
     * @param level Current value of the Generic Level state.
     */
    @Suppress("unused")
    constructor(level: Short) : this(level = level, tid = null)

    /**
     * Convenience constructor to create a GenericLevelSetUnacknowledged message with a transaction
     * ID without any transition time or delay.
     *
     * @param level  Desired state of Generic OnOff Server.
     * @param tid Transaction ID.
     */
    @Suppress("unused")
    constructor(level: Short, tid: UByte) : this(level = level, tid = tid, transitionParams = null)

    override fun toString() = "GenericLevelSetUnacknowledged(tid: $tid, level: $level" +
            if (transitionTime != null && delay != null) {
                ", transitionTime: $transitionTime, delay: ${delay.toInt() * 5} ms)"
            } else ")"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8207u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 5 || it.size == 3
        }?.let { params ->
            GenericLevelSetUnacknowledged(
                level = params.getShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                tid = params[2].toUByte(),
                transitionParams = if (params.size == 5) TransitionParameters(
                    transitionTime = TransitionTime(rawValue = params[3].toUByte()),
                    delay = params[4].toUByte()
                ) else null
            )
        }
    }
}