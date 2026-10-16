package no.nordicsemi.android.nrfmesh.feature.dfu.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import no.nordicsemi.kotlin.mesh.core.util.CompanyIdentifier

/**
 * Mesh DFU metadata of a firmware image, as found in the `*.json` file of a distribution package.
 *
 * @property signVersion        The version of the firmware.
 * @property binarySize         Size of the binary, in octets. This is a 24-bit value.
 * @property coreType           The core the firmware is for.
 * @property compositionData    The Composition Data of the Node after the update, or `null` when
 *                              the metadata does not declare it.
 * @property compositionHash    The hash of the Composition Data after the update. This is
 *                              a 32-bit value.
 * @property metadataString    The vendor specific metadata, as a hexadecimal string, or `null`
 *                              when not present.
 * @property firmwareIdString         The Firmware ID, as a hexadecimal string.
 * @property metadataOctets           [metadataString] decoded into octets, or `null` when not present.
 * @property firmwareIdOctets   [firmwareIdString] decoded into octets.
 */
@Serializable
data class Metadata(
    @SerialName("sign_version") val signVersion: Version,
    @SerialName("binary_size") val binarySize: Int,
    @SerialName("core_type") val coreType: Int,
    @SerialName("composition_data") val compositionData: CompositionData? = null,
    @SerialName("composition_hash") val compositionHash: UInt,
    @SerialName("encoded_metadata") val metadataString: String? = null,
    @SerialName("firmware_id") val firmwareIdString: String,
) {

    /**
     * The version of a firmware image.
     *
     * @property major    The major version.
     * @property minor    The minor version.
     * @property revision The revision.
     * @property build    The build number.
     */
    @Serializable
    data class Version(
        val major: Int,
        val minor: Int,
        val revision: Int,
        @SerialName("build_number") val build: Long,
    ) {
        override fun toString() =
            if (build > 0) "$major.$minor.$revision+$build" else "$major.$minor.$revision"
    }

    /**
     * The Composition Data of a Node.
     *
     * @property companyIdentifier The 16-bit Company Identifier assigned by Bluetooth SIG.
     * @property productIdentifier The 16-bit vendor assigned Product Identifier.
     * @property versionIdentifier The 16-bit vendor assigned Version Identifier.
     * @property crpl              The minimum number of Replay Protection List entries.
     * @property features          The features supported by the Node, as a bit field.
     * @property elements          The Elements of the Node.
     */
    @Serializable
    data class CompositionData(
        @SerialName("cid") val companyIdentifier: UShort,
        @SerialName("pid") val productIdentifier: UShort,
        @SerialName("vid") val versionIdentifier: UShort,
        val crpl: UShort,
        val features: UShort,
        val elements: List<Element> = emptyList(),
    ) {
        val companyName : String
            get() = CompanyIdentifier.name(id = companyIdentifier) ?: "Unknown"

    }

    /**
     * An Element of a Node.
     *
     * @property location     The numeric location descriptor of the Element.
     * @property sigModels    The Model IDs of the SIG Models of the Element.
     * @property vendorModels The Model IDs of the vendor Models of the Element, where the two
     *                        most significant octets are the Company Identifier.
     */
    @Serializable
    data class Element(
        val location: UShort,
        @SerialName("sig_models") val sigModels: List<UShort> = emptyList(),
        @SerialName("vendor_models") val vendorModels: List<UInt> = emptyList(),
    )

    val metadataOctets: ByteArray?
        get() = metadataString?.hexToByteArray(format = HexFormat.UpperCase)

    val firmwareId: FirmwareId?
        get() = FirmwareId.from(data = firmwareIdOctets)

    val firmwareIdOctets: ByteArray
        get() = firmwareIdString.hexToByteArray(format = HexFormat.UpperCase)
}