@file:Suppress("unused")

package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MeshSingleLineListItem(
    modifier: Modifier = Modifier,
    leadingComposable: @Composable () -> Unit = {},
    title: String,
    titleTextOverflow: TextOverflow = TextOverflow.Clip,
    trailingComposable: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height = 60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingComposable()
        Text(
            modifier = Modifier.weight(weight = 1f),
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = titleTextOverflow
        )
        trailingComposable()
    }
}


@Composable
fun MeshSingleLineListItem(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    title: String,
    titleTextOverflow: TextOverflow = TextOverflow.Ellipsis,
    trailingComposable: @Composable () -> Unit = {},
) {
    MeshSingleLineListItem(
        modifier = modifier,
        leadingComposable = {
            Icon(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(24.dp),
                imageVector = imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = title,
        titleTextOverflow = titleTextOverflow,
        trailingComposable = trailingComposable
    )
}