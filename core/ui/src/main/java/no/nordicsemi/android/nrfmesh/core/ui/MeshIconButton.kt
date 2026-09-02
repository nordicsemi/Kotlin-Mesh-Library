package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Common outlined button composable that is used throughout the app.
 *
 * @param modifier                  Modifier to be used for the button.
 * @param isOnClickActionInProgress Boolean flag that will indicate if the action is in progress and
 *                                  this will show a progress indicator in front of the button text.
 * @param buttonIcon                ImageVector to be shown in the button icon.
 * @param buttonIconTint            Color to be used as tint for the button icon.
 * @param onClick                   Action to be performed on button click.
 * @param enabled                   Flag to enable or disable the button.
 *
 */
@Composable
fun MeshIconButton(
    modifier: Modifier = Modifier,
    isOnClickActionInProgress: Boolean = false,
    buttonIcon: ImageVector,
    buttonIconTint: Color? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        content = {
            if (isOnClickActionInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier.size(size = 24.dp),
                    color = buttonIconTint ?: ProgressIndicatorDefaults.circularColor
                )
            } else {
                Icon(
                    imageVector = buttonIcon,
                    contentDescription = null,
                    tint = (buttonIconTint ?: MaterialTheme.colorScheme.primary).copy(
                        alpha = if (enabled) 1f else 0.38f
                    )
                )
            }
        }
    )
}