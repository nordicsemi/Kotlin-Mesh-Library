package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer

/**
 * This message is used to set the current beacon feature state of the a mesh node. The response
 * received after sending this message is a [ConfigBeaconStatus] message.
 *
 * @property enable True to enable the beacon feature or false to disable it.
 */
@Suppress("MemberVisibilityCanBePrivate")
class ConfigBeaconSet(val enable: Boolean) : AcknowledgedConfigMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = ConfigBeaconStatus.opCode
    override val parameters: ByteArray = byteArrayOf(if (enable) 0x01 else 0x00)

    override fun toString() = "ConfigBeaconSet(enable: $enable)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x800Au

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 1 && it.first() <= 1
        }?.let { params ->
            ConfigBeaconStatus(isEnabled = params.first().toInt() == 0x01)
        }
    }
}