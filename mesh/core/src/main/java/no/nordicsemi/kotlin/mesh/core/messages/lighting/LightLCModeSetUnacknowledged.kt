package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage

/**
 * This message is used to set the Light LC Mode state of a Light LC Server without expecting a
 * response.
 *
 * @property controllerStatus Whether the controller is turned on and the binding with the Light Lightness state is enabled.
 */
class LightLCModeSetUnacknowledged(
    val controllerStatus: Boolean,
) : UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters = byteArrayOf(if (controllerStatus) 0x01 else 0x00)

    override fun toString() = "LightLCModeSetUnacknowledged(controllerStatus: $controllerStatus)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8293u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { LightLCModeSetUnacknowledged(controllerStatus = it[0] == 0x01.toByte()) }
    }
}
