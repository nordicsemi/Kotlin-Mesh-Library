package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Common outlined button composable that is used throughout the app.
 *
 * @param modifier                  Modifier to be used for the button.
 * @param isOnClickActionInProgress Boolean flag that will indicate if the action is in progress and
 *                                  this will show a progress indicator in front of the button text.
 * @param buttonIcon                ImageVector to be shown int he button icon.
 * @param text                      Text to be shown on the button.
 * @param onClick                   Action to be performed on button click.
 * @param enabled                   Flag to enable or disable the button.
 * @param minWidth                  Minimum width of the button, so that different buttons can be
 *                                  given the same width.
 *
 */
@Composable
fun MeshOutlinedButton(
    modifier: Modifier = Modifier,
    isOnClickActionInProgress: Boolean = false,
    buttonIcon: ImageVector,
    buttonIconTint: Color? = null,
    text: String,
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit,
    enabled: Boolean = true,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled = enabled),
    minWidth: Dp = 120.dp,
) {
    OutlinedButton(
        modifier = modifier.widthIn(min = minWidth),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        border = border,
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
                    tint = buttonIconTint ?: LocalContentColor.current,
                )
            }
            Text(modifier = modifier.padding(start = 8.dp), text = text, color = textColor)
        }
    )
}