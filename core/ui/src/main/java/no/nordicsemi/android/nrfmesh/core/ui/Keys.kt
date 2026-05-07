package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun NetworkKey.Row(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    imageVector: ImageVector = Icons.Outlined.VpnKey,
    titleAction: @Composable () -> Unit = {},
) {
    NetworkKeyRow(
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        onClick = onClick,
        imageVector = imageVector,
        title = name,
        titleAction = titleAction,
    )
}

@Composable
fun NetworkKeyRow(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    imageVector: ImageVector = Icons.Outlined.VpnKey,
    title: String,
    titleAction: @Composable () -> Unit = {},
    subtitle: String? = null,
) {
    when (onClick != null) {
        true -> ElevatedCardItem(
            modifier = modifier,
            colors = colors,
            enabled = enabled,
            onClick = onClick,
            imageVector = imageVector,
            title = title,
            titleAction = titleAction,
            subtitle = subtitle
        )

        else -> ElevatedCardItem(
            modifier = modifier,
            colors = colors,
            imageVector = imageVector,
            title = title,
            titleAction = titleAction,
            subtitle = subtitle
        )
    }
}

@Composable
fun ApplicationKey.Row(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    imageVector: ImageVector = Icons.Outlined.VpnKey,
    titleAction: @Composable () -> Unit = {},
) {
    ApplicationKeyRow(
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        onClick = onClick,
        imageVector = imageVector,
        title = name,
        titleAction = titleAction,
        subtitle = "Bound to ${boundNetworkKey.name}",
    )
}

@Composable
fun ApplicationKeyRow(
    modifier: Modifier = Modifier,
    colors: CardColors = CardDefaults.outlinedCardColors(),
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    imageVector: ImageVector = Icons.Outlined.VpnKey,
    title: String,
    titleAction: @Composable () -> Unit = {},
    subtitle: String? = null,
) = when (onClick != null) {
    true -> ElevatedCardItem(
        modifier = modifier,
        colors = colors,
        enabled = enabled,
        onClick = onClick,
        imageVector = imageVector,
        title = title,
        titleAction = titleAction,
        subtitle = subtitle
    )

    else -> ElevatedCardItem(
        modifier = modifier,
        colors = colors,
        imageVector = imageVector,
        title = title,
        titleAction = titleAction,
        subtitle = subtitle
    )
}

@Composable
fun KeyRow(
    modifier: Modifier = Modifier,
    title: String,
    key: ByteArray,
    onKeyChanged: (ByteArray) -> Unit,
    isEditable: Boolean,
    onEditableStateChanged: () -> Unit,
) {
    var isError by rememberSaveable { mutableStateOf(false) }
    ElevatedCardItemHexTextField(
        modifier = modifier,
        showPrefix = false,
        imageVector = Icons.Outlined.VpnKey,
        title = title,
        subtitle = key.toHexString(format = HexFormat.UpperCase),
        onValueChanged = { onKeyChanged(it.hexToByteArray(HexFormat.UpperCase)) },
        isError = isError,
        isEditable = isEditable,
        onEditableStateChanged = onEditableStateChanged,
        regex = Regex(pattern = "[0-9A-Fa-f]{0,32}"),
        validator = Regex(pattern = "[0-9A-Fa-f]{32}")
    )
}
