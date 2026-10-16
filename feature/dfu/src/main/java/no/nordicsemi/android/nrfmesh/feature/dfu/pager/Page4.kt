package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.nrfmesh.core.data.configurator.MeshTask
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.DfuSession

@Composable
internal fun Page4(session: DfuSession, onTasksCompleted: (Boolean) -> Unit) {
    val viewModel = hiltViewModel<Page4ViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(session) {
        viewModel.enqueue(session = session)
    }
    LaunchedEffect(uiState.isCompleted) {
        onTasksCompleted(uiState.isCompleted)
    }
    Column {
        uiState.targets.forEach { uuid ->
            uiState.tasks[uuid].orEmpty().takeIf {
                it.isNotEmpty()
            }?.let { taskList ->
                SectionTitle(
                    modifier = Modifier.padding(top = 8.dp),
                    title = uiState.names[uuid] ?: "Unknown"
                )
                taskList.forEach { task ->
                    MeshTaskItem(task = task)
                }
            }
        }
    }
}

@Composable
private fun MeshTaskItem(task: MeshTask) {
    ElevatedCardItem(
        modifier = Modifier.padding(top = 8.dp),
        imageVector = task.icon,
        title = task.label,
        subtitle = task.status.description(),
        subtitleTextColor = task.status.color()
    )
}
