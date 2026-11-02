package no.nordicsemi.android.nrfmesh.feature.scenes.scene

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItemTextField
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.scenes.R
import no.nordicsemi.kotlin.mesh.core.model.Scene
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

@Composable
internal fun SceneScreen(uiState: SceneScreenUiState, save: () -> Unit) {
    when (uiState.sceneState) {
        is SceneState.Success -> {
            SceneContent(
                scene = uiState.sceneState.scene,
                save = save
            )
        }

        else -> {}
    }
}

@Composable
internal fun SceneContent(scene: Scene, save: () -> Unit) {
    Column {
        SectionTitle(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp),
            title = stringResource(id = R.string.label_scene)
        )
        Name(
            name = scene.name,
            onNameChanged = {
                scene.name = it
                save()
            }
        )
        Spacer(modifier = Modifier.size(8.dp))
        Number(number = scene.number)
    }
}

@Composable
fun Name(name: String, onNameChanged: (String) -> Unit) {
    ElevatedCardItemTextField(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Badge,
        title = stringResource(id = R.string.label_name),
        subtitle = name,
        onValueChanged = onNameChanged
    )
}

@Composable
fun Number(number: SceneNumber) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.AutoAwesome,
        title = stringResource(id = R.string.label_scene_number),
        subtitle = number.toHexString(
            format = HexFormat {
                this.number.prefix = "0x"
                upperCase = true
            }
        )
    )
}