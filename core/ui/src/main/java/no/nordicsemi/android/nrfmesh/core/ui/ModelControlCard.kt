package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.theme.nordicFall
import no.nordicsemi.android.common.theme.nordicGrass

/** Icons and colors used by the model control cards. */
object ModelControlCardDefaults {
    val OnOffIcon: ImageVector = Icons.Outlined.Lightbulb
    val LevelIcon: ImageVector = Icons.Outlined.LightMode
    val OnOffTint: Color @Composable get() = nordicGrass
    val LevelTint: Color @Composable get() = nordicFall

    /** Height of the row below the icon that holds the buttons. */
    val ControlsHeight = 40.dp
}

/**
 * A card with a large icon on top, the controls below it and an optional footer. The content is
 * centered vertically, so if the card is taller than its content the content stays in the middle.
 *
 * @param modifier Modifier to be applied to the card.
 * @param icon     Icon shown at the top of the card.
 * @param tint     Tint of the icon.
 * @param onClick  Invoked when the card is clicked. The card is not clickable if null.
 * @param controls Content shown below the icon.
 * @param footer   Content shown below the controls.
 */
@Composable
fun ModelControlCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color,
    onClick: (() -> Unit)? = null,
    controls: @Composable ColumnScope.() -> Unit = {},
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(
                space = 8.dp,
                alignment = Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                modifier = Modifier.size(50.dp),
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
            controls()
            footer()
        }
    }
    when (onClick) {
        null -> OutlinedCard(modifier = modifier, content = content)
        else -> OutlinedCard(modifier = modifier, onClick = onClick, content = content)
    }
}

/** A [ModelControlCard] with ON and OFF buttons for a Generic OnOff model. */
@Composable
fun GenericOnOffCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = ModelControlCardDefaults.OnOffTint,
    onOn: () -> Unit,
    onOff: () -> Unit,
    onClick: (() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    ModelControlCard(
        modifier = modifier,
        icon = ModelControlCardDefaults.OnOffIcon,
        tint = tint,
        onClick = onClick,
        controls = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ModelControlCardDefaults.ControlsHeight),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(enabled = enabled, onClick = onOn) {
                    Text(text = stringResource(R.string.label_control_on).uppercase())
                }
                TextButton(enabled = enabled, onClick = onOff) {
                    Text(text = stringResource(R.string.label_control_off).uppercase())
                }
            }
        },
        footer = footer
    )
}

/** A [ModelControlCard] with - and + buttons for a Generic Level model. */
@Composable
fun GenericLevelCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = ModelControlCardDefaults.LevelTint,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onClick: (() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    ModelControlCard(
        modifier = modifier,
        icon = ModelControlCardDefaults.LevelIcon,
        tint = tint,
        onClick = onClick,
        controls = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ModelControlCardDefaults.ControlsHeight),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(enabled = enabled, onClick = onMinus) {
                    Text(
                        text = stringResource(R.string.label_control_minus),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                TextButton(enabled = enabled, onClick = onPlus) {
                    Text(
                        text = stringResource(R.string.label_control_plus),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        },
        footer = footer
    )
}
