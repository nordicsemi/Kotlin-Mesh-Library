package no.nordicsemi.android.nrfmesh.core.data.model

import kotlin.time.Clock.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * GenericState is a data class that represents a state with a value of type T.
 *
 * @property value           The current value of the state.
 * @property storedWithScene Indicates whether the state is stored with a scene.
 * @property transition      Represents a transition from the current value to a target value.
 * @property animation       Represents an animation from the current value to a target value.
 */
data class GenericState<T> @OptIn(ExperimentalTime::class) constructor(
    val value: T,
    val storedWithScene: Boolean = false,
    val transition: Transition<T>? = null,
    val animation: Move<T>? = null,
) {

    data class Transition<T> @OptIn(ExperimentalTime::class) constructor(
        val targetValue: T,
        val start: Instant,
        val delay: Duration,
        val duration: Duration,
    ) {

        @OptIn(ExperimentalTime::class)
        val startTime: Instant
            get() = start + delay

        @OptIn(ExperimentalTime::class)
        val remainingTime: Duration
            get() = startTime.minus(other = System.now()).let { startsIn ->
                if (startsIn + duration > 0.0.milliseconds) {
                    startsIn + duration
                } else {
                    return 0.0.milliseconds
                }
            }
    }

    @OptIn(ExperimentalTime::class)
    @ExperimentalTime
    data class Move<T> @OptIn(ExperimentalTime::class) constructor(
        val start: Instant,
        val delay: Duration,
        val speed: T,
    ) {
        val startTime: Instant
            get() = start.plus(duration = delay)
    }

    /**
     * Convenience constructor for creating a GenericState with a value and storedWithScene flag.
     */
    constructor(state: T, storedWithScene: Boolean = false) : this(
        value = state,
        storedWithScene = storedWithScene,
        transition = null,
        animation = null
    )

    companion object {

        /**
         * Creates a new [GenericState] object with the specified value that will transition from a
         * given state..
         */
        @ExperimentalTime
        fun <T> transitionFrom(
            transitionFrom: GenericState<T>,
            to: T,
            delay: Duration,
            duration: Duration?,
            storedWithScene: Boolean = false,
        ) = GenericState<T>(
            value = transitionFrom.value,
            storedWithScene = storedWithScene,
            transition = if (duration == null ||
                (delay <= 0.0.milliseconds && duration <= 0.0.milliseconds) ||
                (transitionFrom.transition == null && transitionFrom.value == to)
            ) null else Transition(targetValue = to, start = System.now(), delay, duration),
            animation = null
        )

        @OptIn(ExperimentalTime::class)
        @ExperimentalTime
        @Suppress("unused")
        fun <T> continueTransitionFrom(
            state: GenericState<T>,
            targetValue: T,
            delay: Duration,
            duration: Duration?,
            storedWithScene: Boolean = false,
        ): GenericState<T> = when {
            duration == null || (delay <= 0.0.milliseconds && duration <= 0.0.milliseconds) ->
                GenericState(
                    value = targetValue,
                    transition = null,
                    animation = null,
                    storedWithScene = storedWithScene
                )

            state.transition == null && state.value == targetValue -> GenericState(
                value = state.value,
                transition = null,
                animation = null,
                storedWithScene = storedWithScene
            )

            else -> GenericState(
                value = state.value,
                transition = state.transition?.let {
                    Transition(
                        targetValue = targetValue,
                        start = it.start,
                        delay = delay,
                        duration = duration
                    )
                } ?: Transition(
                    targetValue = targetValue,
                    start = System.now(),
                    delay = delay,
                    duration = duration
                ),
                animation = null,
                storedWithScene = storedWithScene
            )
        }

        /**
         * Creates a new [GenericState] object with the specified value that will transition from a
         * given state..
         */
        @OptIn(ExperimentalTime::class)
        @Suppress("unused")
        fun <Int> transitionFromInt(
            transitionFrom: GenericState<out Int>,
            to: Int,
            delay: Duration,
            duration: Duration?,
            storedWithScene: Boolean = false,
        ) = GenericState<Int>(
            value = transitionFrom.value,
            storedWithScene = storedWithScene,
            transition = if (duration == null ||
                (delay <= 0.0.milliseconds && duration <= 0.0.milliseconds) ||
                (transitionFrom.transition == null && transitionFrom.value == to)
            ) null else Transition(to, System.now(), delay, duration),
            animation = null
        )

        @OptIn(ExperimentalTime::class)
        @ExperimentalTime
        @Suppress("unused")
        fun <Int> continueTransitionFromInt(
            state: GenericState<out Int>,
            targetValue: Int,
            delay: Duration,
            duration: Duration?,
            storedWithScene: Boolean = false,
        ): GenericState<out Int> = when {
            duration == null || (delay <= 0.0.milliseconds && duration <= 0.0.milliseconds) ->
                GenericState(
                    value = targetValue,
                    transition = null,
                    animation = null,
                    storedWithScene = storedWithScene
                )

            state.transition == null && state.value == targetValue -> GenericState(
                value = state.value,
                transition = null,
                animation = null,
                storedWithScene = storedWithScene
            )

            else -> GenericState(
                value = state.value,
                transition = state.transition?.let {
                    Transition(
                        targetValue = targetValue,
                        start = it.start,
                        delay = delay,
                        duration = duration
                    )
                } ?: Transition(
                    targetValue = targetValue,
                    start = System.now(),
                    delay = delay,
                    duration = duration
                ),
                animation = null,
                storedWithScene = storedWithScene
            )
        }

        @OptIn(ExperimentalTime::class)
        @ExperimentalTime
        @Suppress("unused")
        fun <T> GenericState<Int>.currentValue(): Int {

            animation?.let { anim ->
                val timeDiff = anim.startTime.minus(System.now())
                return if (timeDiff >= 0.milliseconds) {
                    value
                } else {
                    val adjustedValue = value.milliseconds - timeDiff * anim.speed
                    adjustedValue.inWholeMilliseconds.toInt()
                }
            }

            transition?.let { trans ->
                val timeDiff = trans.startTime.minus(System.now())
                return when {
                    timeDiff >= 0.milliseconds -> value
                    trans.remainingTime == 0.0.milliseconds -> trans.targetValue
                    else -> {
                        val progress = trans.remainingTime / trans.duration
                        val diff = value - trans.targetValue
                        val interpolated = trans.targetValue + diff * progress
                        interpolated.toInt()
                    }
                }
            }
            return value
        }
    }
}
