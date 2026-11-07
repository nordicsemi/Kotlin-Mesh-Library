package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Sets the Light LC Mode state of a Light LC Server.
 *
 * The response received to the message is [LightLCModeStatus].
 *
 * @property controllerStatus Whether the controller is turned on and the binding with the Light
 *                            Lightness state is enabled.
 */
class LightLCModeSet(
    val controllerStatus: Boolean,
) : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLCModeStatus.opCode
    override val parameters = byteArrayOf(if (controllerStatus) 0x01 else 0x00)

    override fun toString() = "LightLCModeSet(controllerStatus: $controllerStatus)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8292u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { LightLCModeSet(controllerStatus = it[0] == 0x01.toByte()) }
    }
}
