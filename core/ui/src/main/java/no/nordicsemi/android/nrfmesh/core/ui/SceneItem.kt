package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

/**
 * Row displaying a scene, which is used in the list of scenes in the network and when selecting
 * a scene.
 *
 * @param name       Name of the scene.
 * @param sceneNumber Scene number.
 * @param isSelected True if the row is highlighted as selected.
 * @param isCurrent  True if the scene is the current scene, which is marked with a check.
 * @param onClick    Invoked when the row is clicked.
 */
@Composable
fun SceneItem(
    name: String,
    sceneNumber: SceneNumber,
    isSelected: Boolean = false,
    isCurrent: Boolean = false,
    onClick: () -> Unit,
) {
    ElevatedCardItem(
        onClick = onClick,
        colors = when (isSelected) {
            true -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )

            else -> CardDefaults.outlinedCardColors()
        },
        imageVector = Icons.Outlined.AutoAwesome,
        title = name,
        titleAction = {
            if (isCurrent) {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = Icons.Outlined.CheckCircle,
                    tint = Color.Green,
                    contentDescription = null
                )
            }
        },
        subtitle = stringResource(
            R.string.label_scene_number_value,
            sceneNumber.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )
        )
    )
}

/**
 * Empty state shown when there are no scenes to display.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param title    Title of the empty state.
 */
@Composable
fun NoScenesAvailable(
    modifier: Modifier = Modifier.fillMaxSize(),
    title: String = stringResource(R.string.no_scenes_currently_added),
) {
    MeshNoItemsAvailable(
        modifier = modifier,
        imageVector = Icons.Outlined.AutoAwesome,
        title = title
    )
}
