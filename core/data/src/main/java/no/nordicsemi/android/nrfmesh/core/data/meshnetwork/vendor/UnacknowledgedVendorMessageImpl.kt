package no.nordicsemi.android.nrfmesh.core.data.meshnetwork.vendor

import no.nordicsemi.kotlin.data.bigEndian
import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessageSecurity
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedVendorMessage
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId


/**
 * Implementation of Unacknowledged Vendor Message.
 *
 * @param modelId            Vendor Model Identifier.
 * @param vendorOpCode       6-bit opcode defined for the message.
 * @param parameters         Optional Parameters of the message.
 */
class UnacknowledgedVendorMessageImpl(
    modelId: VendorModelId,
    vendorOpCode: UByte,
    override val parameters: ByteArray?,
    override val isSegmented: Boolean = false,
    override val security: MeshMessageSecurity = MeshMessageSecurity.Low
) : UnacknowledgedVendorMessage {

    override val opCode =
        ((0xC0.toUByte() or vendorOpCode).toUInt() shl 16) or
                modelId.companyIdentifier.bigEndian.toUInt()

    override fun toString() = "UnacknowledgedVendorMessageImpl(" +
            "opCode: ${
                opCode.toHexString(
                    format = HexFormat {
                        number.prefix = "0x"
                        upperCase = true
                    }
                )
            }, " +
            "parameters: ${parameters?.toHexString(prefixOx = true)}, " +
            "isSegmented: $isSegmented, " +
            "security: $security)"
}