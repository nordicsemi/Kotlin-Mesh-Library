package no.nordicsemi.kotlin.mesh.core.messages

/**
 * Base initializer for Generic Messages.
 */
interface GenericMessageInitializer : BaseMeshMessageInitializer, HasOpCode

/**
 * Defines the status codes returned by a server in the range status messages, such as the Light
 * Lightness Range Status message.
 *
 * @property value Raw value of the status code.
 */
enum class RangeMessageStatus(val value: UByte) {
    SUCCESS(value = 0x00u),
    CANNOT_SET_RANGE_MIN(value = 0x01u),
    CANNOT_SET_RANGE_MAX(value = 0x02u);

    /**
     * Returns a human-readable description of the status code.
     */
    val description: String
        get() = when (this) {
            SUCCESS -> "Success"
            CANNOT_SET_RANGE_MIN -> "Cannot Set Range Min"
            CANNOT_SET_RANGE_MAX -> "Cannot Set Range Max"
        }

    companion object {
        /**
         * Returns the [RangeMessageStatus] for the given raw value or null if the value is unknown
         * (reserved for future use).
         */
        fun from(value: UByte) = entries.firstOrNull { it.value == value }
    }
}
