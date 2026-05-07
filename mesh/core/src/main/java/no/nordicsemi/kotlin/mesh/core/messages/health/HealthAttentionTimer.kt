package no.nordicsemi.kotlin.mesh.core.messages.health

import kotlin.time.Duration

/**
 * Health attention time event to notify the application when to start and stop the attention timer
 */
sealed class HealthAttentionTimer {

    /**
     * Event to notify the application to start the attention timer
     *
     * @property duration The duration of the attention timer
     */
    data class Start(val duration: Duration) : HealthAttentionTimer()

    /**
     * Event to notify the application to stop the attention timer
     */
    object Stop : HealthAttentionTimer()
}