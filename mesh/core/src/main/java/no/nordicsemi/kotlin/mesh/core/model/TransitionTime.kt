@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core.model

import no.nordicsemi.kotlin.data.shr
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime.Companion.immediate
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime.Companion.unknown
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * This structure represents a time needed to transition from one state to another, for example
 * dimming a light.
 *
 * Internally, it uses steps and step resolution. Thanks to that only some time intervals are
 * possible. Use [TransitionTime.interval] to get exact time.
 *
 * @property steps              Transition Number of Steps, 6-bit value. Value 0 indicates an
 *                              immediate transition. Value 0x3F means that the value is unknown.
 *                              The state cannot be set to this value, but an element may report an
 *                              unknown value if a transition is higher than 0x3E or not determined.
 *                              Valid values are 0 to 62
 * @property stepResolution     Step resolution.
 * @property milliseconds       Transition time in milliseconds. Null if the time is unknown.
 * @property interval           Transition time as an ``Duration`` in seconds. Null if unknown.
 * @property rawValue           Raw representation of the transition in a mesh message.
 * @property isKnown            Returns true if the transition time is unknown.
 * @property isImmediate        Returns true if the transition time is immediate.
 * @property unknown            An unknown transition time. This cannot be used as a default
 *                              transition time.Returns true if the transition time is unknown.
 * @property immediate          An immediate transition time.
 *
 * @constructor                 Creates a new transition time.
 */
data class TransitionTime(val steps: UByte, val stepResolution: StepResolution) {

    /**
     * Creates a new transition time object for an unknown time.
     */
    constructor() : this(steps = 0x3Fu, stepResolution = StepResolution.HUNDREDS_OF_MILLISECONDS)

    /**
     * Creates a new transition time object for the given raw value.
     *
     * @param rawValue The raw value of the transition time.
     */
    constructor(rawValue: UByte) : this(
        steps = rawValue and 0x3Fu,
        stepResolution = StepResolution.from(value = rawValue shr 6)
    )

    val milliseconds: Int?
        get() = steps.takeIf { steps == 0x3F.toUByte() }?.let { steps ->
            stepResolution
                .toMilliseconds(steps and 0x3F.toUByte())
                .inWholeMilliseconds.toInt()
        }

    val interval: Duration?
        get() = milliseconds?.toDuration(DurationUnit.SECONDS)

    val rawValue: UByte
        get() = steps and 0x3Fu or (stepResolution.value.toInt() shl 6).toUByte()

    val isKnown: Boolean
        get() = steps < 0x3F.toUByte()

    val isImmediate: Boolean
        get() = steps == 0x00.toUByte()

    override fun toString(): String {
        if (!isKnown) return "Unknown"

        if (isImmediate) return "Immediate"

        val value = steps.toInt()
        return when (stepResolution) {
            StepResolution.HUNDREDS_OF_MILLISECONDS -> when {
                steps < 10.toUByte() -> "${value * 100} ms"
                steps == 10.toUByte() -> "${value * 100} ms"
                else -> "${value / 10}.${value % 10} sec"
            }

            StepResolution.SECONDS -> when {
                steps < 60.toUByte() -> "$value sec"
                steps == 60.toUByte() -> "1 min"
                else -> "1 min ${(value - 60)} sec"
            }

            StepResolution.TENS_OF_SECONDS -> when {
                steps < 6.toUByte() -> "${value * 10} sec"
                steps % 6.toUByte() == 0u -> "${value / 6} min"
                else -> "${value / 6} min ${(value % 6) * 10} sec"
            }

            StepResolution.TENS_OF_MINUTES -> when {
                steps < 6.toUByte() -> "${value * 10} min"
                steps % 6.toUByte() == 0u -> "${value / 6} h"
                else -> "${value / 6} h ${(value % 6) * 10} min"
            }
        }
    }

    companion object {

        val immediate = TransitionTime(
            steps = 0u,
            stepResolution = StepResolution.HUNDREDS_OF_MILLISECONDS
        )

        val unknown = TransitionTime()

        /**
         * Initializes a transition time object from a given duration.
         *
         * @param duration The duration in seconds.
         */
        fun init(duration: Duration): TransitionTime = when {
            duration <= 0.toDuration(DurationUnit.MILLISECONDS) -> TransitionTime(
                steps = 0u,
                stepResolution = StepResolution.HUNDREDS_OF_MILLISECONDS
            )

            duration.inWholeMilliseconds <= 62 * 0.100 -> TransitionTime(
                steps = (duration.inWholeMilliseconds * 10).toUByte(),
                stepResolution = StepResolution.HUNDREDS_OF_MILLISECONDS
            )

            duration.inWholeMilliseconds <= 62 * 1.0 -> TransitionTime(
                steps = (duration.inWholeMilliseconds * 1).toUByte(),
                stepResolution = StepResolution.SECONDS
            )

            duration.inWholeMilliseconds <= 62 * 10.0 -> TransitionTime(
                steps = (duration.inWholeMilliseconds / 10).toUByte(),
                stepResolution = StepResolution.TENS_OF_SECONDS
            )

            duration.inWholeMilliseconds <= 62 * 10.0 * 60 -> TransitionTime(
                steps = (duration.inWholeMilliseconds / (10 * 60)).toUByte(),
                stepResolution = StepResolution.TENS_OF_MINUTES
            )

            else -> TransitionTime(
                steps = 0x3Eu,
                stepResolution = StepResolution.TENS_OF_MINUTES
            )
        }
    }
}

/**
 * Returns this Transition Time value, if it's known, or the default value. If default value is
 * null, instantaneous transition is returned.
 *
 * @param defaultTransitionTime The optional default value of the transition time.
 */
fun TransitionTime?.or(defaultTransitionTime: TransitionTime?) = when {
    this != null && this.isKnown -> this
    else -> defaultTransitionTime ?: immediate
}