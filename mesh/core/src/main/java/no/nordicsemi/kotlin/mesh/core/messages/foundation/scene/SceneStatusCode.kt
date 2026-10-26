package no.nordicsemi.kotlin.mesh.core.messages.foundation.scene

/**
 * Defines the status codes returned by a Scene Server in the Scene Status and Scene Register
 * Status messages.
 *
 * @property value Raw value of the status code.
 */
enum class SceneStatusCode(val value: UByte) {
    SUCCESS(value = 0x00u),
    SCENE_REGISTER_FULL(value = 0x01u),
    SCENE_NOT_FOUND(value = 0x02u);

    /**
     * Returns a human readable description of the status code.
     */
    val description: String
        get() = when (this) {
            SUCCESS -> "Success"
            SCENE_REGISTER_FULL -> "Scene Register Full"
            SCENE_NOT_FOUND -> "Scene Not Found"
        }

    companion object {
        /**
         * Returns the [SceneStatusCode] for the given raw value or null if the value is unknown
         * (reserved for future use).
         */
        fun from(value: UByte) = entries.firstOrNull { it.value == value }
    }
}
