package no.nordicsemi.android.nrfmesh.feature.dfu

import androidx.compose.animation.AnimatedVisibility
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
import no.nordicsemi.kotlin.mesh.core.model.Model


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FirmwareUpdateScreen(
    uiState: FirmwareUpdateScreenUiState,
    onGattProxyClicked: () -> Unit,
    onBindAppKeysClicked: (Model) -> Unit,
    onBackClick: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val pagerState = rememberPagerState(pageCount = { 4 })
    var enableNext by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.consumeWindowInsets(WindowInsets(0)),
        topBar = {
            NordicAppBar(
                title = { Text(text = stringResource(R.string.label_firmware_update)) },
                showBackButton = true,
                backButtonIcon = Icons.Outlined.Close,
                onNavigationButtonClick = onBackClick,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) {
        HorizontalPager(
            modifier = Modifier
                .padding(paddingValues = it),
            state = pagerState,
            userScrollEnabled = false
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(fraction = if (!isCompactWidth()) 0.5f else 1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(state = rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp),
                ) {
                    when (page) {
                        0 -> {
                            Page0(
                                onGattProxyClicked = onGattProxyClicked,
                                onBindAppKeysClicked = onBindAppKeysClicked,
                                enableNextStage = {
                                    enableNext = true
                                }
                            )
                        }

                        1 -> Page1(
                            goToNext = dropUnlessResumed {
                                scope.launch {
                                    pagerState.requestScrollToPage(page = pagerState.currentPage + 1)
                                }
                            }
                        )

                        2 -> {

                        }

                        3 -> {

                        }
                    }
                    AnimatedVisibility(visible = enableNext) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(space = 32.dp),
                        ) {
                            OutlinedButton(
                                modifier = Modifier
                                    .widthIn(min = 80.dp)
                                    .weight(1f),
                                onClick = dropUnlessResumed {
                                    scope.launch {
                                        pagerState.requestScrollToPage(
                                            page = when (pagerState.currentPage == 2) {
                                                true -> pagerState.currentPage - 2
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
                                    .weight(1f),
                                onClick = dropUnlessResumed {
                                    scope.launch {
                                        pagerState.requestScrollToPage(page = pagerState.currentPage + 1)
                                    }
                                },
                                enabled = enableNext && pagerState.currentPage < 4,
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
                }
            }
        }
    }
}