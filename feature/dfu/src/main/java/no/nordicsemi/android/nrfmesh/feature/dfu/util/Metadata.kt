package no.nordicsemi.android.nrfmesh.feature.dfu.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import java.io.File
import java.io.IOException
import java.io.InputStream

@Serializable
data class Metadata(
    @SerialName("sign_version") val signVersion: Version,
    /** Size of the binary, 24-bit. */
    @SerialName("binary_size") val binarySize: Int,
    @SerialName("core_type") val coreType: Int,
    @SerialName("composition_hash") val compositionDataHash: Int,
    @SerialName("encoded_metadata") val metadataString: String? = null,
    @SerialName("firmware_id") val firmwareIdString: String,
) {

    @Serializable
    data class Version(
        val major: Int,
        val minor: Int,
        val revision: Int,
        @SerialName("build_number") val build: Long,
    ) {
        override fun toString(): String =
            if (build > 0) "$major.$minor.$revision+$build"
            else "$major.$minor.$revision"
    }

    /** The decoded metadata, or null if not present. */
    val metadata: ByteArray?
        get() = metadataString?.decodeHex()

    /** The parsed Firmware ID, or null if it cannot be parsed. */
    val firmwareId: FirmwareId?
        get() = FirmwareId.from(firmwareIdString.decodeHex())

    companion object {
        private val JSON = Json {
            ignoreUnknownKeys = true
        }

        /** Decodes metadata from a JSON file. */
        @Throws(IOException::class)
        fun decode(file: File): Metadata = decode(file.readText())

        /** Decodes metadata from a stream. The stream is *not* closed. */
        @Throws(IOException::class)
        fun decode(input: InputStream): Metadata = decode(input.readBytes().decodeToString())

        /** Decodes metadata from a JSON string. */
        @Throws(IOException::class)
        fun decode(json: String): Metadata = try {
            JSON.decodeFromString<Metadata>(json)
        } catch (e: SerializationException) {
            throw IOException("Invalid metadata", e)
        }
    }
}

/**
 * File name, metadata, manifest and images of a selected firmware package.
 *
 * @property name File name.
 * @property metadata Mesh DFU Metadata of the selected firmware.
 * @property manifest MCU Manager Manifest of the selected firmware.
 * @property images Firmware images.
 */
// data class UpdatePackage(
//     /** File name. */
//     val name: String,
//     /** Mesh DFU Metadata of the selected firmware. */
//     val metadata: Metadata,
//     /** MCU Manager Manifest of the selected firmware. */
//     val manifest: McuMgrManifest,
//     /** Firmware images. */
//     val images: List<ImageManager.Image>,
// )

/**
 * Decodes a hexadecimal string into a byte array.
 *
 * Accepts an optional "0x" prefix. Returns an empty array for malformed input, mirroring
 * the lenient behaviour of `Data(hex:)`.
 */
private fun String.decodeHex(): ByteArray {
    val hex = removePrefix("0x").removePrefix("0X")
    if (hex.length % 2 != 0) return ByteArray(0)
    return ByteArray(hex.length / 2) { i ->
        val hi = Character.digit(hex[i * 2], 16)
        val lo = Character.digit(hex[i * 2 + 1], 16)
        if (hi < 0 || lo < 0) return ByteArray(0)
        ((hi shl 4) or lo).toByte()
    }
}
