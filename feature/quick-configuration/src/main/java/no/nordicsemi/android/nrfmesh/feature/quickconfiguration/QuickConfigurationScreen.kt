package no.nordicsemi.android.nrfmesh.feature.quickconfiguration

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.common.name
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.outlined.Check
import no.nordicsemi.android.nrfmesh.core.ui.errorColor
import no.nordicsemi.android.nrfmesh.core.ui.successColor
import no.nordicsemi.android.nrfmesh.core.ui.warningColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.ui.view.NordicAppBar
import no.nordicsemi.android.nrfmesh.core.data.configurator.TaskStatus
import no.nordicsemi.android.nrfmesh.core.data.name
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.Row
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.android.nrfmesh.feature.model.common.ApplicationKeys
import no.nordicsemi.android.nrfmesh.feature.model.common.Destination
import no.nordicsemi.android.nrfmesh.feature.model.common.FriendshipCredential
import no.nordicsemi.android.nrfmesh.feature.model.common.PeriodicPublishingInterval
import no.nordicsemi.android.nrfmesh.feature.model.common.RetransmissionCountAndInterval
import no.nordicsemi.android.nrfmesh.feature.model.common.Ttl
import no.nordicsemi.kotlin.mesh.core.model.Credentials
import no.nordicsemi.kotlin.mesh.core.model.FixedGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.PublicationAddress
import no.nordicsemi.kotlin.mesh.core.model.PublishPeriod
import no.nordicsemi.kotlin.mesh.core.model.Retransmit
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.Model
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuickConfigurationScreen(
    uiState: QuickConfigurationUiState,
    onModeSelected: (QuickConfigurationMode) -> Unit,
    onKeyToggled: (ApplicationKey) -> Unit,
    onGroupToggled: (Group) -> Unit,
    onFixedGroupToggled: (FixedGroupAddress) -> Unit,
    onAllFixedGroupsToggled: () -> Unit,
    onPublishKeySelected: (ApplicationKey) -> Unit,
    onDestinationSelected: (PublicationAddress) -> Unit,
    onTtlChanged: (Int) -> Unit,
    onPeriodChanged: (PublishPeriod) -> Unit,
    onCredentialsChanged: (Credentials) -> Unit,
    onRetransmitChanged: (Retransmit) -> Unit,
    onModelToggled: (Model) -> Unit,
    onAllModelsToggled: () -> Unit,
    onAllKeysToggled: () -> Unit,
    onAllGroupsToggled: () -> Unit,
    onAddKeyClicked: () -> Unit,
    onAddGroupClicked: () -> Unit,
    onApplyPressed: () -> Unit,
    onRetryPressed: () -> Unit,
    onFailureDismissed: () -> Unit,
    onClose: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { QuickConfigurationStep.entries.size })
    val currentStep = QuickConfigurationStep.entries[pagerState.currentPage]
    // All the tasks have finished, whether they succeeded, failed or were skipped.
    val isCompleted = uiState.nothingToConfigure || (uiState.tasks.isNotEmpty() &&
            uiState.tasks.none { it.status is TaskStatus.Idle || it.status is TaskStatus.InProgress })
    val canRetry = isCompleted && uiState.tasks.any { it.status !is TaskStatus.Completed }
    val canProceed = when (currentStep) {
        QuickConfigurationStep.Selection -> uiState.hasSelection
        QuickConfigurationStep.Models -> uiState.selectedModels.isNotEmpty()
        QuickConfigurationStep.Configuration -> false
    }
    // The dialog ignores the system back by default. On the first step nothing has been started
    // yet, so it behaves like the close button, on the next step it goes to the previous step. The
    // configuration cannot be interrupted by accident.
    BackHandler {
        when (currentStep) {
            QuickConfigurationStep.Selection -> onClose()
            QuickConfigurationStep.Models -> scope.launch {
                pagerState.requestScrollToPage(page = pagerState.currentPage - 1)
            }

            QuickConfigurationStep.Configuration -> Unit
        }
    }
    uiState.failure?.let {
        MeshMessageStatusDialog(
            text = it.describe(),
            showDismissButton = false,
            onDismissRequest = onFailureDismissed,
            // Shown over this full screen dialog, so it keeps the platform width.
            usePlatformDefaultWidth = true
        )
    }
    Scaffold(
        modifier = Modifier.consumeWindowInsets(WindowInsets(0)),
        topBar = {
            NordicAppBar(
                title = { Text(text = stringResource(R.string.label_quick_configuration)) },
                showBackButton = true,
                backButtonIcon = Icons.Outlined.Close,
                onNavigationButtonClick = onClose,
            )
        }
    ) { paddingValues ->
        HorizontalPager(
            modifier = Modifier.padding(paddingValues = paddingValues),
            state = pagerState,
            userScrollEnabled = false
        ) { pageIndex ->
            val step = QuickConfigurationStep.entries[pageIndex]
            val scrollState = rememberScrollState()
            var viewportHeight by remember { mutableIntStateOf(0) }
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(fraction = if (!isCompactWidth()) 0.5f else 1f)
                        .fillMaxHeight()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(weight = 1f)
                            .fillMaxWidth()
                            .onSizeChanged { viewportHeight = it.height }
                            .verticalScroll(state = scrollState),
                        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        when (step) {
                            QuickConfigurationStep.Selection -> SelectionPage(
                                uiState = uiState,
                                onModeSelected = onModeSelected,
                                onKeyToggled = onKeyToggled,
                                onGroupToggled = onGroupToggled,
                                onFixedGroupToggled = onFixedGroupToggled,
                                onAllFixedGroupsToggled = onAllFixedGroupsToggled,
                                onPublishKeySelected = onPublishKeySelected,
                                onDestinationSelected = onDestinationSelected,
                                onTtlChanged = onTtlChanged,
                                onPeriodChanged = onPeriodChanged,
                                onCredentialsChanged = onCredentialsChanged,
                                onRetransmitChanged = onRetransmitChanged,
                                onAllKeysToggled = onAllKeysToggled,
                                onAllGroupsToggled = onAllGroupsToggled,
                                onAddKeyClicked = onAddKeyClicked,
                                onAddGroupClicked = onAddGroupClicked
                            )

                            QuickConfigurationStep.Models -> ModelsPage(
                                uiState = uiState,
                                onModelToggled = onModelToggled,
                                onAllModelsToggled = onAllModelsToggled
                            )

                            QuickConfigurationStep.Configuration -> ConfigurationPage(uiState, scrollState, viewportHeight)
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(
                            space = 32.dp,
                            alignment = Alignment.CenterHorizontally
                        )
                    ) {
                        if (step == QuickConfigurationStep.Configuration) {
                            // Same style as the buttons of the configuration tasks sheet.
                            MeshOutlinedButton(
                                enabled = canRetry,
                                onClick = onRetryPressed,
                                buttonIcon = Icons.Outlined.Refresh,
                                buttonIconTint = warningColor.takeIf { canRetry },
                                text = stringResource(R.string.label_retry),
                                textColor = if (canRetry) warningColor else Color.Unspecified,
                                border = BorderStroke(width = 1.dp, color = warningColor)
                                    .takeIf { canRetry }
                                    ?: ButtonDefaults.outlinedButtonBorder(enabled = false)
                            )
                            MeshOutlinedButton(
                                onClick = onClose,
                                buttonIcon = if (isCompleted) Icons.Outlined.Check else Icons.Outlined.Cancel,
                                buttonIconTint = if (isCompleted) successColor else errorColor,
                                text = stringResource(
                                    if (isCompleted) R.string.label_completed else R.string.label_cancel
                                ),
                                textColor = if (isCompleted) successColor else errorColor,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isCompleted) successColor else errorColor
                                )
                            )
                        } else {
                            ButtonRow(
                                text = stringResource(R.string.label_back),
                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                enabled = currentStep != QuickConfigurationStep.Selection,
                                onClick = dropUnlessResumed {
                                    scope.launch {
                                        pagerState.requestScrollToPage(page = pagerState.currentPage - 1)
                                    }
                                }
                            )
                            ButtonRow(
                                text = stringResource(R.string.label_next),
                                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                                iconAtEnd = true,
                                enabled = canProceed,
                                onClick = dropUnlessResumed {
                                    // The tasks are created and executed when the last page is shown.
                                    if (currentStep == QuickConfigurationStep.Models) onApplyPressed()
                                    scope.launch {
                                        pagerState.requestScrollToPage(page = pagerState.currentPage + 1)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.ButtonRow(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconAtEnd: Boolean = false,
) {
    OutlinedButton(
        modifier = Modifier
            .widthIn(min = 80.dp)
            .weight(weight = 1f),
        enabled = enabled,
        onClick = onClick
    ) {
        if (!iconAtEnd) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text)
        if (iconAtEnd) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = icon, contentDescription = null)
        }
    }
}

@Composable
private fun SelectionPage(
    uiState: QuickConfigurationUiState,
    onModeSelected: (QuickConfigurationMode) -> Unit,
    onKeyToggled: (ApplicationKey) -> Unit,
    onGroupToggled: (Group) -> Unit,
    onFixedGroupToggled: (FixedGroupAddress) -> Unit,
    onAllFixedGroupsToggled: () -> Unit,
    onPublishKeySelected: (ApplicationKey) -> Unit,
    onDestinationSelected: (PublicationAddress) -> Unit,
    onTtlChanged: (Int) -> Unit,
    onPeriodChanged: (PublishPeriod) -> Unit,
    onCredentialsChanged: (Credentials) -> Unit,
    onRetransmitChanged: (Retransmit) -> Unit,
    onAllKeysToggled: () -> Unit,
    onAllGroupsToggled: () -> Unit,
    onAddKeyClicked: () -> Unit,
    onAddGroupClicked: () -> Unit,
) {
    var isInfoDialogVisible by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ModeSelector(
            modifier = Modifier.weight(weight = 1f),
            mode = uiState.mode,
            onModeChanged = onModeSelected
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Info,
            onClick = { isInfoDialogVisible = true }
        )
    }
    if (isInfoDialogVisible) {
        // The mode selected in the dialog is only used to read about the modes, it doesn't
        // change the mode selected on the screen.
        var infoMode by rememberSaveable { mutableStateOf(uiState.mode) }
        MeshAlertDialog(
            onDismissRequest = { isInfoDialogVisible = false },
            confirmButtonText = stringResource(R.string.label_close),
            onConfirmClick = { isInfoDialogVisible = false },
            dismissButtonText = null,
            icon = Icons.Outlined.Info,
            iconColor = AlertDialogDefaults.iconContentColor
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
                ModeSelector(
                    modifier = Modifier.fillMaxWidth(),
                    mode = infoMode,
                    onModeChanged = { infoMode = it },
                    showIcons = false
                )
                // All the texts are laid out on top of each other, so the dialog is as tall as the
                // longest one and doesn't resize when switching between the modes.
                Box {
                    QuickConfigurationMode.entries.forEach { entry ->
                        val isSelected = entry == infoMode
                        Text(
                            modifier = Modifier
                                .alpha(alpha = if (isSelected) 1f else 0f)
                                .then(if (isSelected) Modifier else Modifier.clearAndSetSemantics {}),
                            text = stringResource(entry.description())
                        )
                    }
                }
            }
        }
    }
    when (uiState.mode) {
        QuickConfigurationMode.Bind -> {
            SelectableHeader(
                title = stringResource(R.string.label_application_keys),
                allSelected = uiState.applicationKeys.isNotEmpty() &&
                        uiState.selectedKeys.size == uiState.applicationKeys.size,
                enabled = uiState.applicationKeys.isNotEmpty(),
                onSelectAllClicked = onAllKeysToggled,
                onAddClicked = onAddKeyClicked
            )
            if (uiState.applicationKeys.isEmpty()) NoKeys()
            uiState.applicationKeys.forEach { key ->
                key.Row(
                    onClick = { onKeyToggled(key) },
                    titleAction = {
                        Checkbox(modifier = Modifier.padding(end = 16.dp), checked = key in uiState.selectedKeys, onCheckedChange = null)
                    }
                )
            }
        }

        QuickConfigurationMode.Subscribe -> {
            SelectableHeader(
                title = stringResource(R.string.label_groups),
                allSelected = uiState.groups.isNotEmpty() &&
                        uiState.selectedGroups.size == uiState.groups.size,
                enabled = uiState.groups.isNotEmpty(),
                onSelectAllClicked = onAllGroupsToggled,
                onAddClicked = onAddGroupClicked
            )
            if (uiState.groups.isEmpty()) NoGroups()
            uiState.groups.forEach { group ->
                GroupRow(group = group, onClick = { onGroupToggled(group) }) {
                    Checkbox(modifier = Modifier.padding(end = 16.dp), checked = group in uiState.selectedGroups, onCheckedChange = null)
                }
            }
            SelectableHeader(
                title = stringResource(R.string.label_fixed_groups),
                allSelected = uiState.selectedFixedGroups.size == uiState.fixedGroups.size,
                enabled = uiState.fixedGroups.isNotEmpty(),
                onSelectAllClicked = onAllFixedGroupsToggled
            )
            uiState.fixedGroups.forEach { address ->
                ElevatedCardItem(
                    imageVector = Icons.Outlined.GroupWork,
                    title = address.name(),
                    subtitle = "0x${address.address.toHexString(HexFormat.UpperCase)}",
                    onClick = { onFixedGroupToggled(address) },
                    titleAction = {
                        Checkbox(
                            modifier = Modifier.padding(end = 16.dp),
                            checked = address in uiState.selectedFixedGroups,
                            onCheckedChange = null
                        )
                    }
                )
            }
        }

        QuickConfigurationMode.Publish -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SectionTitle(
                    modifier = Modifier.weight(weight = 1f),
                    title = stringResource(R.string.label_application_key)
                )
                MeshIconButton(buttonIcon = Icons.Outlined.Add, onClick = onAddKeyClicked)
            }
            if (uiState.applicationKeys.isEmpty()) NoKeys()
            // The shared publication controls add their own horizontal padding.
            Box(modifier = Modifier.widenBy(32.dp)) {
                ApplicationKeys(
                    keys = uiState.applicationKeys,
                    selectedKeyIndex = uiState.publishKey?.index?.toInt() ?: -1,
                    onApplicationKeySelected = { index ->
                        uiState.applicationKeys
                            .firstOrNull { it.index.toInt() == index }
                            ?.let(onPublishKeySelected)
                    }
                )
            }
            SectionTitle(title = stringResource(R.string.label_destination))
            // The shared publication controls add their own horizontal padding.
            Column(
                modifier = Modifier.widenBy(32.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                Destination(
                    network = uiState.node?.network,
                    destination = uiState.destination,
                    onDestinationSelected = onDestinationSelected
                )
                Ttl(ttl = uiState.ttl, onTtlChanged = onTtlChanged)
                PeriodicPublishingInterval(
                    publishPeriod = uiState.period,
                    onPeriodChanged = onPeriodChanged
                )
                FriendshipCredential(
                    credentials = uiState.credentials,
                    onCredentialsChanged = onCredentialsChanged
                )
                RetransmissionCountAndInterval(
                    retransmit = uiState.retransmit,
                    onRetransmitChanged = onRetransmitChanged
                )
            }
        }
    }
}

