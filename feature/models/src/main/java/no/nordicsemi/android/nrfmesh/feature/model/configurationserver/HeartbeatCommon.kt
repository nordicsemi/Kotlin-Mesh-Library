package no.nordicsemi.android.nrfmesh.feature.model.configurationserver

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.model.AllFriends
import no.nordicsemi.kotlin.mesh.core.model.AllNodes
import no.nordicsemi.kotlin.mesh.core.model.AllProxies
import no.nordicsemi.kotlin.mesh.core.model.AllRelays
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.HasAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress

/**
 * Section title of a heartbeat section with a refresh (get), add (set) and, optionally, a delete
 * button.
 */
@Composable
internal fun HeartbeatSectionTitle(
    title: String,
    enabled: Boolean,
    isRefreshInProgress: Boolean,
    isAddInProgress: Boolean,
    showDelete: Boolean,
    onRefresh: () -> Unit,
    onAdd: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(modifier = Modifier.weight(weight = 1f), title = title)
        if (showDelete) {
            MeshIconButton(
                enabled = enabled,
                buttonIcon = Icons.Outlined.Delete,
                onClick = onDelete
            )
        }
        MeshIconButton(
            enabled = enabled,
            isOnClickActionInProgress = isRefreshInProgress,
            buttonIcon = Icons.Outlined.Refresh,
            onClick = onRefresh
        )
        MeshIconButton(
            enabled = enabled,
            isOnClickActionInProgress = isAddInProgress,
            buttonIcon = Icons.Outlined.Add,
            onClick = onAdd
        )
    }
}

/** Returns the name of the node or group that has the given address or a fixed address name. */
@Composable
internal fun addressName(network: MeshNetwork?, address: HasAddress): String = when (address) {
    is UnicastAddress -> network?.node(address = address.address)?.name
        ?: address.toHexString()

    is AllRelays -> stringResource(R.string.label_all_relays)
    is AllFriends -> stringResource(R.string.label_all_friends)
    is AllProxies -> stringResource(R.string.label_all_proxies)
    is AllNodes -> stringResource(R.string.label_all_nodes)
    is GroupAddress -> network?.group(address = address.address)?.name ?: address.toHexString()
    is UnassignedAddress -> stringResource(R.string.label_unassigned_address)
    else -> address.toHexString()
}

/** Returns the name of the node or group followed by the address, e.g. `Mesh Light · 0x0017`. */
@Composable
internal fun addressText(network: MeshNetwork?, address: HasAddress): String {
    val name = addressName(network = network, address = address)
    val hex = address.toHexString()
    return if (address is UnassignedAddress || name == hex) name else "$name · $hex"
}
