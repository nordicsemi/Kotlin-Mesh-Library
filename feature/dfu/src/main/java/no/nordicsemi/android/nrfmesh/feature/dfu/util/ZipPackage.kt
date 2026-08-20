package no.nordicsemi.android.nrfmesh.feature.dfu.util

import io.runtime.mcumgr.dfu.mcuboot.model.ImageSet
import io.runtime.mcumgr.dfu.mcuboot.model.TargetImage
import io.runtime.mcumgr.dfu.suit.model.CacheImageSet
import io.runtime.mcumgr.exception.McuMgrException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import no.nordicsemi.android.nrfmesh.feature.dfu.util.ZipPackage.Companion.NO_SLOT
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipInputStream

/**
 * Parses a DFU ZIP package: reads `manifest.json` and keeps the `.bin` / `.suit`
 * images in memory, keyed by file name.
 *
 * @param data the raw contents of the ZIP file.
 * @throws IOException if the ZIP is malformed, or the manifest is missing or invalid.
 */
class ZipPackage(data: ByteArray) {

    @Serializable
    private data class Manifest(
        @SerialName("format_version") val formatVersion: Int = 0,
        val files: List<Entry> = emptyList(),
    ) {
        @Serializable
        data class Entry(
            /** The file type. Expected values: "application", "bin", "suit-envelope", "cache", "mcuboot". */
            val type: String,

            /** The name of the image file. */
            val file: String,

            /**
             * The declared size of the image file in bytes. Does not have to equal the actual
             * file size.
             */
            val size: Int = 0,

            /**
             * Image index, used for multicore devices. Index 0 is the main core (app core),
             * index 1 the secondary core (net core), etc.
             *
             * Absent from the manifest on single-core devices, in which case it defaults to 0.
             */
            @SerialName("image_index") val imageIndex: Int = 0,

            /**
             * The slot number the image is to be sent to. By default, images are sent to the
             * secondary slot and swapped to the primary slot after the image is confirmed and
             * the device is reset.
             *
             * Devices supporting Direct XIP can run an app from the secondary slot; the image
             * must be compiled for that slot. A ZIP may contain images for both slots, and only
             * the one targeting the available slot is sent.
             *
             * [NO_SLOT] means "not set in the manifest" — see [getBinaries].
             *
             * @since NCS v2.5, nRF Connect Device Manager 1.8.
             */
            val slot: Int = NO_SLOT,

            /** The target partition ID. Valid for files of type `cache`. */
            val partition: Int = 0,
        )
    }

    private val manifest: Manifest
    private val entries = mutableMapOf<String, ByteArray>()

    init {
        var manifest: Manifest? = null

        // Unzip the file and look for the manifest.json.
        ZipInputStream(ByteArrayInputStream(data)).use { zis ->
            while (true) {
                val ze = zis.nextEntry ?: break
                if (ze.isDirectory) throw IOException("Invalid ZIP")

                val name = validateFilename(ze.name, ".")
                when {
                    name == MANIFEST -> {
                        // ZipInputStream.read() returns -1 at the end of the current entry,
                        // so readBytes() reads exactly this entry.
                        val json = zis.readBytes().decodeToString()
                        manifest = try {
                            JSON.decodeFromString<Manifest>(json)
                        } catch (e: SerializationException) {
                            throw IOException("Invalid $MANIFEST", e)
                        }
                    }
                    name.endsWith(".bin") || name.endsWith(".suit") -> {
                        entries[name] = zis.readBytes()
                    }
                    else -> {
                        // Timber.w("Unsupported file found: %s", name)
                    }
                }
            }
        }

        this.manifest = manifest ?: throw IOException("Invalid ZIP: $MANIFEST not found")
    }

    /** All images declared in the manifest, or an empty set if there are none. */
    @Throws(IOException::class, McuMgrException::class)
    fun getBinaries(): ImageSet = getBinaries(null) ?: ImageSet()

    /** Images of type `mcuboot`, or null if the ZIP contains none. */
    @Throws(IOException::class, McuMgrException::class)
    fun getMcuBootBinaries(): ImageSet? = getBinaries("mcuboot")

    @Throws(IOException::class, McuMgrException::class)
    private fun getBinaries(type: String?): ImageSet? {
        var binaries: ImageSet? = null

        // Search for images.
        var i = 0
        for (file in manifest.files) {
            if (type != null && type != file.type) continue

            val content = entries[file.file] ?: throw IOException("File not found: ${file.file}")

            if (binaries == null) binaries = ImageSet()

            var slot = file.slot
            // If slot wasn't set in the JSON, fall back to the default.
            if (slot < 0) {
                slot = if (file.type == "mcuboot") {
                    // Before nRF Connect SDK 3.0 the slot was not given for mcuboot updates.
                    // Instead, slots were assigned in order of appearance in the manifest.
                    i++
                } else {
                    // By default, send the image to the secondary slot, even though it is later
                    // swapped to the primary slot.
                    TargetImage.SLOT_SECONDARY
                }
            }
            binaries.add(TargetImage(file.imageIndex, slot, content))
        }
        return binaries
    }

    /**
     * The SUIT envelope, or null if not present in the ZIP.
     *
     * Valid only for SUIT updates using the SUIT manager.
     */
    fun getSuitEnvelope(): ByteArray? {
        // First, search for an entry of type "suit-envelope".
        manifest.files.firstOrNull { it.type == "suit-envelope" }
            ?.let { return entries[it.file] }
        // If not found, search for a file with the ".suit" extension.
        manifest.files.firstOrNull { it.file.endsWith(".suit") }
            ?.let { return entries[it.file] }
        // Not found.
        return null
    }

    /**
     * Raw cache images, which are sent to the device together with the SUIT envelope before
     * the update process starts, and are stored in the cache partitions.
     *
     * @return the cache images, or null if not present in the ZIP.
     * @throws IOException if at least one of the cache images is missing.
     */
    @Throws(IOException::class)
    fun getCacheBinaries(): CacheImageSet? {
        var cache: CacheImageSet? = null

        // Search for images.
        for (file in manifest.files) {
            if (file.type != "cache") continue

            val content = entries[file.file] ?: throw IOException("File not found: ${file.file}")

            if (cache == null) cache = CacheImageSet()
            cache.add(file.partition, content)
        }
        return cache
    }

    fun getResource(name: String): ByteArray? = entries[name]

    /**
     * Validates the path (not the content) of a zip entry to prevent path traversal issues.
     *
     * When unzipping an archive, always validate the compressed files' paths and reject any path
     * containing a path traversal (such as `../..`). Simply looking for `..` in the path may not
     * be enough. If the name is invalid, the entire extraction is aborted.
     *
     * @param filename the path to the file.
     * @param intendedDir the intended directory the zip should be extracted into.
     * @return the validated path to the file.
     * @throws IOException in case of path traversal issues.
     */
    @Throws(IOException::class)
    private fun validateFilename(filename: String, intendedDir: String): String {
        val canonicalPath = File(filename).canonicalPath
        val canonicalId = File(intendedDir).canonicalPath

        if (!canonicalPath.startsWith(canonicalId)) {
            throw IllegalStateException("File is outside extraction target directory.")
        }
        return canonicalPath.substring(1) // remove leading "/"
    }

    private companion object {
        const val MANIFEST = "manifest.json"
        const val NO_SLOT = -1

        val JSON = Json {
            ignoreUnknownKeys = true
        }
    }
}