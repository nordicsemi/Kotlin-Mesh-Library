package no.nordicsemi.android.nrfmesh.feature.model.utils

import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import kotlin.math.floor

/**
 * Converts a transition time slider value to a [TransitionTime] and a label.
 *
 * @param value                   Slider value in the range 0..235.
 * @param onTransitionTimeChanged Invoked with the resulting [TransitionTime] and a human-readable
 *                                label.
 */
internal fun handleTransitionTimeChange(
    value: Float,
    onTransitionTimeChanged: (TransitionTime, String) -> Unit,
) {
    when {
        value < 1.0f -> {
            onTransitionTimeChanged(
                TransitionTime(
                    steps = 0u,
                    stepResolution = StepResolution.HUNDREDS_OF_MILLISECONDS
                ),
                "Immediate"
            )
        }

        value in 1.0f..<10.0f -> {
            onTransitionTimeChanged(
                TransitionTime(
                    steps = value.toInt().toUByte(),
                    stepResolution = StepResolution.HUNDREDS_OF_MILLISECONDS
                ),
                "${(value.toInt() * 100)} ms"
            )
        }

        value in 10.0f..<63.0f -> {
            onTransitionTimeChanged(
                TransitionTime(
                    steps = value.toInt().toUByte(),
                    stepResolution = StepResolution.HUNDREDS_OF_MILLISECONDS
                ),
                "${floor(value) / 10} sec"
            )
        }

        value in 63.0f..<116.0f -> {
            onTransitionTimeChanged(
                TransitionTime(
                    steps = (value.toInt() - 56).toUByte(),
                    stepResolution = StepResolution.SECONDS
                ),
                "${(value.toInt() - 56)} sec"
            )
        }

        value in 116.0f..<119.0f -> {
            val min = (value.toInt() + 4) / 60 - 1
            val sec = (value.toInt() + 4) % 60
            onTransitionTimeChanged(
                TransitionTime(
                    steps = (value.toInt() - 56).toUByte(),
                    stepResolution = StepResolution.SECONDS
                ),
                "$min min ${sec.toString().padStart(2, '0')} sec"
            )
        }

        value in 119.0f..<175.0f -> {
            val sec = ((value.toInt() + 2) % 6) * 10
            val secString = if (sec == 0) "00" else sec.toString()
            onTransitionTimeChanged(
                TransitionTime(
                    steps = (value.toInt() - 112).toUByte(),
                    stepResolution = StepResolution.TENS_OF_SECONDS
                ),
                "${(value.toInt() + 2) / 6 - 19} min $secString sec"
            )
        }

        value in 175.0f..<179.0f -> {
            onTransitionTimeChanged(
                TransitionTime(
                    steps = (value.toInt() - 173).toUByte(),
                    stepResolution = StepResolution.TENS_OF_MINUTES
                ),
                "${(value.toInt() - 173) * 10} min"
            )
        }

        value >= 179.0f -> {
            val min = (value.toInt() - 173) % 6 * 10
            val minString = if (min == 0) "00" else min.toString()
            onTransitionTimeChanged(
                TransitionTime(
                    steps = (value.toInt() - 173).toUByte(),
                    stepResolution = StepResolution.TENS_OF_MINUTES
                ),
                "${(value.toInt() + 1) / 6 - 29} h $minString min"
            )
        }
    }
}
