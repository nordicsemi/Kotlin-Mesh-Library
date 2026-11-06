package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.kotlin.mesh.core.model.Scene
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

/**
 * Modal bottom sheet listing scenes to choose from.
 *
 * @param scenes           Scenes to choose from.
 * @param emptyText        Title shown when there are no scenes to choose from.
 * @param selectedScene    Scene that is marked with a check, if any.
 * @param onDismissRequest Invoked when the sheet is closed.
 * @param onSceneSelected  Invoked with the number of the scene selected from the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SceneSelectionBottomSheet(
    scenes: List<Scene>,
    emptyText: String,
    selectedScene: SceneNumber? = null,
    onDismissRequest: () -> Unit,
    onSceneSelected: (SceneNumber) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismissRequest, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                text = stringResource(R.string.label_select_scene),
                style = MaterialTheme.typography.titleLarge
            )
            if (scenes.isEmpty()) {
                NoScenesAvailable(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 240.dp),
                    title = emptyText
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                items(items = scenes, key = { it.number.toInt() }) { scene ->
                    SceneItem(
                        name = scene.name,
                        sceneNumber = scene.number,
                        isCurrent = scene.number == selectedScene,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                onDismissRequest()
                            }
                            onSceneSelected(scene.number)
                        }
                    )
                }
            }
        }
    }
}