@Composable
private fun ModelsPage(
    uiState: QuickConfigurationUiState,
    onModelToggled: (Model) -> Unit,
    onAllModelsToggled: () -> Unit,
) {
    val models = uiState.models
    SelectableHeader(
        title = stringResource(R.string.label_models),
        allSelected = models.isNotEmpty() && uiState.selectedModels.size == models.size,
        enabled = models.isNotEmpty(),
        onSelectAllClicked = onAllModelsToggled
    )
    if (models.isEmpty()) {
        MeshNoItemsAvailable(
            modifier = Modifier.fillMaxWidth(),
            imageVector = Icons.Outlined.Widgets,
            title = stringResource(R.string.label_no_models)
        )
        return
    }
    models.forEach { model ->
        ElevatedCardItem(
            imageVector = Icons.Outlined.Widgets,
            title = model.name(),
            subtitle = model.parentElement?.let {
                it.name ?: stringResource(R.string.label_element, it.index + 1)
            },
            onClick = { onModelToggled(model) },
            titleAction = {
                Checkbox(
                    modifier = Modifier.padding(end = 16.dp),
                    checked = model in uiState.selectedModels,
                    onCheckedChange = null
                )
            }
        )
    }
}

@Composable
private fun ConfigurationPage(
    uiState: QuickConfigurationUiState,
    scrollState: ScrollState,
    viewportHeight: Int,
) {
    // The list follows the task being executed until the user scrolls it, and again after a retry.
    var followTasks by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(scrollState) {
        scrollState.interactionSource.interactions.collect {
            if (it is DragInteraction.Start) followTasks = false
        }
    }
    LaunchedEffect(uiState.tasks.all { it.status == TaskStatus.Idle }) {
        if (uiState.tasks.all { it.status == TaskStatus.Idle }) followTasks = true
    }
    // The vertical range of each task in the scrolled content.
    val bounds = remember { mutableStateMapOf<Int, ClosedFloatingPointRange<Float>>() }
    val runningIndex = uiState.tasks.indexOfFirst { it.status == TaskStatus.InProgress }
    LaunchedEffect(runningIndex, followTasks, bounds[runningIndex]) {
        val running = bounds[runningIndex]
        if (!followTasks || running == null) return@LaunchedEffect
        fun isVisible(range: ClosedFloatingPointRange<Float>) =
            range.start >= scrollState.value &&
                    range.endInclusive <= scrollState.value + viewportHeight
        if (isVisible(running)) return@LaunchedEffect
        // The list is only scrolled once all the tasks that are visible have completed.
        val visibleTasksCompleted = uiState.tasks.indices.all { index ->
            val range = bounds[index]
            range == null || !isVisible(range) ||
                    uiState.tasks[index].status.let { it != TaskStatus.Idle && it != TaskStatus.InProgress }
        }
        if (visibleTasksCompleted) scrollState.animateScrollTo(value = running.start.roundToInt())
    }
    SectionTitle(
        modifier = Modifier.padding(top = 8.dp),
        title = stringResource(R.string.label_configuration)
    )
    if (uiState.nothingToConfigure) {
        // Everything that was selected is already configured on the node.
        uiState.alreadyConfigured.forEach {
            ElevatedCardItem(
                imageVector = when (it.mode) {
                    QuickConfigurationMode.Subscribe -> Icons.Outlined.GroupWork
                    else -> Icons.Outlined.AddLink
                },
                title = when (it.mode) {
                    QuickConfigurationMode.Subscribe -> stringResource(
                        R.string.label_model_already_subscribed, it.modelName, it.itemName
                    )

                    else -> stringResource(
                        R.string.label_key_already_bound, it.itemName, it.modelName
                    )
                },
                subtitle = stringResource(R.string.label_no_configuration_required),
                subtitleTextColor = successColor
            )
        }
        return
    }
    uiState.tasks.forEachIndexed { index, task ->
        ElevatedCardItem(
            modifier = Modifier.onGloballyPositioned {
                val top = it.positionInParent().y
                bounds[index] = top..(top + it.size.height)
            },
            imageVector = task.icon,
            title = task.label,
            subtitle = task.status.description(),
            subtitleTextColor = task.status.color()
        )
    }
}

