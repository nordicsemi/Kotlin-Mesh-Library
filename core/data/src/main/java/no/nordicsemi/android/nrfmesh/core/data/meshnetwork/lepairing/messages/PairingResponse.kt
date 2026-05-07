package no.nordicsemi.android.nrfmesh.core.data.meshnetwork.lepairing.messages

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.VendorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.VendorResponse

/**
 * This message is used to respond to a Pairing Request message.
 *
 * @property status   Status of the pairing request.
 * @property passKey  24-bit Passkey assigned for the pairing process.
 *
 * @property opCode The Op Code consists of:
 *                  0xD1-0000 - Vendor Op Code bitmask
 *                  0x59-0000 - The Op Code defined by...
 *                  0x00-5900 - Nordic Semiconductor ASA company ID (in Little Endian) as defined
 *                              here:
 *                              https://www.bluetooth.com/specifications/assigned-numbers/company-identifiers/
 */
class PairingResponse(val status: UByte, val passKey: Int) : VendorResponse {
    override val opCode = Initializer.opCode
    override val parameters = byteArrayOf(0x01, status.toByte()) + passKey.toByteArray()

    override fun toString(): String {
        return "PairingResponse(status: $status, passKey: $passKey)"
    }

    companion object Initializer : VendorMessageInitializer {
        override val opCode: UInt = 0xD15900u // The same Op Code as PairingRequest!

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 5 && it[0] == 0x01.toByte() }
            ?.let { params ->
                PairingResponse(
                    status = params[1].toUByte(),
                    passKey = params.getInt(offset = 2, format = IntFormat.INT24)
                )
            }
    }
}