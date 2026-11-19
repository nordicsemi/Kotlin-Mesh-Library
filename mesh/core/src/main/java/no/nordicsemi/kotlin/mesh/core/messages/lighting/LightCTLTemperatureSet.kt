package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransitionMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters

/**
 * Sets the Light CTL Temperature and Delta UV state of a Light CTL Temperature Server.
 *
 * The response to this message is a [LightCTLTemperatureStatus].
 *
 * @property tid            Defines a unique Transaction identifier that each message must have.
 * @property temperature    Temperature in Kelvin, coerced to 800 to 20000.
 * @property deltaUV        Delta UV value (signed).
 * @property transitionTime Defines the time interval an element will take to transition to the
 *                          target state from the present state.
 * @property delay          Message execution delay in 5 millisecond steps.
 */
class LightCTLTemperatureSet(
    override var tid: UByte?,
    temperature: UShort,
    val deltaUV: Short,
    transitionParams: TransitionParameters? = null,
) : AcknowledgedApplicationMessage, TransactionMessage, TransitionMessage {
    val temperature: UShort = temperature.coerceCtlTemperature()
    override val opCode = Initializer.opCode
    override val responseOpCode = LightCTLTemperatureStatus.opCode
    override val transitionTime = transitionParams?.transitionTime
    override val delay = transitionParams?.delay
    override val continueTransaction = false
    override val parameters: ByteArray
        get() {
            val data = temperature.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                deltaUV.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                tid!!.toByteArray()
            return when (transitionTime != null && delay != null) {
                true -> data + transitionTime.rawValue.toByteArray() + delay.toByteArray()
                else -> data
            }
        }

    /**
     * Convenience constructor to create a LightCTLTemperatureSet message without a transaction ID,
     * transition time or delay.
     *
     * @param temperature Temperature in Kelvin, coerced to 800 to 20000.
     * @param deltaUV Delta UV value (signed).
     */
    @Suppress("unused")
    constructor(temperature: UShort, deltaUV: Short) : this(
        tid = null,
        temperature = temperature,
        deltaUV = deltaUV
    )

    override fun toString() = "LightCTLTemperatureSet(tid: $tid, temperature: $temperature, deltaUV: $deltaUV" +
            if (transitionTime != null && delay != null) {
                ", transitionTime: $transitionTime, delay: ${delay.toInt() * 5} ms)"
            } else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8264u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 7 || it.size == 5
        }?.let { params ->
            LightCTLTemperatureSet(
                temperature = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                deltaUV = params.getShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                tid = params[4].toUByte(),
                transitionParams = if (params.size == 7) TransitionParameters(
                    transitionTime = TransitionTime(rawValue = params[5].toUByte()),
                    delay = params[6].toUByte()
                ) else null
            )
        }
    }
}
