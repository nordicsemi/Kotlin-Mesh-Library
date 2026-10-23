@file:Suppress("unused", "UnusedReceiverParameter")

package no.nordicsemi.kotlin.mesh.core.util

import kotlin.uuid.Uuid

object Utils {

    private val HEX_UUID_PATTERN = Regex("[0-9a-fA-F]{32}")


    /**
     * Drops the dashes in the UUID.
     *
     * @return a UUID string without dashes.
     */
    fun Uuid.encode() = toString().uppercase().filter { it.isLetterOrDigit() }

    /**
     * Formats a UUID string to a standard UUID format.
     *
     * @param uuid the UUID string to decode.
     */
    fun decode(uuid: String): Uuid = Uuid.parse(
        uuidString = uuid
            .uppercase()
            .takeIf { HEX_UUID_PATTERN.matches(it) }
            ?.run {
                StringBuilder(this).apply {
                    insert(8, "-")
                    insert(13, "-")
                    insert(18, "-")
                    insert(23, "-")
                }.toString()
            } ?: uuid)
}

