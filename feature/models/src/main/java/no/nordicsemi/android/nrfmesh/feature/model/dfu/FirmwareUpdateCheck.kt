package no.nordicsemi.android.nrfmesh.feature.model.dfu

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import no.nordicsemi.android.nrfmesh.core.common.UpdatedFirmwareInformation
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLDecoder
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection


/**
 * Checks for firmware updates.
 *
 * @param url URL to check for updates.
 * @return updated firmware information if there is an update available, null otherwise.
 */
internal suspend fun checkForUpdates(url: URL): UpdatedFirmwareInformation? = try {
    withContext(Dispatchers.IO) {
        val connection = (URL(url.toString()).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
        }

        try {
            when (val statusCode = connection.responseCode) {
                404 -> return@withContext null // Success - no update available.
                !in 200..299 -> throw Exception("Server returned error code: $statusCode")
            }

            val data = connection.inputStream.bufferedReader().use { it.readText() }
            Json.decodeFromString<UpdatedFirmwareInformation>(data)
        } finally {
            connection.disconnect()
        }
    }
} catch (e: Exception) {
    throw Exception("Failed to decode firmware information: ${e.localizedMessage}")
}

/**
 * Downloads the firmware from the given URL.
 *
 * @param context The application context.
 * @param url The URL to download the firmware from.
 * @param firmwareId The firmware ID.
 */
internal suspend fun downloadFirmware(context: Context, url: URL, firmwareId: FirmwareId): File {
    val updatedUrl = url.toString()
        .replace(oldValue = "192.168.0.173", newValue = "10.0.0.22")
        .toUri()
        .let { uri ->
            URI(
                uri.scheme,
                uri.authority,
                uri.path + "/get",
                "cfwid=${firmwareId.bytes.toHexString(format = HexFormat.UpperCase)}",
                null
            )
        }
        .toURL()
    return withContext(Dispatchers.IO) {
        val connection = (updatedUrl.openConnection() as HttpURLConnection).apply {
            if (this is HttpsURLConnection) {
                hostnameVerifier = HostnameVerifier { _, _ -> true }
            }
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
        }

        try {
            connection.connect()
            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                throw IOException("Server returned error code: $statusCode")
            }

            val fileName = connection.getHeaderField("Content-Disposition")
                ?.let { extractFileNameFromHeader(it) }
                ?: url.path.substringAfterLast('/')
                    .ifBlank { "firmware_${System.currentTimeMillis()}.zip" }

            val tempFile = File(context.cacheDir, fileName)
            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            throw Exception("Failed to download firmware: ${e.localizedMessage}")
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Saves the given zip file to the downloads' directory.
 *
 * @param context The application context.
 * @param zipFile The zip file to save.
 */
internal fun saveToDownloads(context: Context, zipFile: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, zipFile.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Failed to create Downloads entry")

        resolver.openOutputStream(uri)?.use { output ->
            zipFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
    } else {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        downloadsDir.mkdirs()
        zipFile.copyTo(File(downloadsDir, zipFile.name), overwrite = true)
    }
}


/**
 * Extracts the file name from the Content-Disposition header.
 */
private fun extractFileNameFromHeader(header: String): String? {
    // Try filename*= first (RFC 5987, takes priority over filename=)
    // Format: filename*=UTF-8''my%20file.zip
    header.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("filename*=", ignoreCase = true) }
        ?.removePrefix("filename*=")
        ?.let { encoded ->
            val parts = encoded.split("''", limit = 2)
            if (parts.size == 2) {
                return URLDecoder.decode(parts[1], parts[0].ifBlank { "UTF-8" })
            }
        }

    // Fall back to filename=
    // Format: filename="my file.zip" or filename=myfile.zip
    return header.split(";")
        .map { it.trim() }
        .firstOrNull { it.startsWith("filename=", ignoreCase = true) }
        ?.removePrefix("filename=")
        ?.trim('"')
}