/** Segmented buttons to select one of the [QuickConfigurationMode]s. */
@Composable
private fun ModeSelector(
    mode: QuickConfigurationMode,
    onModeChanged: (QuickConfigurationMode) -> Unit,
    modifier: Modifier = Modifier,
    showIcons: Boolean = true,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        QuickConfigurationMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                modifier = Modifier.defaultMinSize(minWidth = 60.dp),
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = QuickConfigurationMode.entries.size
                ),
                onClick = { onModeChanged(entry) },
                selected = entry == mode,
                icon = {
                    if (showIcons) {
                        SegmentedButtonDefaults.Icon(active = entry == mode) {
                            Icon(
                                imageVector = entry.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    }
                },
                label = {
                    Text(
                        text = stringResource(entry.label()),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

private fun QuickConfigurationMode.icon(): ImageVector = when (this) {
    QuickConfigurationMode.Bind -> Icons.Outlined.AddLink
    QuickConfigurationMode.Subscribe -> Icons.Outlined.GroupWork
    QuickConfigurationMode.Publish -> Icons.Outlined.Publish
}

private fun QuickConfigurationMode.label(): Int = when (this) {
    QuickConfigurationMode.Bind -> R.string.label_bind
    QuickConfigurationMode.Subscribe -> R.string.label_subscribe
    QuickConfigurationMode.Publish -> R.string.label_publication
}

private fun QuickConfigurationMode.description(): Int = when (this) {
    QuickConfigurationMode.Bind -> R.string.label_bind_app_keys_rationale
    QuickConfigurationMode.Subscribe -> R.string.label_subscribe_rationale
    QuickConfigurationMode.Publish -> R.string.label_set_publication_rationale
}

/** A section title with a button that selects or deselects all the items of the section. */
/**
 * Negative padding is not allowed, so the content is widened by [extra] and shifted to compensate.
 * It is used for the shared publication controls, which add their own horizontal padding.
 */
private fun Modifier.widenBy(extra: androidx.compose.ui.unit.Dp) = layout { measurable, constraints ->
    val px = extra.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.maxWidth + px, maxWidth = constraints.maxWidth + px)
    )
    layout(constraints.maxWidth, placeable.height) { placeable.place(x = -px / 2, y = 0) }
}

@Composable
private fun SelectableHeader(
    title: String,
    allSelected: Boolean,
    enabled: Boolean,
    onSelectAllClicked: () -> Unit,
    onAddClicked: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(modifier = Modifier.weight(weight = 1f), title = title)
        onAddClicked?.let {
            MeshIconButton(buttonIcon = Icons.Outlined.Add, onClick = it)
        }
        MeshIconButton(
            buttonIcon = when (allSelected) {
                true -> Icons.Outlined.CheckBox
                false -> Icons.Outlined.CheckBoxOutlineBlank
            },
            onClick = onSelectAllClicked,
            enabled = enabled
        )
    }
}

@Composable
private fun GroupRow(
    group: Group,
    onClick: () -> Unit,
    titleAction: @Composable () -> Unit,
) {
    ElevatedCardItem(
        imageVector = Icons.Outlined.GroupWork,
        title = group.name,
        subtitle = "0x${group.address.address.toHexString(HexFormat.UpperCase)}",
        onClick = onClick,
        titleAction = titleAction
    )
}

@Composable
private fun NoKeys() = MeshNoItemsAvailable(
    modifier = Modifier.fillMaxWidth(),
    imageVector = Icons.Outlined.AddLink,
    title = stringResource(R.string.label_no_application_keys)
)

@Composable
private fun NoGroups() = MeshNoItemsAvailable(
    modifier = Modifier.fillMaxWidth(),
    imageVector = Icons.Outlined.GroupWork,
    title = stringResource(R.string.label_no_groups)
)
