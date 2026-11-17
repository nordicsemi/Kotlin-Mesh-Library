package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * A modal bottom sheet with a title, optional subtitles and the given content below them.
 *
 * @param title            Title of the sheet.
 * @param subtitles        Optional lines of text shown below the title.
 * @param actions          Optional actions, e.g. icon buttons, shown at the end of the title row.
 * @param onDismissRequest Invoked when the sheet is closed.
 * @param content          Content of the sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelInfoBottomSheet(
    title: String,
    subtitles: List<String> = emptyList(),
    onDismissRequest: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
        ) {
            // The actions are aligned with the title, the subtitles are shown below them.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    modifier = Modifier.weight(weight = 1f),
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )
                Row(content = actions)
            }
            subtitles.forEach {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
            BottomInsetSpacer(spacing = 16.dp)
        }
    }
}

/**
 * A card with a circular icon, a title and an optional subtitle, to be used in the content of a
 * [ModelInfoBottomSheet].
 */
@Composable
fun ModelInfoItem(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    title: String,
    subtitle: String? = null,
) {
    OutlinedCard(modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        MeshTwoLineListItem(
            imageVector = imageVector,
            title = title,
            subtitle = subtitle
        )
    }
}

