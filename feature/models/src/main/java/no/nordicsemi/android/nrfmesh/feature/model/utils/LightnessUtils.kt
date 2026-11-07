package no.nordicsemi.android.nrfmesh.feature.model.utils

import kotlin.math.roundToInt

/** Highest value of the Light Lightness states. */
private const val MAX_LIGHTNESS = 65535f

/**
 * Converts a 16-bit lightness to a percentage between 0 and 100.
 */
internal fun UShort.toLightnessPercent(): Int = (toFloat() / MAX_LIGHTNESS * 100f).roundToInt()

/**
 * Converts a percentage between 0 and 100 to a 16-bit lightness.
 */
internal fun Float.toLightness(): UShort =
    (coerceIn(minimumValue = 0f, maximumValue = 100f) / 100f * MAX_LIGHTNESS)
        .roundToInt()
        .toUShort()
