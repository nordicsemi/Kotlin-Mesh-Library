package no.nordicsemi.kotlin.mesh.core.messages

/**
 * A base interface for vendor messages.
 *
 * Vendor messages have 24-bit long Op Code, of which 16 least significant bits contain the Company
 * ID and 6 least significant bits of the most significant byte are the vendor Op Code.
 *
 * @property vendorOpCode The Op Code as defined by the company.
 * @property companyIdentifier The Company Identifiers are 16-bit values defined by the Bluetooth
 *                             SIG and are coded into the second and third octets of the 3-octet
 *                             Op Code.
 */
sealed interface VendorMessage : ApplicationMessage {

    /**
     * The Op Code as defined by the company.
     *
     * There are 64 3-octet Op Codes available per company identifier. Op Code is encoded in the 6
     * least significant bits of the most significant octet of the message Op Code.
     */
    val vendorOpCode: UByte
        get() = ((opCode shr 16) and 0x3Fu).toUByte()

    /**
     * The Company Identifiers are 16-bit values defined by the Bluetooth SIG and are coded into the
     * second and third octets of the 3-octet Op Code.
     */
    val companyIdentifier
        get() = (opCode and 0xFFFFu).toUShort()
}

/** A base interface for unacknowledged vendor message. */
interface UnacknowledgedVendorMessage : VendorMessage, UnacknowledgedApplicationMessage

/** The base interface for vendor response messages. */
interface VendorResponse : ApplicationResponse, UnacknowledgedVendorMessage

/** A base interface for acknowledged vendor message. */
interface AcknowledgedVendorMessage : VendorMessage, AcknowledgedApplicationMessage

/** A base interface for vendor status message. */
interface VendorStatusMessage : UnacknowledgedVendorMessage, StatusMessage

interface VendorMessageInitializer : BaseMeshMessageInitializer, HasOpCode