package no.nordicsemi.kotlin.mesh.core.messages.generic

/**
 * Defines the value of the Generic OnPowerUp state, which determines the behavior of the element's
 * bound states when the element is powered up.
 *
 * @property value Raw value of the state.
 */
enum class OnPowerUp(val value: UByte) {
    /** Bound states are set to their off values (e.g. Generic OnOff = Off). */
    OFF(value = 0x00u),

    /** Bound states are set to their default values. */
    DEFAULT(value = 0x01u),

    /** Bound states are restored to the values they had before the power was lost. */
    RESTORE(value = 0x02u);

    /**
     * Returns a human-readable description of the state.
     */
    val description: String
        get() = when (this) {
            OFF -> "Off"
            DEFAULT -> "Default"
            RESTORE -> "Restore"
        }

    companion object {
        /**
         * Returns the [OnPowerUp] for the given raw value or null if the value is unknown
         * (reserved for future use).
         */
        fun from(value: UByte) = entries.firstOrNull { it.value == value }
    }
}
