package no.nordicsemi.android.nrfmesh.core.data.meshnetwork.vendor

import no.nordicsemi.kotlin.data.bigEndian
import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedVendorMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessageSecurity
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId


/**
 * Implementation of Acknowledged Vendor Message.
 *
 * @param modelId                   Vendor Model Identifier.
 * @param vendorOpCode              6-bit opcode defined for the message.
 * @param vendorResponseOpCode      6-bit opcode defined for the response message.
 * @param parameters                Parameters for the message.
 * @param isSegmented               Determines whether the message is segmented.
 * @param security                  Security level of the message.
 */
class AcknowledgedVendorMessageImpl(
    modelId: VendorModelId,
    vendorOpCode: UByte,
    vendorResponseOpCode: UByte,
    override val parameters: ByteArray?,
    override val isSegmented: Boolean = false,
    override val security: MeshMessageSecurity = MeshMessageSecurity.Low,
) : AcknowledgedVendorMessage {

    override val opCode =
        ((0xC0.toUByte() or vendorOpCode).toUInt() shl 16) or
                modelId.companyIdentifier.bigEndian.toUInt()

    override val responseOpCode =
        ((0xC0u or vendorResponseOpCode.toUInt()) shl 16) or
                modelId.companyIdentifier.bigEndian.toUInt()

    override fun toString() = "AcknowledgedVendorMessage(" +
            "opCode: ${
                opCode.toHexString(
                    format = HexFormat {
                        number.prefix = "0x"
                        upperCase = true
                    }
                )
            }, " +
            "parameters: ${parameters?.toHexString(prefixOx = true)}, " +
            "responseOpCode: ${
                responseOpCode.toHexString(
                    format = HexFormat {
                        number.prefix = "0x"
                        upperCase = true
                    }
                )
            }, " +
            "isSegmented: $isSegmented, " +
            "security: $security)"
}