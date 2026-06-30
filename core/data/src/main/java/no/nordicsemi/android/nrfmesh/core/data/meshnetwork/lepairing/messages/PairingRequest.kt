package no.nordicsemi.android.nrfmesh.core.data.meshnetwork.lepairing.messages

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedVendorMessage
import no.nordicsemi.kotlin.mesh.core.messages.VendorMessageInitializer

/**
 * This message is used to respond to a Pairing Request message.
 *
 * @property opCode The Op Code consists of:
 *                  0xD1-0000 - Vendor Op Code bitmask
 *                  0x59-0000 - The Op Code defined by...
 *                  0x00-5900 - Nordic Semiconductor ASA company ID (in Little Endian) as defined
 *                              here:
 *                              https://www.bluetooth.com/specifications/assigned-numbers/company-identifiers/
 */
class PairingRequest : AcknowledgedVendorMessage {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray
        get() = byteArrayOf(0x00)
    override val responseOpCode = PairingResponse.opCode

    companion object Initializer : VendorMessageInitializer {
        override val opCode: UInt = 0xD15900u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 && it[0] == 0x00.toByte() }
            ?.let { PairingRequest() }
    }
}