package no.nordicsemi.android.nrfmesh.feature.groups.group

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.isGenericLevelServer
import no.nordicsemi.android.nrfmesh.core.common.isGenericOnOffServer
import no.nordicsemi.android.nrfmesh.core.common.isSceneServer
import no.nordicsemi.android.nrfmesh.core.common.isSceneSetupServer
import no.nordicsemi.android.nrfmesh.core.data.name
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItemTextField
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.groups.R
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.ModelId
import no.nordicsemi.kotlin.mesh.core.model.PrimaryGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import kotlin.uuid.ExperimentalUuidApi

@Composable
internal fun GroupScreen(
    snackbarHostState: SnackbarHostState,
    uiState: GroupScreenUiState,
    onModelClicked: (ModelId, Int) -> Unit,
    isDetailPaneVisible: Boolean,
    deleteGroup: (Group) -> Unit,
    save: () -> Unit,
) {
    when(uiState.groupState) {
        is GroupState.Success -> {
            GroupInfoContent(
                snackbarHostState = snackbarHostState,
                groupInfo = uiState.groupState.groupInfoListData,
                group = uiState.groupState.group,
                onModelClicked = onModelClicked,
                isDetailPaneVisible = isDetailPaneVisible,
                selectedModelIndex = uiState.groupState.selectedModelIndex,
                deleteGroup = deleteGroup,
                save = save
            )
        }
        else -> {

        }
    }
}

@Composable
internal fun GroupInfoContent(
    snackbarHostState: SnackbarHostState,
    groupInfo: GroupInfoListData,
    group: Group,
    onModelClicked: (ModelId, Int) -> Unit,
    isDetailPaneVisible: Boolean,
    selectedModelIndex: Int,
    deleteGroup: (Group) -> Unit,
    save: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.padding(horizontal = 16.dp).padding(top = 8.dp),
            title = stringResource(id = R.string.label_group)
        )
        NodeNameRow(
            name = group.name,
            onNameChanged = {
                group.name = it
                save()
            }
        )
        AddressRow(address = group.address)
        DeleteGroup(
            snackbarHostState = snackbarHostState,
            group = group,
            deleteGroup = { deleteGroup(group) }
        )
        if (groupInfo.models.isNotEmpty()) {
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(id = R.string.label_subscribed_models)
            )
            groupInfo.models.forEach { entry ->
                ModelRow(
                    models = entry.value,
                    onModelClicked = { onModelClicked(it, groupInfo.models.keys.indexOf(it)) },
                    isSelected = isDetailPaneVisible &&
                            groupInfo.models.keys.indexOf(entry.key) == selectedModelIndex
                )
            }
        } else {
            if (!isDetailPaneVisible) {
                SectionTitle(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(id = R.string.label_subscribed_models)
                )
                ElevatedCardItem(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = Icons.Outlined.SportsScore,
                    title = stringResource(R.string.label_no_models_subscribed)
                )
            }
        }
    }
}

@Composable
private fun NodeNameRow(name: String, onNameChanged: (String) -> Unit) {
    ElevatedCardItemTextField(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Badge,
        title = stringResource(id = R.string.label_name),
        subtitle = name,
        onValueChanged = onNameChanged
    )
}

@OptIn(ExperimentalStdlibApi::class, ExperimentalUuidApi::class)
@Composable
private fun AddressRow(address: PrimaryGroupAddress) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Lan,
        title = stringResource(id = R.string.label_address),
        subtitle = if (address is GroupAddress) {
            "0x${address.address.toHexString(format = HexFormat.UpperCase)}"
        } else {
            (address as VirtualAddress).uuid.toString().uppercase()
        }
    )
}

@Composable
private fun DeleteGroup(
    snackbarHostState: SnackbarHostState,
    group: Group,
    deleteGroup: (Group) -> Unit,
) {
    val scope = rememberCoroutineScope()
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Delete,
        title = stringResource(id = R.string.label_delete),
        supportingText = stringResource(id = R.string.label_delete_group_rationale),
    ) {
        MeshOutlinedButton(
            border = BorderStroke(width = 1.dp, color = Color.Red),
            onClick = {
                scope.launch {
                    if (group.isUsed) snackbarHostState.showSnackbar(message = group.name)
                    else deleteGroup(group)
                }
            },
            text = stringResource(R.string.label_delete),
            buttonIcon = Icons.Outlined.Delete,
            buttonIconTint = Color.Red,
            textColor = Color.Red,
        )
    }
}

@Composable
private fun ModelRow(
    models: List<Model>,
    onModelClicked: (ModelId) -> Unit,
    isSelected: Boolean,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = when (isSelected) {
            true -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )

            else -> CardDefaults.outlinedCardColors()
        },
        imageVector = models.first().toIcon(),
        title = models.firstOrNull()?.name() ?: "Unknown Model",
        subtitle = pluralStringResource(R.plurals.label_models_count, models.size, models.size),
        onClick = { onModelClicked(models.first().modelId) }
    )
}

@Composable
private fun Model.toIcon() = if (isGenericOnOffServer()) {
    Icons.Outlined.Lightbulb
} else if (isGenericLevelServer()) {
    Icons.Outlined.LightMode
} else if (isSceneServer() || isSceneSetupServer()) {
    Icons.Outlined.Palette
} else {
    Icons.Outlined.QuestionMark
}