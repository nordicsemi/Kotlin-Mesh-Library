package no.nordicsemi.android.nrfmesh.feature.groups.group.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.SensorOccupied
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.theme.nordicFall
import no.nordicsemi.android.common.theme.nordicGrass
import no.nordicsemi.android.common.theme.nordicLake
import no.nordicsemi.android.common.theme.nordicSky
import no.nordicsemi.android.nrfmesh.core.common.isGenericLevelServer
import no.nordicsemi.android.nrfmesh.core.common.isGenericOnOffServer
import no.nordicsemi.android.nrfmesh.core.common.isLightLCServer
import no.nordicsemi.android.nrfmesh.core.common.isSceneServer
import no.nordicsemi.android.nrfmesh.core.common.isSceneSetupServer
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.groups.R
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.ModelId

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GroupControlsScreen(
    uiState: GroupControlsScreenUiState,
    send: (UnacknowledgedMeshMessage, ApplicationKey) -> Unit
) {
    when(uiState.groupState) {
        is GroupModelControlsState.Success -> {
            GroupControls(
                network = uiState.groupState.network,
                modelId = uiState.groupState.modelId,
                models = uiState.groupState.groupInfoListData.models,
                send = send,
            )
        }
        else -> {

        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GroupControls(
    network: MeshNetwork,
    modelId: ModelId,
    models: Map<ModelId, List<Model>>,
    send: (UnacknowledgedMeshMessage, ApplicationKey) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.padding(top = 8.dp).padding(horizontal = 16.dp),
            title = stringResource(id = R.string.goup_controls)
        )
        FlowRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            maxItemsInEachRow = 5,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            network.applicationKeys.forEach { key ->
                models[modelId]?.filter {
                    key.isBoundTo(model = it)
                }?.takeIf {
                    it.isNotEmpty()
                }?.let { models ->
                    when {
                        models.first().isGenericOnOffServer() ->
                            GenericOnOffItem(count = models.size, key = key, send = send)

                        models.first().isGenericLevelServer() ->
                            GenericLevelItem(count = models.size, key = key, send = send)

                        models.first().isSceneServer() ->
                            SceneServerGroupItem(count = models.size, key = key, send = send)

                        models.first().isSceneSetupServer() ->
                            SceneSetupServerGroupItem(count = models.size, key = key, send = send)

                        models.first().isLightLCServer() -> {
                            LightLCModeGroupItem(count = models.size, key = key, send = send)
                            LightLCOccupancyModeGroupItem(count = models.size, key = key, send = send)
                            LightLCLightOnOffGroupItem(count = models.size, key = key, send = send)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GenericOnOffItem(
    key: ApplicationKey,
    send: (UnacknowledgedMeshMessage, ApplicationKey) -> Unit,
    count: Int,
) {
    OutlinedCard(
        modifier = Modifier.width(width = 150.dp),
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(50.dp),
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = nordicLake
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    content = {
                        TextButton(
                            onClick = { send(GenericOnOffSetUnacknowledged(on = true), key) },
                            content = { Text(text = stringResource(R.string.label_on).uppercase()) },
                        )
                        TextButton(
                            onClick = { send(GenericOnOffSetUnacknowledged(on = false), key) },
                            content = { Text(text = stringResource(R.string.label_off).uppercase()) },
                        )
                    }
                )
                KeyRow(key = key)
                CountRow(count = count)
            }
        }
    )
}

@Composable
private fun GenericLevelItem(
    key: ApplicationKey,
    send: (UnacknowledgedMeshMessage, ApplicationKey) -> Unit,
    count: Int,
) {
    OutlinedCard(
        modifier = Modifier.width(width = 150.dp),
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(50.dp),
                    imageVector = Icons.Outlined.LightMode,
                    contentDescription = null,
                    tint = nordicFall
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    content = {
                        TextButton(
                            onClick = { send(GenericDeltaSetUnacknowledged(delta = -8192), key) },
                            content = {
                                Text(
                                    text = stringResource(R.string.label_minus),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                        )
                        TextButton(
                            onClick = { send(GenericDeltaSetUnacknowledged(delta = +8192), key) },
                            content = {
                                Text(
                                    text = stringResource(R.string.label_plus),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                        )
                    }
                )
                KeyRow(key = key)
                CountRow(count = count)
            }
        }
    )
}

@Composable
private fun KeyRow(key: ApplicationKey) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Outlined.VpnKey,
            contentDescription = null
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = key.name,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun CountRow(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = Icons.Outlined.Widgets,
            contentDescription = null
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = pluralStringResource(R.plurals.label_models_count, count, count),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Suppress("unused")
@Composable
private fun SceneServerGroupItem(
    key: ApplicationKey,
    send: (UnacknowledgedMeshMessage, ApplicationKey) -> Unit,
    count: Int,
) {
    OutlinedCard(
        modifier = Modifier.width(width = 150.dp),
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(50.dp),
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = nordicSky
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { },
                        content = { Text(text = stringResource(R.string.label_recall).uppercase()) }
                    )
                }
                KeyRow(key = key)
                CountRow(count = count)
            }
        }
    )
}

@Suppress("unused")
@Composable
private fun SceneSetupServerGroupItem(
    key: ApplicationKey,
    send: (UnacknowledgedMeshMessage, ApplicationKey) -> Unit,
    count: Int,
) {
    OutlinedCard(
        modifier = Modifier.width(width = 150.dp),
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(50.dp),
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = null,
                    tint = nordicLake
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = { },
                        content = { Text(text = stringResource(R.string.label_store).uppercase()) }
                    )
                }
                KeyRow(key = key)
                CountRow(count = count)
            }
        }
    )
}

