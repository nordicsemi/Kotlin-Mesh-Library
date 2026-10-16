package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer

/**
 * Firmware Distribution Receivers Delete All message is an acknowledged message sent by a
 * Firmware Distribution Client to remove all entries from the Distribution Receivers List state of
 * a Firmware Distribution Server.
 */
class FirmwareDistributionReceiversDeleteAll : AcknowledgedApplicationMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = FirmwareDistributionReceiversStatus.opCode
    override val parameters = null

    override fun toString() = "FirmwareDistributionReceiversDeleteAll()"

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8312u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { FirmwareDistributionReceiversDeleteAll() }
    }
}