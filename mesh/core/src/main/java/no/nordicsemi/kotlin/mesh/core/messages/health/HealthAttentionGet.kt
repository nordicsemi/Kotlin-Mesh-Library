package no.nordicsemi.kotlin.mesh.core.messages.health

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.HealthMessageInitializer

/**
 * A Health Attention Get is an acknowledged message used to get the current
 * Attention Timer state of an element.
 *
 * The Attention Timer is intended to allow an Element to attract human attention and, among others,
 * is used during provisioning.
 *
 * When the Attention Timer state is on, the value determines how long (in seconds) the Element shall
 * remain attracting human’s attention. The Element does that by behaving in a human-recognizable
 * way (e.g., a lamp flashes, a motor makes noise, an LED blinks). The exact behavior is implementation
 * specific and depends on the type of device.
 */
class HealthAttentionGet: AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = HealthAttentionStatus.opCode
    override val parameters = null

    companion object Initializer : HealthMessageInitializer {
        override val opCode = 0x8004u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { HealthAttentionGet() }
    }
}