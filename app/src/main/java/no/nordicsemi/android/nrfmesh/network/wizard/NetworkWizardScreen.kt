package no.nordicsemi.android.nrfmesh.network.wizard

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import no.nordicsemi.android.common.theme.nordicGreen
import no.nordicsemi.android.nrfmesh.R
import no.nordicsemi.android.nrfmesh.core.common.Action
import no.nordicsemi.android.nrfmesh.core.common.Configuration
import no.nordicsemi.android.nrfmesh.core.common.ConfigurationProperty
import no.nordicsemi.android.nrfmesh.core.common.NetworkProperties
import no.nordicsemi.android.nrfmesh.core.common.description
import no.nordicsemi.android.nrfmesh.core.common.icon
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.android.nrfmesh.feature.nodes.R.drawable
import no.nordicsemi.android.nrfmesh.network.util.ImportState

@Composable
internal fun NetworkWizardScreen(
    configurations: List<Configuration>,
    configuration: Configuration,
    onConfigurationSelected: (Configuration) -> Unit,
    add: (ConfigurationProperty) -> Unit,
    remove: (ConfigurationProperty) -> Unit,
    onContinuePressed: () -> Unit,
    importState: ImportState,
    importNetwork: (uri: Uri, contentResolver: ContentResolver) -> Unit,
    navigateToNetwork: () -> Unit,
    onImportErrorAcknowledged: () -> Unit,
) {
    val context = LocalContext.current
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = {
            if (it != null) {
                importNetwork(it, context.contentResolver)
            }
        }
    )
    LaunchedEffect(importState) {
        if (importState is ImportState.Completed && importState.error == null) {
            navigateToNetwork()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = if (!isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.background
            ),
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(fraction = if (!isCompactWidth()) 0.5f else 1f)
                .fillMaxHeight()
                .verticalScroll(state = rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.size(size = 48.dp))
            Image(
                modifier = Modifier.size(size = 80.dp),
                painter = painterResource(drawable.ic_mesh),
                contentDescription = null,
                colorFilter = ColorFilter.tint(color = MaterialTheme.colorScheme.secondary)
            )
            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = stringResource(R.string.label_app_welcome_rationale),
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.label_start_creating),
                textAlign = TextAlign.Center
            )
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                configurations.forEachIndexed { index, config ->
                    SegmentedButton(
                        modifier = Modifier.defaultMinSize(minWidth = 60.dp),
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = configurations.size
                        ),
                        onClick = { onConfigurationSelected(config) },
                        selected = config == configuration,
                        icon = {
                            SegmentedButtonDefaults.Icon(active = config == configuration) {
                                Icon(
                                    imageVector = config.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = config.description(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
                Spacer(modifier = Modifier.size(size = 16.dp))
                if (isCompactWidth()) {
                    MeshIconButton(
                        buttonIcon = Icons.Outlined.Download,
                        buttonIconTint = MaterialTheme.colorScheme.primary,
                        onClick = { fileLauncher.launch("application/json") }
                    )
                } else {
                    MeshOutlinedButton(
                        buttonIcon = Icons.Outlined.Download,
                        text = stringResource(R.string.label_import),
                        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.primary),
                        buttonIconTint = MaterialTheme.colorScheme.primary,
                        textColor = MaterialTheme.colorScheme.primary,
                        onClick = { fileLauncher.launch("application/json") }
                    )
                }
            }
            ConfigurationProperty.entries.forEach { property ->
                val networkProperties = configuration as NetworkProperties
                ElevatedCardItem(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    imageVector = property.icon(),
                    title = property.description(),
                    titleAction = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (property) {
                                    ConfigurationProperty.NETWORK_KEYS -> "${networkProperties.networkKeys}"
                                    ConfigurationProperty.APPLICATION_KEYS -> "${networkProperties.applicationKeys}"
                                    ConfigurationProperty.GROUPS -> "${networkProperties.groups}"
                                    ConfigurationProperty.VIRTUAL_GROUPS -> "${configuration.virtualGroups}"
                                    ConfigurationProperty.SCENES -> "${networkProperties.scenes}"
                                }
                            )
                            Manage(
                                configuration = configuration,
                                property = property,
                                add = add,
                                remove = remove
                            )
                        }
                    },
                    subtitle = when (configuration) {
                        is Configuration.Empty if (property == ConfigurationProperty.NETWORK_KEYS ||
                                property == ConfigurationProperty.APPLICATION_KEYS) -> "Random"

                        is Configuration.Custom if (property == ConfigurationProperty.NETWORK_KEYS ||
                                property == ConfigurationProperty.APPLICATION_KEYS) -> "Random"

                        is Configuration.Debug if (property == ConfigurationProperty.NETWORK_KEYS ||
                                property == ConfigurationProperty.APPLICATION_KEYS) -> "Fixed"

                        else -> null
                    }
                )
            }
            Spacer(modifier = Modifier.size(size = 16.dp))
            MeshOutlinedButton(
                buttonIcon = Icons.AutoMirrored.Outlined.ArrowForward,
                buttonIconTint = nordicGreen,
                border = BorderStroke(width = 1.dp, color = nordicGreen),
                textColor = nordicGreen,
                text = stringResource(R.string.label_continue),
                onClick = dropUnlessResumed {
                    onContinuePressed()
                    navigateToNetwork()
                }
            )
            Spacer(modifier = Modifier.size(size = 72.dp))
        }
    }

    if (importState is ImportState.Completed && importState.error != null) {
        MeshAlertDialog(
            icon = Icons.Outlined.Download,
            iconColor = Color.Red,
            title = stringResource(R.string.label_import),
            text = importState.error.message?.let {
                stringResource(R.string.label_error_while_importing_a_network, it)
            } ?: stringResource(R.string.label_unknown_error_while_importing),
            confirmButtonText = stringResource(android.R.string.ok),
            onConfirmClick = onImportErrorAcknowledged,
            dismissButtonText = null,
            onDismissRequest = onImportErrorAcknowledged
        )
    }
}

@Composable
private fun Manage(
    configuration: Configuration,
    property: ConfigurationProperty,
    add: (ConfigurationProperty) -> Unit,
    remove: (ConfigurationProperty) -> Unit,
) {
    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
        Action.entries.forEach { option ->
            IconButton(
                enabled = configuration != Configuration.Empty,
                onClick = {
                    when (option) {
                        Action.ADD -> add(property)
                        Action.REMOVE -> remove(property)
                    }
                },
                content = {
                    Icon(
                        imageVector = when (option) {
                            Action.ADD -> Icons.Outlined.Add
                            Action.REMOVE -> Icons.Outlined.Remove
                        },
                        contentDescription = null
                    )
                }
            )
        }
    }
}