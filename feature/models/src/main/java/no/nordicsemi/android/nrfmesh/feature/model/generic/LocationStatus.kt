package no.nordicsemi.android.nrfmesh.feature.model.generic

import android.content.res.Resources
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.East
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.North
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.Altitude
import no.nordicsemi.kotlin.mesh.core.messages.FloorNumber
import no.nordicsemi.kotlin.mesh.core.messages.Latitude
import no.nordicsemi.kotlin.mesh.core.messages.LocalLocationStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.LocationUncertainty
import no.nordicsemi.kotlin.mesh.core.messages.Longitude
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalStatus
import no.nordicsemi.kotlin.mesh.core.model.Model
import java.math.BigDecimal

/** The global and local location last read from a Generic Location Server. */
internal class LocationState {
    var global by mutableStateOf<GenericLocationGlobalStatus?>(null)
    var local by mutableStateOf<GenericLocationLocalStatus?>(null)
}

/**
 * Keeps the latest global and local location received as a response. Nothing is sent when the
 * screen is opened.
 */
@Composable
internal fun rememberLocationState(messageState: MessageState): LocationState {
    val state = remember { LocationState() }
    LaunchedEffect(key1 = messageState) {
        if (messageState is Completed) {
            when (val response = messageState.response) {
                is GenericLocationGlobalStatus -> state.global = response
                is GenericLocationLocalStatus -> state.local = response
                else -> {}
            }
            state.forgetAfter(message = messageState.message)
        }
    }
    return state
}

/**
 * The node does not report back an unacknowledged set, so the location it set is no longer known
 * once the message is sent.
 */
internal fun LocationState.forgetAfter(message: Any?) {
    when (message) {
        is GenericLocationGlobalSetUnacknowledged -> global = null
        is GenericLocationLocalSetUnacknowledged -> local = null
        else -> {}
    }
}

/**
 * Keeps the content at the tallest height it has had, so that switching to a shorter location does
 * not shrink the screen and make it jump when it is scrolled to the bottom.
 */
@Composable
internal fun StableHeight(content: @Composable ColumnScope.() -> Unit) {
    val density = LocalDensity.current
    var tallest by remember { mutableIntStateOf(0) }
    Column(
        modifier = Modifier
            .heightIn(min = with(density) { tallest.toDp() })
            .onSizeChanged { tallest = maxOf(tallest, it.height) },
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        content = content
    )
}

/** The locations that can be selected on the Generic Location screens. */
internal enum class LocationKind { GLOBAL, LOCAL }

/** A segmented selector for the global and the local location. */
@Composable
internal fun LocationSelector(
    selected: LocationKind,
    onSelected: (LocationKind) -> Unit,
    enabled: Boolean = true,
) {
    val kinds = LocationKind.entries
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        kinds.forEachIndexed { index, kind ->
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = kinds.size),
                onClick = { onSelected(kind) },
                selected = kind == selected,
                enabled = enabled,
                icon = {
                    SegmentedButtonDefaults.Icon(active = kind == selected) {
                        Icon(
                            imageVector = if (kind == LocationKind.GLOBAL) Icons.Outlined.Public
                            else Icons.Outlined.Place,
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                },
                label = {
                    Text(
                        text = stringResource(
                            if (kind == LocationKind.GLOBAL) R.string.label_global
                            else R.string.label_local
                        )
                    )
                }
            )
        }
    }
}

/**
 * Displays the global or the local location of a Generic Location Server, depending on the
 * selection. The refresh button reads the selected location.
 */
