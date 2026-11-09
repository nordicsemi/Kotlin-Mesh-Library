package no.nordicsemi.android.nrfmesh.feature.model.utils

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import no.nordicsemi.kotlin.mesh.core.messages.RangeMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLastStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLinearStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessRangeStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessStatus
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
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

/**
 * Saves a lightness status as `[lightness, target lightness, remaining time]`, using -1 for the
 * target lightness and the remaining time when they are absent.
 */
private fun lightnessToList(
    lightness: UShort,
    target: UShort?,
    remainingTime: TransitionTime?,
) = listOf(
    lightness.toInt(),
    target?.toInt() ?: -1,
    remainingTime?.rawValue?.toInt() ?: -1
)

private fun List<Int>.target() = this[1].takeIf { it >= 0 }?.toUShort()

private fun List<Int>.remainingTime() = this[2].takeIf { it >= 0 }
    ?.let { TransitionTime(rawValue = it.toUByte()) }

internal val LightnessStatusSaver = listSaver<LightLightnessStatus?, Int>(
    save = { status ->
        status?.let { lightnessToList(it.lightness, it.targetLightness, it.remainingTime) }
            ?: emptyList()
    },
    restore = {
        if (it.isEmpty()) null
        else LightLightnessStatus(
            lightness = it[0].toUShort(),
            targetLightness = it.target(),
            remainingTime = it.remainingTime()
        )
    }
)

internal val LinearStatusSaver = listSaver<LightLightnessLinearStatus?, Int>(
    save = { status ->
        status?.let { lightnessToList(it.lightness, it.targetLightness, it.remainingTime) }
            ?: emptyList()
    },
    restore = {
        if (it.isEmpty()) null
        else LightLightnessLinearStatus(
            lightness = it[0].toUShort(),
            targetLightness = it.target(),
            remainingTime = it.remainingTime()
        )
    }
)

internal val LastStatusSaver = Saver<LightLightnessLastStatus?, Int>(
    save = { it?.lightness?.toInt() },
    restore = { LightLightnessLastStatus(lightness = it.toUShort()) }
)

internal val DefaultStatusSaver = Saver<LightLightnessDefaultStatus?, Int>(
    save = { it?.lightness?.toInt() },
    restore = { LightLightnessDefaultStatus(lightness = it.toUShort()) }
)

internal val RangeStatusSaver = listSaver<LightLightnessRangeStatus?, Int>(
    save = { status ->
        status?.let { listOf(it.status.value.toInt(), it.min.toInt(), it.max.toInt()) }
            ?: emptyList()
    },
    restore = {
        if (it.isEmpty()) null
        else RangeMessageStatus.from(value = it[0].toUByte())?.let { status ->
            LightLightnessRangeStatus(
                status = status,
                min = it[1].toUShort(),
                max = it[2].toUShort()
            )
        }
    }
)

/** Saves a [TransitionTime] as its raw value. */
internal val TransitionTimeSaver = Saver<TransitionTime, Int>(
    save = { it.rawValue.toInt() },
    restore = { TransitionTime(rawValue = it.toUByte()) }
)
