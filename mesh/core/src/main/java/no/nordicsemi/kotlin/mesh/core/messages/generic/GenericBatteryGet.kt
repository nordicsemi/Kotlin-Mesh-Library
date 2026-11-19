package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * This message is used to request the current status of a GenericBatteryServer model. Response
 * received to the message is [GenericBatteryStatus].
 */
class GenericBatteryGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericBatteryStatus.opCode
    override val parameters = null

    override fun toString() = "GenericBatteryGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8223u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { GenericBatteryGet() }
    }
}