@Composable
internal fun LocationStatus(
    model: Model,
    state: LocationState,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val resources = LocalResources.current
    val enabled = model.boundApplicationKeys.isNotEmpty() && !messageState.isInProgress()
    val global = state.global
    val local = state.local
    var selected by rememberSaveable { mutableStateOf(LocationKind.GLOBAL) }
    val unknown = stringResource(R.string.label_unknown)

    LocationSectionHeader(
        title = stringResource(R.string.label_status),
        enabled = enabled,
        isReadInProgress = messageState.isInProgress() && when (selected) {
            LocationKind.GLOBAL -> messageState.message is GenericLocationGlobalGet
            LocationKind.LOCAL -> messageState.message is GenericLocationLocalGet
        },
        onRead = {
            sendApplicationMessage(
                model,
                when (selected) {
                    LocationKind.GLOBAL -> GenericLocationGlobalGet()
                    LocationKind.LOCAL -> GenericLocationLocalGet()
                }
            )
        }
    )
    LocationSelector(selected = selected, onSelected = { selected = it })
    StableHeight {
    when (selected) {
        LocationKind.GLOBAL -> {
            LocationItem(
                icon = Icons.Outlined.SwapVert,
                title = stringResource(R.string.label_latitude),
                subtitle = global?.latitude?.describe(resources) ?: unknown
            )
            LocationItem(
                icon = Icons.Outlined.SwapHoriz,
                title = stringResource(R.string.label_longitude),
                subtitle = global?.longitude?.describe(resources) ?: unknown
            )
            LocationItem(
                icon = Icons.Outlined.Terrain,
                title = stringResource(R.string.label_altitude),
                subtitle = global?.altitude?.describe(resources) ?: unknown
            )
        }

        LocationKind.LOCAL -> {
            LocationItem(
                icon = Icons.Outlined.North,
                title = stringResource(R.string.label_north),
                subtitle = local?.north?.describeCoordinate(resources) ?: unknown
            )
            LocationItem(
                icon = Icons.Outlined.East,
                title = stringResource(R.string.label_east),
                subtitle = local?.east?.describeCoordinate(resources) ?: unknown
            )
            LocationItem(
                icon = Icons.Outlined.Terrain,
                title = stringResource(R.string.label_altitude),
                subtitle = local?.altitude?.describeLocalAltitude(resources) ?: unknown
            )
            LocationItem(
                icon = Icons.Outlined.Apartment,
                title = stringResource(R.string.label_floor),
                subtitle = local?.floor?.describe(resources) ?: unknown
            )
            LocationItem(
                icon = Icons.Outlined.MyLocation,
                title = stringResource(R.string.label_uncertainty),
                subtitle = local?.uncertainty?.describe(resources) ?: unknown,
                subtitleMaxLines = 2
            )
        }
    }
    }
}

@Composable
private fun LocationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    subtitleMaxLines: Int = 1,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = icon,
        title = title,
        subtitle = subtitle,
        subtitlesMaxLines = subtitleMaxLines
    )
}

@Composable
internal fun LocationSectionHeader(
    title: String,
    enabled: Boolean,
    isReadInProgress: Boolean,
    onRead: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(horizontal = 16.dp),
            title = title
        )
        MeshIconButton(
            onClick = onRead,
            buttonIcon = Icons.Outlined.Autorenew,
            enabled = enabled,
            isOnClickActionInProgress = isReadInProgress,
        )
    }
}

internal fun Latitude.describe(resources: Resources): String = degrees
    ?.let { "%.5f°".format(it) }
    ?: resources.getString(R.string.label_not_configured)

internal fun Longitude.describe(resources: Resources): String = degrees
    ?.let { "%.5f°".format(it) }
    ?: resources.getString(R.string.label_not_configured)

/** Describes a global altitude, which is in meters. */
internal fun Altitude.describe(resources: Resources): String = when {
    isTooLarge -> resources.getString(R.string.label_altitude_too_large)
    !isConfigured -> resources.getString(R.string.label_not_configured)
    else -> "$raw m"
}

/** Describes a local north or east coordinate, which is in decimeters. */
internal fun Short.describeCoordinate(resources: Resources): String =
    if (this == LocalLocationStatusMessage.LOCAL_NOT_CONFIGURED) {
        resources.getString(R.string.label_not_configured)
    } else {
        "$this dm"
    }

/** Describes a local altitude, which is in decimeters. */
internal fun Short.describeLocalAltitude(resources: Resources): String = when (this) {
    Altitude.NOT_CONFIGURED -> resources.getString(R.string.label_not_configured)
    Altitude.TOO_LARGE -> resources.getString(R.string.label_altitude_too_large)
    else -> "$this dm"
}

internal fun FloorNumber.describe(resources: Resources): String = when {
    this == FloorNumber.GROUND_FLOOR_0 -> resources.getString(R.string.label_ground_floor_0)
    this == FloorNumber.GROUND_FLOOR_1 -> resources.getString(R.string.label_ground_floor_1)
    !isKnown -> resources.getString(R.string.label_unknown)
    isMinOrBelow -> resources.getString(R.string.label_floor_or_below, FloorNumber.MIN)
    isMaxOrAbove -> resources.getString(R.string.label_floor_or_above, FloorNumber.MAX)
    else -> number.toString()
}

/** Milliseconds as seconds, or millimeters as meters, without trailing zeros: 125 -> 0.125. */
internal fun Long.thousandthsToString(): String =
    BigDecimal(this).movePointLeft(3).stripTrailingZeros().toPlainString()

internal fun LocationUncertainty.describe(resources: Resources): String = listOf(
    resources.getString(if (isMobile) R.string.label_mobile else R.string.label_stationary),
    resources.getString(R.string.label_update_time_s, updateTimeMillis.thousandthsToString()),
    resources.getString(R.string.label_precision_m, precisionMillimeters.thousandthsToString()),
).joinToString(separator = ", ")
