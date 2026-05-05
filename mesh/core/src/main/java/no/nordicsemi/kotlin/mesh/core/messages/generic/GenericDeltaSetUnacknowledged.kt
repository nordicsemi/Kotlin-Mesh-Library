package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.getInt
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
 * This message is used to set the current status of a GenericLevelServer model. Response received
 * to the message is [GenericLevelStatus].
 *
 * @property delta            Change in level.
 * @property transitionTime   Defines the time interval an element will take to transition to the
 *                            target state from the present state.
 * @property delay            Message execution delay in 5 millisecond steps.
 */
class GenericDeltaSetUnacknowledged(
    override var tid: UByte?,
    val delta: Int,
    val transitionParams: TransitionParameters? = null,
) : UnacknowledgedMeshMessage, TransactionMessage, TransitionMessage {
    override val opCode = Initializer.opCode
    override val transitionTime = transitionParams?.transitionTime
    override val continueTransaction = true
    override val delay = transitionParams?.delay
    override val parameters: ByteArray
        get() = when (transitionTime != null && delay != null) {
            true -> delta.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    tid!!.toByteArray() + transitionTime.rawValue.toByteArray() +
                    delay.toByteArray()

            else -> delta.toByteArray(order = ByteOrder.LITTLE_ENDIAN) + tid!!.toByteArray()
        }

    /**
     * Convenience constructor to create a GenericMoveSet message.
     *
     * @param percent          Level value in percent in the form of a float between 0 and 100%.
     */
    @Suppress("unused")
    constructor(percent: Float) : this(tid = null, percent = percent, transitionParams = null)

    /**
     * Convenience constructor to create a GenericMoveSet message.
     *
     * @param tid              Defines a unique Transaction identifier that each message must have.
     * @param percent          Level value in percent in the form of a float between 0 and 100%.
     * @param transitionParams Defines the transition parameters for the message.
     */
    constructor(
        tid: UByte?,
        percent: Float,
        transitionParams: TransitionParameters? = null,
    ) : this(
        tid = tid,
        delta = min(
            a = 65535,
            b = max(-65535, (655.35 * percent).toInt())
        ),
        transitionParams = transitionParams
    )

    /**
     * Convenience constructor to create a GenericDeltaSetUnacknowledged message without any
     * transition time, tid or delay.
     *
     * @param delta Current value of the Generic Level state.
     */
    @Suppress("unused")
    constructor(delta: Int) : this(delta = delta, tid = null)

    /**
     * Convenience constructor to create a GenericDeltaSetUnacknowledged message without any
     * transition time, tid or delay.
     *
     * @param delta Current value of the Generic Level state.
     * @param tid   Transaction ID.
     */
    @Suppress("unused")
    constructor(delta: Int, tid: UByte) : this(
        delta = delta,
        tid = tid,
        transitionParams = null
    )

    override fun toString() = "GenericDeltaSetUnacknowledged(tid: $tid, delta: $delta" +
            if (transitionTime != null && delay != null) {
                ", transitionTime: $transitionTime, delay: ${delay.toInt() * 5} ms)"
            } else ")"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x820Au

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 5 || it.size == 7
        }?.let { params ->
            GenericDeltaSetUnacknowledged(
                delta = params.getInt(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                tid = params[4].toUByte(),
                transitionParams = if (params.size == 7) TransitionParameters(
                    transitionTime = TransitionTime(rawValue = params[5].toUByte()),
                    delay = params[6].toUByte()
                ) else null
            )
        }
    }
}