@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A Health Attention Set is an acknowledged message used to set the current
 * Attention Timer state of an element.
 *
 * The Attention Timer is intended to allow an Element to attract human attention and, among others,
 * is used during provisioning.
 *
 * When the Attention Timer state is on, the value determines how long (in seconds) the Element shall
 * remain attracting human’s attention. The Element does that by behaving in a human-recognizable
 * way (e.g., a lamp flashes, a motor makes noise, an LED blinks). The exact behavior is implementation
 * specific and depends on the type of device.
 *
 * @property attentionTimer The current Attention Timer value, in seconds.
 */
class HealthAttentionSet(
    val attentionTimer: UByte
): AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = HealthAttentionStatus.opCode
    override val parameters = byteArrayOf(attentionTimer.toByte())

    /**
     * The duration of the Attention Timer.
     */
    val duration: Duration
        get() = attentionTimer.toInt().seconds

    /**
     * Creates [HealthAttentionSet] message with the given duration.
     *
     * The duration will be truncated to whole seconds.
     */
    constructor(duration: Duration) : this(attentionTimer = duration.inWholeSeconds.toUByte())

    /**
     * Creates [HealthAttentionSet] message that disables the Attention Timer.
     */
    constructor() : this(attentionTimer = 0u)

    override fun toString() = "HealthAttentionSetAcknowledged(attentionTimer: $attentionTimer)"

    companion object Initializer: HealthMessageInitializer {
        override val opCode = 0x8005u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { params ->
                HealthAttentionSet(params[0].toUByte())
            }
    }
}