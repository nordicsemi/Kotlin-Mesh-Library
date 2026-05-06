package no.nordicsemi.android.nrfmesh.ui.network

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteItem
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.rememberSceneState
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.theme.nordicSun
import no.nordicsemi.android.common.ui.view.NordicAppBar
import no.nordicsemi.android.nrfmesh.R
import no.nordicsemi.android.nrfmesh.core.navigation.MESH_TOP_LEVEL_NAV_ITEMS
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.NodeKey
import no.nordicsemi.android.nrfmesh.core.navigation.SettingsKey
import no.nordicsemi.android.nrfmesh.core.navigation.toEntries
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.android.nrfmesh.feature.export.navigation.ExportKey
import no.nordicsemi.android.nrfmesh.feature.export.navigation.exportEntry
import no.nordicsemi.android.nrfmesh.feature.groups.navigation.groupsEntry
import no.nordicsemi.android.nrfmesh.feature.nodes.navigation.nodesEntry
import no.nordicsemi.android.nrfmesh.feature.provisioning.navigation.provisioningEntry
import no.nordicsemi.android.nrfmesh.feature.proxy.navigation.proxyEntry
import no.nordicsemi.android.nrfmesh.feature.settings.navigation.settingsEntry
import no.nordicsemi.android.nrfmesh.navigation.MeshAppState
import no.nordicsemi.android.nrfmesh.viewmodel.MeshNetworkState
import no.nordicsemi.android.nrfmesh.viewmodel.NetworkScreenUiState
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
internal fun NetworkScreen(
    appState: MeshAppState,
    uiState: NetworkScreenUiState,
    shouldSelectProvisioner: Boolean,
    onProvisionerSelected: (provisioner: Provisioner) -> Unit,
    importNetwork: (uri: Uri, contentResolver: ContentResolver) -> Unit,
    onImportErrorAcknowledged: () -> Unit,
    resetNetwork: () -> Unit,
    navigateToWizard: () -> Unit,
    isCompactWidth: Boolean = isCompactWidth(),
    resetMeshNetworkUiState: () -> Unit,
    startAttentionTimer: (Node) -> Unit,
) {
    when (uiState.networkState) {
        MeshNetworkState.Loading -> {

        }

        is MeshNetworkState.Success -> {
            val context = LocalContext.current
            val topAppBarTitle by remember(
                key1 = appState.navigationState.currentKey,
                key2 = uiState.networkState.network.createKeysForAppTitles()
            ) {
                derivedStateOf {
                    title(
                        context = context,
                        network = uiState.networkState.network,
                        navigationState = appState.navigationState,
                        isCompactWidth = isCompactWidth
                    )
                }
            }
            NetworkContent(
                appState = appState,
                network = uiState.networkState.network,
                shouldSelectProvisioner = shouldSelectProvisioner,
                onProvisionerSelected = onProvisionerSelected,
                importState = uiState.importState,
                importNetwork = importNetwork,
                resetNetwork = resetNetwork,
                navigateToWizard = navigateToWizard,
                topAppBarTitle = topAppBarTitle,
                onImportErrorAcknowledged = onImportErrorAcknowledged,
                startAttentionTimer = startAttentionTimer,
            )
        }

        MeshNetworkState.NoNetwork -> {
            // LaunchedEffect(Unit) {
            //     appState.run {
            //         snackbarHostState.currentSnackbarData?.dismiss()
            //         snackbarHostState.showSnackbar(
            //             message = "This Network has been reset. Returning to the onboarding wizard.",
            //             duration = SnackbarDuration.Indefinite
            //         )
            //     }
            // }
            navigateToWizard()
            resetMeshNetworkUiState()
        }
    }
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalStdlibApi::class,
    ExperimentalUuidApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalTime::class
)
@Composable
private fun NetworkContent(
    appState: MeshAppState,
    network: MeshNetwork,
    shouldSelectProvisioner: Boolean,
    onProvisionerSelected: (provisioner: Provisioner) -> Unit,
    importState: ImportState,
    importNetwork: (uri: Uri, contentResolver: ContentResolver) -> Unit,
    onImportErrorAcknowledged: () -> Unit,
    resetNetwork: () -> Unit,
    topAppBarTitle: String,
    navigateToWizard: () -> Unit,
    startAttentionTimer: (Node) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val selectProvisionerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showResetNetworkDialog by rememberSaveable { mutableStateOf(false) }
    var showAutoConfigurationHealthServerDialog by rememberSaveable { mutableStateOf(false) }
    val navigator = remember { Navigator(appState.navigationState) }
    NavigationSuiteScaffold(
        navigationItemVerticalArrangement = Arrangement.Center,
        navigationItems = {
            MESH_TOP_LEVEL_NAV_ITEMS.forEach { (navKey, navItem) ->
                val selected = navKey == appState.navigationState.currentTopLevelKey
                NavigationSuiteItem(
                    selected = selected,
                    onClick = { if (!selected) navigator.navigate(key = navKey) },
                    icon = {
                        Icon(
                            imageVector = when (selected) {
                                true -> navItem.selectedIcon
                                false -> navItem.unselectedIcon
                            },
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(navItem.iconTextId),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                )
            }
        }
    ) {
        val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
        val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()
        val entryProvider = entryProvider {
            nodesEntry(
                appState = appState,
                navigator = navigator,
                navigateToWizard = {
                    resetNetwork()
                    navigateToWizard()
                }
            )
            provisioningEntry(appState = appState, navigator = navigator)
            groupsEntry(appState = appState, navigator = navigator)
            proxyEntry()
            settingsEntry(appState = appState, navigator = navigator)
            exportEntry(appState = appState, navigator = navigator)
        }
        val entries = appState.navigationState.toEntries(entryProvider = entryProvider)
        val sceneStrategies = listOf(listDetailStrategy, bottomSheetStrategy)
        val sceneState = rememberSceneState(
            entries = entries, sceneStrategies = sceneStrategies, onBack = navigator::goBack
        )
        val scene = sceneState.currentScene
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = appState.snackbarHostState) },
            topBar = {
                NordicAppBar(
                    title = { Text(text = topAppBarTitle) },
                    backButtonIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                    showBackButton = appState.showBackButton,
                    onNavigationButtonClick = {
                        // TODO to be clarified as of now uses the back button behaviour from tbe
                        //  NavDisplay's NavigationBackHandler
                        // If `enabled` becomes stale (e.g., it was set to false but a gesture was
                        // dispatched in the same frame), this may result in no entries being popped
                        // due to entries.size being smaller than scene.previousEntries.size
                        // but that's preferable to crashing with an IndexOutOfBoundsException
                        repeat(entries.size - scene.previousEntries.size) { navigator.goBack() }
                    },
                    actions = {
                        // We have to consider two conditions when displaying the dropdown in the settings screen
                        // 1. Current key destination is settings key
                        // 2. For non-compact width devices the dropdown must be displayed irrespective of where you
                        //    are in the settings screen
                        if (appState.navigationState.currentKey is SettingsKey ||
                            appState.navigationState.currentTopLevelKey is SettingsKey && !isCompactWidth()
                        ) {
                            var menuExpanded by remember { mutableStateOf(false) }
                            DisplayDropdown(
                                menuExpanded = menuExpanded,
                                onExpandPressed = { menuExpanded = true },
                                onDismissRequest = { menuExpanded = false },
                                importNetwork = { uri, contentResolver ->
                                    importNetwork(uri, contentResolver)
                                },
                                navigateToExport = {
                                    menuExpanded = false
                                    navigator.navigate(key = ExportKey)
                                },
                                onResetNetworkClicked = {
                                    menuExpanded = false
                                    showResetNetworkDialog = true
                                }
                            )
                        }
                        // On Node screen, show items for Identify and Configure.
                        val nodeUuid =
                            (appState.navigationState.currentKey as? NodeKey)?.nodeUuid
                                ?: appState.navigationState.currentSubStack
                                    .firstNotNullOfOrNull { (it as? NodeKey)?.nodeUuid }
                                    ?.takeIf { !isCompactWidth() }
                        val node = nodeUuid?.let { network.node(Uuid.parse(it)) }
                        if (node != null) {
                            IconButton(
                                onClick = {
                                    val healthServerModel =
                                        node.primaryElement.models.firstOrNull { it.isHealthServer }
                                            ?: return@IconButton
                                    val hasBoundKeys =
                                        healthServerModel.boundApplicationKeys.isNotEmpty()
                                    if (hasBoundKeys) {
                                        startAttentionTimer(node)
                                    } else {
                                        showAutoConfigurationHealthServerDialog = true
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                )
            }
        ) { padding ->
            NavDisplay(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues = padding),
                entries = entries,
                sceneStrategies = sceneStrategies,
                onBack = navigator::goBack
            )
        }
        if (showResetNetworkDialog) {
            MeshAlertDialog(
                icon = Icons.Outlined.RestartAlt,
                iconColor = Color.Red,
                title = stringResource(R.string.label_reset_network),
                text = stringResource(R.string.label_reset_network_rationale),
                onConfirmClick = {
                    scope.launch {
                        showResetNetworkDialog = false
                    }.invokeOnCompletion {
                        resetNetwork()
                        navigateToWizard()
                    }
                },
                onDismissClick = { showResetNetworkDialog = false },
                onDismissRequest = { showResetNetworkDialog = false })
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
        if (showAutoConfigurationHealthServerDialog) {
            val nodeUuid =
                (appState.navigationState.currentKey as? NodeKey)?.nodeUuid
                    ?: appState.navigationState.currentSubStack
                        .firstNotNullOfOrNull { (it as? NodeKey)?.nodeUuid }
                        ?.takeIf { !isCompactWidth() }
            val node = nodeUuid?.let { network.node(Uuid.parse(it)) }
            if (node != null) {
                MeshAlertDialog(
                    icon = Icons.Outlined.NotificationsActive,
                    iconColor = nordicSun,
                    title = stringResource(R.string.label_identify),
                    text = stringResource(R.string.label_identify_rationale),
                    onConfirmClick = {
                        showAutoConfigurationHealthServerDialog = false
                        startAttentionTimer(node)
                    },
                    onDismissClick = { showAutoConfigurationHealthServerDialog = false },
                    onDismissRequest = { showAutoConfigurationHealthServerDialog = false }
                )
            }
        }
        if (shouldSelectProvisioner) {
            ModalBottomSheet(
                properties = ModalBottomSheetProperties(
                    shouldDismissOnBackPress = false, shouldDismissOnClickOutside = false
                ),
                sheetState = selectProvisionerSheetState,
                sheetGesturesEnabled = false,
                onDismissRequest = { },
                content = {
                    SectionTitle(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .padding(vertical = 8.dp),
                        title = stringResource(R.string.label_select_provisioner_rationale),
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        items(items = network.provisioners, key = { it.uuid.toString() }) {
                            ElevatedCardItem(
                                imageVector = Icons.Filled.PersonPin, title = it.name, onClick = {
                                    onProvisionerSelected(it)
                                    // scope.launch { exportSheetState.hide() }.invokeOnCompletion {
                                    //     if (!exportSheetState.isVisible) {
                                    //         // showExportBottomSheet = false
                                    //     }
                                    // }
                                }
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun DisplayDropdown(
    menuExpanded: Boolean,
    onExpandPressed: () -> Unit,
    onDismissRequest: () -> Unit,
    importNetwork: (uri: Uri, contentResolver: ContentResolver) -> Unit,
    onResetNetworkClicked: () -> Unit,
    navigateToExport: () -> Unit,
) {
    val context = LocalContext.current
    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            importNetwork(uri, context.contentResolver)
        }
    }
    IconButton(onClick = onExpandPressed) {
        Icon(
            imageVector = Icons.Filled.MoreVert,
            contentDescription = null
        )
    }
    DropdownMenu(
        expanded = menuExpanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null
                )
            },
            text = { Text(text = stringResource(R.string.label_import)) },
            onClick = dropUnlessResumed {
                fileLauncher.launch("application/json")
                onDismissRequest()
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Upload,
                    contentDescription = null
                )
            },
            text = { Text(text = stringResource(R.string.label_export)) },
            onClick = dropUnlessResumed { navigateToExport() }
        )
        HorizontalDivider()
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = null
                )
            },
            text = { Text(text = stringResource(R.string.label_reset_network)) },
            onClick = dropUnlessResumed { onResetNetworkClicked() }
        )
    }
}