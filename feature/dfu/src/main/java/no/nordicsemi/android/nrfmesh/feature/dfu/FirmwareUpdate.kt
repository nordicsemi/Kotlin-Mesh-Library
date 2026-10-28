package no.nordicsemi.android.nrfmesh.feature.dfu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.ui.view.NordicAppBar
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.Distribution
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.DistributionParameters
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.DistributorSelection
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.FirmwareSelection
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.LePairing
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.NodeConfiguration
import no.nordicsemi.android.nrfmesh.feature.dfu.util.DfuParameters
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Receiver
import no.nordicsemi.android.nrfmesh.feature.dfu.util.UpdatePackage
import no.nordicsemi.kotlin.mesh.core.model.Model


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FirmwareUpdateScreen(
    uiState: FirmwareUpdateScreenUiState,
    session: DfuSession,
    onGattProxyClicked: () -> Unit,
    onBindAppKeysClicked: (Model) -> Unit,
    onBackClick: () -> Unit,
    onClearTasks: () -> Unit,
    onKeyIndexChanged: (Int) -> Unit,
    onMaxReceiversListSizeChanged: (Int) -> Unit,
    onOngoingFirmwareSizeChanged: (Int?) -> Unit,
    onReceiversChanged: (List<Receiver>) -> Unit,
    onUpdatePackageChanged: (UpdatePackage?) -> Unit,
    onDfuParametersChanged: (DfuParameters) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { FirmwareUpdateStep.entries.size })
    val currentStep = FirmwareUpdateStep.entries[pagerState.currentPage]
    var isPaired by rememberSaveable { mutableStateOf(false) }
    var tasksCompleted by rememberSaveable { mutableStateOf(false) }
    var isUploadCompleted by rememberSaveable { mutableStateOf(false) }
    var isDistributorConnected by rememberSaveable { mutableStateOf(false) }
    var option by rememberSaveable { mutableStateOf(FirmwareUpdateOptions.SMP) }
    // The name of the Distributor, when it is selected in addition to other targets.
    var distributorWithTargets by rememberSaveable { mutableStateOf<String?>(null) }
    var isDistributorWarningVisible by rememberSaveable { mutableStateOf(false) }
    // The firmware can only be distributed through the Distributor that was chosen, so the user
    // cannot proceed from the firmware selection if another node is connected. Only SMP is
    // supported, so the user cannot proceed from the first page with any other option.
    val enableNext = session.canProceed(
        step = currentStep,
        isPaired = isPaired,
        tasksCompleted = tasksCompleted
    ) && (currentStep != FirmwareUpdateStep.FirmwareSelection || isDistributorConnected) &&
            (currentStep != FirmwareUpdateStep.DistributorSelection || option == FirmwareUpdateOptions.SMP)
    fun goToNextStep() {
        // The tasks enqueued on the node configuration page have been executed, they are cleared
        // so they are not duplicated when it is shown again.
        if (currentStep == FirmwareUpdateStep.NodeConfiguration) onClearTasks()
        scope.launch {
            pagerState.requestScrollToPage(
                page = when {
                    // A distribution that is in progress can only be followed on the last page.
                    currentStep == FirmwareUpdateStep.DistributorSelection &&
                            session.ongoingFirmwareSize != null ->
                        FirmwareUpdateStep.last.ordinal

                    else -> pagerState.currentPage + 1
                }
            )
        }
    }
    if (isDistributorWarningVisible) MeshAlertDialog(
        onDismissRequest = { isDistributorWarningVisible = false },
        icon = Icons.Rounded.WarningAmber,
        iconColor = MaterialTheme.colorScheme.error,
        title = stringResource(R.string.label_distributor_selected_title),
        text = stringResource(
            R.string.label_distributor_selected_message,
            distributorWithTargets.orEmpty()
        ),
        confirmButtonText = stringResource(android.R.string.ok),
        onConfirmClick = {
            isDistributorWarningVisible = false
            goToNextStep()
        },
        dismissButtonText = stringResource(R.string.label_cancel),
        onDismissClick = { isDistributorWarningVisible = false }
    )
    Scaffold(
        modifier = Modifier.consumeWindowInsets(WindowInsets(0)),
        topBar = {
            NordicAppBar(
                title = { Text(text = stringResource(R.string.label_firmware_update)) },
                showBackButton = true,
                // Once the image is uploaded, the screen can be minimized as the distribution
                // continues on the Distributor.
                backButtonIcon = when (currentStep == FirmwareUpdateStep.last && isUploadCompleted) {
                    true -> Icons.Outlined.KeyboardArrowDown
                    false -> Icons.Outlined.Close
                },
                onNavigationButtonClick = onBackClick,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        HorizontalPager(
            modifier = Modifier
                .padding(paddingValues = paddingValues),
            state = pagerState,
            userScrollEnabled = false
        ) { pageIndex ->
            val step = FirmwareUpdateStep.entries[pageIndex]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(fraction = if (!isCompactWidth()) 0.5f else 1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp),
                    content = {
                        // The page takes the space above the buttons, which stay at the bottom of
                        // the screen. The last page scrolls its content on its own.
                        Column(
                            modifier = Modifier
                                .weight(weight = 1f)
                                .fillMaxWidth()
                                .then(
                                    if (step == FirmwareUpdateStep.last) Modifier
                                    else Modifier.verticalScroll(state = rememberScrollState())
                                ),
                            content = {
                                when (step) {
                                    FirmwareUpdateStep.DistributorSelection -> DistributorSelection(
                                        option = option,
                                        onOptionChanged = { option = it },
                                        onGattProxyClicked = onGattProxyClicked,
                                        onBindAppKeysClicked = onBindAppKeysClicked,
                                        onKeySelected = { index -> onKeyIndexChanged(index.toInt()) },
                                        onKeyDeselected = { onKeyIndexChanged(-1) },
                                        onMaxReceiversListSizeChanged = onMaxReceiversListSizeChanged,
                                        onOngoingDistributionChanged = { ongoing ->
                                            ongoing?.let { onKeyIndexChanged(it.keyIndex) }
                                            onOngoingFirmwareSizeChanged(ongoing?.firmwareSize)
                                        }
                                    )

                                    FirmwareUpdateStep.LePairing -> LePairing(
                                        goToNext = dropUnlessResumed {
                                            scope.launch {
                                                pagerState.requestScrollToPage(page = pagerState.currentPage + 1)
                                            }
                                        },
                                        onBondedChanged = { isPaired = it }
                                    )

                                    FirmwareUpdateStep.FirmwareSelection -> FirmwareSelection(
                                        index = session.keyIndex,
                                        maxReceiversListSize = session.maxReceiversListSize,
                                        snackbarHostState = snackbarHostState,
                                        onUpdatePackageChanged = onUpdatePackageChanged,
                                        onReceiversSelected = onReceiversChanged,
                                        onDistributorConnectedChanged = { isDistributorConnected = it },
                                        onDistributorWithTargetsChanged = { distributorWithTargets = it }
                                    )

                                    FirmwareUpdateStep.DistributionParameters -> DistributionParameters(onDfuParametersChanged = onDfuParametersChanged)

                                    FirmwareUpdateStep.NodeConfiguration -> NodeConfiguration(
                                        session = session,
                                        onTasksCompleted = { tasksCompleted = it }
                                    )

                                    FirmwareUpdateStep.Distribution -> Distribution(
                                        snackbarHostState = snackbarHostState,
                                        session = session,
                                        onUploadCompletedChanged = { isUploadCompleted = it },
                                        onCancelled = onBackClick
                                    )
                                }
                            }
                        )
                        // The last page has its own button, that cancels the firmware update.
                        if (step != FirmwareUpdateStep.last) Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(space = 32.dp),
                        ) {
                            OutlinedButton(
                                modifier = Modifier
                                    .widthIn(min = 80.dp)
                                    .weight(weight = 1f),
                                onClick = dropUnlessResumed {
                                    // The tasks enqueued on the last page are discarded when
                                    // navigating back, they are enqueued again when returning.
                                    if (currentStep == FirmwareUpdateStep.NodeConfiguration) onClearTasks()
                                    scope.launch {
                                        pagerState.requestScrollToPage(
                                            page = when (currentStep) {
                                                // LE pairing is skipped when going back.
                                                FirmwareUpdateStep.FirmwareSelection ->
                                                    FirmwareUpdateStep.DistributorSelection.ordinal
                                                // Node configuration is skipped when going back. If
                                                // the pages before were not used, i.e. a distribution
                                                // was already running, go to the first page.
                                                FirmwareUpdateStep.Distribution -> when (session.keyIndex >= 0 && session.receivers.isNotEmpty()) {
                                                    true -> FirmwareUpdateStep.DistributionParameters.ordinal
                                                    false -> FirmwareUpdateStep.DistributorSelection.ordinal
                                                }

                                                else -> pagerState.currentPage - 1
                                            }
                                        )
                                    }
                                },
                                enabled = currentStep != FirmwareUpdateStep.DistributorSelection,
                                content = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "Back")
                                }
                            )
                            OutlinedButton(
                                modifier = Modifier
                                    .widthIn(min = 80.dp)
                                    .weight(weight = 1f),
                                onClick = dropUnlessResumed {
                                    // Updating the Distributor with other targets can make it
                                    // restart, so the user has to confirm it.
                                    if (currentStep == FirmwareUpdateStep.FirmwareSelection &&
                                        distributorWithTargets != null
                                    ) isDistributorWarningVisible = true
                                    else goToNextStep()
                                },
                                enabled = enableNext,
                                content = {
                                    Text(text = "Next")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}