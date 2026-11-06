package no.nordicsemi.android.nrfmesh.feature.nodes.node

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.KeyIdGenerator
import no.nordicsemi.android.nrfmesh.core.data.configurator.MeshTask
import no.nordicsemi.android.nrfmesh.core.data.configurator.TaskStatus
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.core.ui.errorColor
import no.nordicsemi.android.nrfmesh.core.ui.successColor
import no.nordicsemi.android.nrfmesh.core.ui.warningColor
import no.nordicsemi.android.nrfmesh.feature.nodes.R

/**
 * Modal bottom sheet showing the status of the configuration tasks of a messenger.
 *
 * The sheet can be dismissed only once all the tasks have completed. Until then it can be
 * cancelled and the tasks that have failed can be retried.
 *
 * @param tasks               Tasks to be displayed.
 * @param onRetryPressed      Invoked when the user wants to retry the tasks.
 * @param onCompletePressed   Invoked when the sheet is closed after all the tasks have completed.
 * @param onCancelPressed     Invoked when the sheet is closed before all the tasks have completed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfigurationTasksBottomSheet(
    tasks: List<MeshTask>,
    onRetryPressed: () -> Unit,
    onCompletePressed: () -> Unit,
    onCancelPressed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isCompleted = !tasks.any { it.status !is TaskStatus.Completed }
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onCompletePressed,
        sheetGesturesEnabled = isCompleted,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = false,
            shouldDismissOnClickOutside = false
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 16.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(
                modifier = Modifier
                    .weight(weight = 1f)
                    .padding(horizontal = 16.dp),
                title = stringResource(id = R.string.label_configuration)
            )
            // The retry button is orange when there are failed tasks, like the warnings on the DFU screens.
            val canRetry = tasks.any { it.status is TaskStatus.Error }
            MeshOutlinedButton(
                enabled = canRetry,
                onClick = onRetryPressed,
                buttonIcon = Icons.Outlined.Refresh,
                buttonIconTint = warningColor.takeIf { canRetry },
                text = stringResource(R.string.label_retry),
                textColor = if (canRetry) warningColor else Color.Unspecified,
                border = BorderStroke(width = 1.dp, color = warningColor).takeIf { canRetry }
                    ?: ButtonDefaults.outlinedButtonBorder(enabled = false)
            )
            MeshOutlinedButton(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            if (isCompleted) onCompletePressed() else onCancelPressed()
                        }
                    }
                },
                buttonIcon = if (isCompleted) Icons.Outlined.Check else Icons.Outlined.Cancel,
                buttonIconTint = if (isCompleted) successColor else errorColor,
                text = stringResource(if (isCompleted) R.string.label_done else R.string.label_cancel),
                textColor = if (isCompleted) successColor else errorColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isCompleted) successColor else errorColor
                )
            )
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            items(items = tasks, key = { KeyIdGenerator.nextId() }) {
                ElevatedCardItem(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = it.icon,
                    title = it.label,
                    subtitle = it.status.description(),
                    subtitleTextColor = it.status.color()
                )
            }
        }
    }
}
