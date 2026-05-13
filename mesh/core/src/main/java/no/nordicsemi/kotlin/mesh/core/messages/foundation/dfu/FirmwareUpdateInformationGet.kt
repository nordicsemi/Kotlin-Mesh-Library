package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer


/**
 * Firmware Update Information Get message is an acknowledged message used to get information about
 * the firmware images installed on a Node.
 *
 * @property firstIndex   First Index field shall indicate the first entry on the Firmware
 *                        Information List state of the Firmware Update Server to return in the
 *                        Firmware Update Information Status message.
 * @property entriesLimit Entries Limit field shall indicate the maximum number of Firmware
 *                        Information Entry fields to return in the Firmware Update Information
 *                        Status message.
 */
class FirmwareUpdateInformationGet(
    val firstIndex: UByte,
    val entriesLimit: UByte,
) : AcknowledgedMeshMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode = FirmwareUpdateStatus.opCode
    override val parameters = firstIndex.toByteArray() + entriesLimit.toByteArray()

    /**
     * Creates the Firmware Update Information Get message. This convenience constructor will only
     * request the total count of entries in the Firmware Information List state.
     */
    @Suppress("unused")
    constructor() : this(firstIndex = 0u, entriesLimit = 0u)

    override fun toString() =
        "FirmwareUpdateInformationGet(firstIndex: $firstIndex, entriesLimit: $entriesLimit)"

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8308u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let {
                FirmwareUpdateInformationGet(
                    firstIndex = it[0].toUByte(),
                    entriesLimit = it[1].toUByte()
                )
            }
    }
}