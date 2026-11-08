package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage

/**
 * Reports the present Light LC Mode state of a Light LC Server.
 *
 * This message is a response to a [LightLCModeGet], [LightLCModeSet] and
 * [LightLCModeSetUnacknowledged].
 *
 * @property controllerStatus Whether the controller is turned on and the binding with the Light
 *                            Lightness state is enabled.
 */
class LightLCModeStatus(
    val controllerStatus: Boolean,
) : ApplicationResponse, UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters = byteArrayOf(if (controllerStatus) 0x01 else 0x00)

    override fun toString() = "LightLCModeStatus(controllerStatus: $controllerStatus)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8294u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { LightLCModeStatus(controllerStatus = it[0] == 0x01.toByte()) }
    }
}
