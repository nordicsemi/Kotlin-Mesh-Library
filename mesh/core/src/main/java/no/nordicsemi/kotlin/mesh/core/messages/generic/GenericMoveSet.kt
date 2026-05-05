package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransitionMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * This message is used to set the current status of a GenericLevelServer model. Response received
 * to the message is [GenericLevelStatus].
 *
 * @property deltaLevel       Change in level.
 * @property transitionTime   Defines the time interval an element will take to transition to the
 *                            target state from the present state.
 * @property delay            Message execution delay in 5 millisecond steps.
 */
class GenericMoveSet(
    override var tid: UByte?,
    val deltaLevel: Short,
    val transitionParams: TransitionParameters? = null,
) : AcknowledgedMeshMessage, TransactionMessage, TransitionMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericLevelStatus.opCode
    override val transitionTime = transitionParams?.transitionTime
    override val continueTransaction = true
    override val delay = transitionParams?.delay
    override val parameters = when (transitionTime != null && delay != null) {
        true -> deltaLevel.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                tid!!.toByteArray() + transitionTime.rawValue.toByteArray() +
                delay.toByteArray()

        else -> deltaLevel.toByteArray(order = ByteOrder.LITTLE_ENDIAN) + tid!!.toByteArray()
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
        deltaLevel = min(
            a = 32767,
            b = max(32768, (327.67 * percent).toInt())
        ).toShort(),
        transitionParams = transitionParams
    )

    /**
     * Convenience constructor to create a GenericMoveSet message without any transition time,
     * tid or delay.
     *
     * @param deltaLevel Current value of the Generic Level state.
     */
    @Suppress("unused")
    constructor(deltaLevel: Short) : this(deltaLevel = deltaLevel, tid = null)

    /**
     * Convenience constructor to create a GenericMoveSet message without any transition time, tid
     * or delay.
     *
     * @param deltaLevel Current value of the Generic Level state.
     * @param tid        Transaction ID.
     */
    @Suppress("unused")
    constructor(deltaLevel: Short, tid: UByte) : this(
        deltaLevel = deltaLevel,
        tid = tid,
        transitionParams = null
    )

    override fun toString() = "GenericMoveSet(tid: $tid, deltaLevel: $deltaLevel" +
            if (transitionTime != null && delay != null) {
                ", transitionTime: $transitionTime, delay: ${delay.toInt() * 5} ms)"
            } else ")"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x820Bu

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 5 || it.size == 3
        }?.let { params ->
            GenericMoveSet(
                deltaLevel = params.getShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                tid = params[2].toUByte(),
                transitionParams = if (params.size == 5) TransitionParameters(
                    transitionTime = TransitionTime(rawValue = params[3].toUByte()),
                    delay = params[4].toUByte()
                ) else null
            )
        }
    }
}