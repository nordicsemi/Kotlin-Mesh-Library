@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.layers.access.AccessPdu

/**
 * Unknown message defines a message that may not be defined in any of the local Models for the
 * received opcode.
 *
 * @property opCode      Opcode of the message.
 * @property parameters  Parameters of the message.
 * @constructor Constructs an UnknownMessage that cannot be parsed by any of the local models.
 */
class UnknownMessage(
    override val opCode: UInt,
    override val parameters: ByteArray
) : MeshMessage {

    /**
     * Constructs an UnknownMessage that cannot be parsed by any of the local models.
     *
     * @param accessPdu Access PDU received.
     * @constructor Constructs an UnknownMessage that cannot be parsed by any of the local models.
     */
    internal constructor(accessPdu: AccessPdu) : this(
        opCode = accessPdu.opCode,
        parameters = accessPdu.parameters
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        return "UnknownMessage(opCode: ${opCode.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )}, parameters: 0x${parameters.toHexString(HexFormat.UpperCase)})"
    }
}