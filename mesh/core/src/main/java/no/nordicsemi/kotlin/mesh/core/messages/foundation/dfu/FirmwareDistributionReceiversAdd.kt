package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageInitializer
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.data.ByteOrder

/**
 * Firmware Distribution Receivers Add message is an acknowledged message sent by a Firmware
 * Distribution Client to add new entries to the Distribution Receivers List state of a given
 * Firmware Distribution Server.
 *
 * @property receivers List of receiver entries to be added to the Distribution Receivers List state.
 */
class FirmwareDistributionReceiversAdd(val receivers: List<Receiver>) :
    AcknowledgedApplicationMessage {
    override val opCode: UInt = Initializer.opCode
    override val responseOpCode: UInt = FirmwareDistributionReceiversStatus.opCode
    override val parameters: ByteArray
        get() = receivers.fold(ByteArray(0)) { data, receiver ->
            data + receiver.address.address
                .toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    receiver.imageIndex.toByteArray()
        }

    /**
     * Creates a [FirmwareDistributionReceiversAdd] message with a single receiver.
     *
     * @param receiver Receiver entry to be added to the Distribution Receivers List state.
     * @constructor Creates a [FirmwareDistributionReceiversAdd] message with a single receiver.
     */
    @Suppress("unused")
    constructor(receiver: Receiver) : this(listOf(receiver))

    override fun toString() = "FirmwareDistributionReceiversAdd(receivers: $receivers)"

    companion object Initializer : FirmwareDistributionMessageInitializer {
        override val opCode: UInt = 0x8311u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size % 3 == 0 }
            ?.let { params ->
                params
                val receivers = mutableListOf<Receiver>()
                var index = 0
                while (index < params.size) {
                    val address: Address =
                        params.getUShort(offset = index, order = ByteOrder.LITTLE_ENDIAN)
                    val imageIndex = params[index + 2]
                    receivers.add(Receiver(address = address, index = imageIndex.toUByte()))
                    index += 3
                }
                FirmwareDistributionReceiversAdd(receivers = receivers)
            }
    }

    /**
     * Receiver entry
     *
     * @property address    Unicast address of the receiver or the target node.
     * @property imageIndex Index of the firmware image in the Firmware Information List
     */
    data class Receiver(val address: UnicastAddress, val imageIndex: UByte) {
        constructor(address: Address, index: UByte) : this(
            address = UnicastAddress(address = address),
            imageIndex = index
        )

        override fun toString() =
            "Receiver(address: ${address.toHexString(addPrefix = true)}, imageIndex: $imageIndex)"
    }
}