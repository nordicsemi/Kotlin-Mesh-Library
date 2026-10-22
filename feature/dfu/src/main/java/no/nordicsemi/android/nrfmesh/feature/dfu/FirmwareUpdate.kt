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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.Page0
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.Page1
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.Page2
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.Page3
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.Page4
import no.nordicsemi.android.nrfmesh.feature.dfu.pager.Page5
import no.nordicsemi.android.nrfmesh.feature.dfu.util.DfuParameters
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Receiver
import no.nordicsemi.android.nrfmesh.feature.dfu.util.UpdatePackage
import no.nordicsemi.kotlin.mesh.core.model.Model


/** Index of the last page of the firmware update pager, which shows the distribution. */
private const val LAST_PAGE = 5

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
    val pagerState = rememberPagerState(pageCount = { LAST_PAGE + 1 })
    var isPaired by rememberSaveable { mutableStateOf(false) }
    var tasksCompleted by rememberSaveable { mutableStateOf(false) }
    var isUploadCompleted by rememberSaveable { mutableStateOf(false) }
    val enableNext = session.canProceed(
        page = pagerState.currentPage,
        isPaired = isPaired,
        tasksCompleted = tasksCompleted
    )
    Scaffold(
        modifier = Modifier.consumeWindowInsets(WindowInsets(0)),
        topBar = {
            NordicAppBar(
                title = { Text(text = stringResource(R.string.label_firmware_update)) },
                showBackButton = true,
                // Once the image is uploaded, the screen can be minimized as the distribution
                // continues on the Distributor.
                backButtonIcon = when (pagerState.currentPage == LAST_PAGE && isUploadCompleted) {
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
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(fraction = if (!isCompactWidth()) 0.5f else 1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                        // The last page scrolls its content on its own, to keep its button at the
                        // bottom of the screen.
                        .then(
                            if (page == LAST_PAGE) Modifier
                            else Modifier.verticalScroll(state = rememberScrollState())
                        ),
                    content = {
                        when (page) {
                            0 -> Page0(
                                onGattProxyClicked = onGattProxyClicked,
                                onBindAppKeysClicked = onBindAppKeysClicked,
                                onKeySelected = { index -> onKeyIndexChanged(index.toInt()) },
                                onMaxReceiversListSizeChanged = onMaxReceiversListSizeChanged,
                                onOngoingDistributionChanged = { ongoing ->
                                    ongoing?.let { onKeyIndexChanged(it.keyIndex) }
                                    onOngoingFirmwareSizeChanged(ongoing?.firmwareSize)
                                }
                            )

                            1 -> Page1(
                                goToNext = dropUnlessResumed {
                                    scope.launch {
                                        pagerState.requestScrollToPage(page = pagerState.currentPage + 1)
                                    }
                                },
                                onBondedChanged = { isPaired = it }
                            )

                            2 -> Page2(
                                index = session.keyIndex,
                                maxReceiversListSize = session.maxReceiversListSize,
                                snackbarHostState = snackbarHostState,
                                onUpdatePackageChanged = onUpdatePackageChanged,
                                onReceiversSelected = onReceiversChanged
                            )

                            3 -> Page3(onDfuParametersChanged = onDfuParametersChanged)

                            4 -> Page4(
                                session = session,
                                onTasksCompleted = { tasksCompleted = it }
                            )

                            5 -> Page5(
                                snackbarHostState = snackbarHostState,
                                session = session,
                                onUploadCompletedChanged = { isUploadCompleted = it },
                                onCancelled = onBackClick
                            )
                        }
                        // The last page has its own button, that cancels the firmware update.
                        if (page != LAST_PAGE) Row(
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
                                    if (pagerState.currentPage == 4) onClearTasks()
                                    scope.launch {
                                        pagerState.requestScrollToPage(
                                            page = when (pagerState.currentPage) {
                                                // Page 1 (LE pairing) is skipped when going back.
                                                2 -> 0
                                                // Page 4 (tasks) is skipped when going back. If the
                                                // pages before were not used, i.e. a distribution
                                                // was already running, go to the first page.
                                                5 -> when (session.keyIndex >= 0 && session.receivers.isNotEmpty()) {
                                                    true -> 3
                                                    false -> 0
                                                }

                                                else -> pagerState.currentPage - 1
                                            }
                                        )
                                    }
                                },
                                enabled = pagerState.currentPage > 0,
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
                                    // The tasks enqueued on page 4 have been executed, they are
                                    // cleared so they are not duplicated when page 4 is shown again.
                                    if (pagerState.currentPage == 4) onClearTasks()
                                    scope.launch {
                                        pagerState.requestScrollToPage(
                                            page = when {
                                                // A distribution that is in progress can only be
                                                // followed on the last page.
                                                pagerState.currentPage == 0 &&
                                                        session.ongoingFirmwareSize != null -> LAST_PAGE

                                                else -> pagerState.currentPage + 1
                                            }
                                        )
                                    }
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