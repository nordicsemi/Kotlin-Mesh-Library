package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse

/**
 * This message is the status message sent as a response to [ConfigBeaconGet] and [ConfigBeaconSet].
 *
 * @property isEnabled True if the beacon is enabled or false otherwise.
 */
@Suppress("MemberVisibilityCanBePrivate")
class ConfigBeaconStatus(val isEnabled: Boolean) : ConfigResponse {
    override val opCode = Initializer.opCode
    override val parameters = byteArrayOf(if (isEnabled) 0x01 else 0x00)

    override fun toString() = "ConfigBeaconStatus(isEnabled: $isEnabled)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x800Bu

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 1 && it.first() <= 1
        }?.let { params ->
            ConfigBeaconStatus(isEnabled = params.first().toInt() == 0x01)
        }
    }
}