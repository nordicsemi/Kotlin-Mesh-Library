package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

/**
 * Row displaying a scene, which is used in the list of scenes in the network and when selecting
 * a scene.
 *
 * @param name       Name of the scene.
 * @param sceneNumber Scene number.
 * @param isSelected True if the row is highlighted as selected.
 * @param onClick    Invoked when the row is clicked.
 */
@Composable
fun SceneItem(
    name: String,
    sceneNumber: SceneNumber,
    isSelected: Boolean = false,
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