@Suppress("unused")
@Composable
private fun LightLCModeGroupItem(
    key: ApplicationKey,
    send: (UnacknowledgedMeshMessage, ApplicationKey) -> Unit,
    count: Int,
) {
    OutlinedCard(
        modifier = Modifier.width(width = 150.dp),
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(50.dp),
                    imageVector = Icons.Outlined.SensorOccupied,
                    contentDescription = null,
                    tint = nordicGrass
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    content = {
                        TextButton(
                            onClick = { },
                            content = { Text(text = stringResource(R.string.label_on).uppercase()) },
                        )
                        TextButton(
                            onClick = { },
                            content = { Text(text = stringResource(R.string.label_off).uppercase()) },
                        )
                    }
                )
                KeyRow(key = key)
                CountRow(count = count)
            }
        }
    )
}

@Suppress("unused")
@Composable
private fun LightLCOccupancyModeGroupItem(
    key: ApplicationKey,
    send: (UnacknowledgedMeshMessage, ApplicationKey) -> Unit,
    count: Int,
) {
    OutlinedCard(
        modifier = Modifier.width(width = 150.dp),
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(50.dp),
                    imageVector = Icons.Outlined.SensorOccupied,
                    contentDescription = null,
                    tint = nordicGrass
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    content = {
                        TextButton(
                            onClick = { },
                            content = { Text(text = stringResource(R.string.label_on).uppercase()) },
                        )
                        TextButton(
                            onClick = { },
                            content = { Text(text = stringResource(R.string.label_off).uppercase()) },
                        )
                    }
                )
                KeyRow(key = key)
                CountRow(count = count)
            }
        }
    )
}

@Suppress("unused")
@Composable
private fun LightLCLightOnOffGroupItem(
    key: ApplicationKey,
    send: (UnacknowledgedMeshMessage, ApplicationKey) -> Unit,
    count: Int,
) {
    OutlinedCard(
        modifier = Modifier.width(width = 150.dp),
        content = {
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    modifier = Modifier.size(50.dp),
                    imageVector = Icons.Outlined.SensorOccupied,
                    contentDescription = null,
                    tint = nordicGrass
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.CenterHorizontally
                    ),
                    content = {
                        TextButton(
                            onClick = { },
                            content = { Text(text = stringResource(R.string.label_on).uppercase()) },
                        )
                        TextButton(
                            onClick = { },
                            content = { Text(text = stringResource(R.string.label_off).uppercase()) },
                        )
                    }
                )
                KeyRow(key = key)
                CountRow(count = count)
            }
        }
    )
}
