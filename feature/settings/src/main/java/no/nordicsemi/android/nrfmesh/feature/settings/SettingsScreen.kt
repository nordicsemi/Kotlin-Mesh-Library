package no.nordicsemi.android.nrfmesh.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.navigation.ClickableSetting
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItemTextField
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import java.text.DateFormat
import java.util.Date
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
internal fun SettingsListScreen(
    uiState: SettingsScreenUiState,
    highlightSelectedItem: Boolean,
    onNameChanged: (String) -> Unit,
    navigateToProvisioners: () -> Unit,
    navigateToNetworkKeys: () -> Unit,
    navigateToApplicationKeys: () -> Unit,
    navigateToScenes: () -> Unit,
    navigateToIvIndex: () -> Unit,
    navigateToDeveloperSettings: () -> Unit,
) {
    when (uiState.networkState) {
        is MeshNetworkState.Success -> {
            SettingsScreen(
                settingsListData = uiState.networkState.settings,
                selectedSetting = uiState.selectedSetting,
                highlightSelectedItem = highlightSelectedItem,
                onNameChanged = onNameChanged,
                onProvisionersPressed = navigateToProvisioners,
                onNetworkKeysPressed = navigateToNetworkKeys,
                onApplicationKeysPressed = navigateToApplicationKeys,
                onScenesPressed = navigateToScenes,
                onIvIndexPressed = navigateToIvIndex,
                onDeveloperSettingsPressed = navigateToDeveloperSettings
            )
        }

        else -> {

        }
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun SettingsScreen(
    settingsListData: SettingsListData,
    selectedSetting: ClickableSetting?,
    highlightSelectedItem: Boolean,
    onNameChanged: (String) -> Unit,
    onProvisionersPressed: () -> Unit,
    onNetworkKeysPressed: () -> Unit,
    onApplicationKeysPressed: () -> Unit,
    onScenesPressed: () -> Unit,
    onIvIndexPressed: () -> Unit,
    onDeveloperSettingsPressed: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState())
    ) {
        SectionTitle(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp),
            title = stringResource(R.string.label_configuration)
        )
        NetworkNameRow(name = settingsListData.name, onNameChanged = onNameChanged)
        ProvisionersRow(
            count = settingsListData.provisioners,
            isSelected = selectedSetting == ClickableSetting.PROVISIONERS && highlightSelectedItem,
            onProvisionersPressed = onProvisionersPressed
        )
        NetworkKeysRow(
            count = settingsListData.networkKeys,
            isSelected = selectedSetting == ClickableSetting.NETWORK_KEYS && highlightSelectedItem,
            onNetworkKeysPressed = onNetworkKeysPressed
        )
        ApplicationKeysRow(
            count = settingsListData.appKeys,
            isSelected = selectedSetting == ClickableSetting.APPLICATION_KEYS && highlightSelectedItem,
            onApplicationKeysPressed = onApplicationKeysPressed
        )
        ScenesRow(
            count = settingsListData.scenes,
            isSelected = selectedSetting == ClickableSetting.SCENES && highlightSelectedItem,
            onScenesPressed = onScenesPressed
        )
        IvIndexRow(
            isSelected = selectedSetting == ClickableSetting.IV_INDEX && highlightSelectedItem,
            onIvIndexPressed = onIvIndexPressed
        )
        LastModifiedTimeRow(timestamp = settingsListData.timestamp)
        SectionTitle(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp),
            title = stringResource(R.string.label_advanced)
        )
        DeveloperSettingsRow(onDeveloperSettingsPressed = onDeveloperSettingsPressed)
        SectionTitle(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp),
            title = stringResource(R.string.label_about)
        )
        VersionNameRow()
        VersionCodeRow()
        Spacer(modifier = Modifier.size(size = 16.dp))
    }
}

@Composable
private fun NetworkNameRow(name: String, onNameChanged: (String) -> Unit) {
    ElevatedCardItemTextField(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Badge,
        title = stringResource(id = R.string.label_name),
        subtitle = name,
        placeholder = stringResource(id = R.string.label_placeholder_network_name),
        onValueChanged = onNameChanged
    )
}

@Composable
private fun ProvisionersRow(
    count: Int,
    isSelected: Boolean,
    onProvisionersPressed: () -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        colors = when (isSelected) {
            true -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )

            else -> CardDefaults.outlinedCardColors()
        },
        onClick = onProvisionersPressed,
        imageVector = Icons.Outlined.Groups,
        title = stringResource(R.string.label_provisioners),
        subtitle = "$count ${if (count == 1) "provisioner" else "provisioners"} available"
    )
}

@Composable
private fun NetworkKeysRow(
    count: Int,
    isSelected: Boolean,
    onNetworkKeysPressed: () -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        colors = when (isSelected) {
            true -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )

            else -> CardDefaults.outlinedCardColors()
        },
        onClick = onNetworkKeysPressed,
        imageVector = Icons.Outlined.VpnKey,
        title = stringResource(R.string.label_network_keys),
        subtitle = "$count ${if (count == 1) "key" else "keys"} available"
    )
}

@Composable
private fun ApplicationKeysRow(
    count: Int,
    isSelected: Boolean,
    onApplicationKeysPressed: () -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        colors = when (isSelected) {
            true -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )

            else -> CardDefaults.outlinedCardColors()
        },
        onClick = onApplicationKeysPressed,
        imageVector = Icons.Outlined.VpnKey,
        title = stringResource(R.string.label_application_keys),
        subtitle = "$count ${if (count == 1) "key" else "keys"} available"
    )
}

@Composable
private fun ScenesRow(
    count: Int,
    isSelected: Boolean,
    onScenesPressed: () -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        colors = when (isSelected) {
            true -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )

            else -> CardDefaults.outlinedCardColors()
        },
        onClick = onScenesPressed,
        imageVector = Icons.Outlined.AutoAwesome,
        title = stringResource(R.string.label_scenes),
        subtitle = "$count ${if (count == 1) "scene" else "scenes"} available"
    )
}

@Composable
private fun IvIndexRow(isSelected: Boolean, onIvIndexPressed: () -> Unit) {
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        colors = when (isSelected) {
            true -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )

            else -> CardDefaults.outlinedCardColors()
        },
        imageVector = Icons.AutoMirrored.Outlined.List,
        title = stringResource(R.string.label_iv_index),
        onClick = onIvIndexPressed
    )
}

@OptIn(ExperimentalTime::class)
@Composable
private fun LastModifiedTimeRow(timestamp: Instant) {
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        imageVector = Icons.Outlined.Update,
        title = stringResource(R.string.label_last_modified),
        subtitle = DateFormat.getDateTimeInstance().format(
            Date(timestamp.toEpochMilliseconds())
        )
    )
}

@OptIn(ExperimentalTime::class)
@Composable
private fun DeveloperSettingsRow(onDeveloperSettingsPressed: () -> Unit) {
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.DeveloperMode,
        title = stringResource(R.string.label_developer_settings),
        onClick = onDeveloperSettingsPressed
    )
}


@Composable
private fun VersionNameRow() {
    // TODO Clarify version naming
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Subtitles,
        title = stringResource(R.string.label_version),
        subtitle = BuildConfig.VERSION_NAME
    )
}

@Composable
private fun VersionCodeRow() {
    // TODO Clarify version code
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        imageVector = Icons.Outlined.DataObject,
        title = stringResource(R.string.label_version_code),
        subtitle = BuildConfig.VERSION_CODE
    )
}