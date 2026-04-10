package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer

class FirmwareUpdateApply : AcknowledgedMeshMessage{
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = 0u
    override val parameters = null

    init {
        TODO("This has to be clarified")
    }

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x830Fu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { FirmwareUpdateApply() }
    }
}