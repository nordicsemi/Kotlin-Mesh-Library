@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A Health Attention Status is an unacknowledged message used to report the current
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
class HealthAttentionStatus(
    val attentionTimer: UByte
): MeshResponse, UnacknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters = byteArrayOf(attentionTimer.toByte())

    /**
     * The remaining duration of the Attention Timer.
     *
     * Set to [Duration.ZERO] when the timer is disabled.
     */
    val duration: Duration
        get() = attentionTimer.toInt().seconds

    /**
     * Whether the Attention Timer is enabled.
     */
    val isOn: Boolean
        get() = attentionTimer > 0u

    /**
     * Creates [HealthAttentionStatus] message with the given duration.
     *
     * The duration will be truncated to whole seconds.
     */
    constructor(duration: Duration) : this(attentionTimer = duration.inWholeSeconds.toUByte())

    /**
     * Creates [HealthAttentionStatus] message indicating the Attention Timer is disabled.
     */
    constructor() : this(attentionTimer = 0u)

    override fun toString() = "HealthAttentionStatus(attentionTimer: $attentionTimer)"

    companion object Initializer: HealthMessageInitializer {
        override val opCode = 0x8007u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { params ->
                HealthAttentionStatus(params[0].toUByte())
            }
    }
}