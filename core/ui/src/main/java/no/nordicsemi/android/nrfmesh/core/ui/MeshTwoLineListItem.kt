@file:Suppress("unused")

package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun MeshTwoLineListItem(
    modifier: Modifier = Modifier,
    title: String,
    titleTextOverflow: TextOverflow = TextOverflow.Ellipsis,
    subtitle: String? = null,
    subtitleMaxLines: Int = 1,
    subtitleTextColor: Color = LocalTextStyle.current.color,
    subtitleTextOverflow: TextOverflow = TextOverflow.Ellipsis
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = titleTextOverflow
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = subtitleMaxLines,
                    color = subtitleTextColor,
                    overflow = subtitleTextOverflow
                )
            }
        }
    }
}

@Composable
fun MeshTwoLineListItem(
    modifier: Modifier = Modifier,
    leadingComposable: @Composable () -> Unit = {},
    title: String,
    titleTextOverflow: TextOverflow = TextOverflow.Ellipsis,
    subtitle: String? = null,
    subtitleMaxLines: Int = 1,
    subtitleTextColor: Color = LocalTextStyle.current.color,
    subtitleTextOverflow: TextOverflow = TextOverflow.Ellipsis,
    trailingComposable: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingComposable()
        Column(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = titleTextOverflow
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    modifier = Modifier.padding(end = 16.dp),
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = subtitleMaxLines,
                    color = subtitleTextColor,
                    overflow = subtitleTextOverflow
                )
            }
        }
        trailingComposable()
    }
}

@Composable
fun MeshTwoLineListItem(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    title: String,
    subtitle: String? = null,
    subtitleMaxLines: Int = 1,
    subtitleTextColor: Color = LocalTextStyle.current.color,
    subtitleTextOverflow: TextOverflow = TextOverflow.Ellipsis,
    trailingComposable: @Composable () -> Unit = {},
) {
    MeshTwoLineListItem(
        modifier = modifier,
        leadingComposable = {
            Icon(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .size(24.dp),
                imageVector = imageVector,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = title,
        subtitle = subtitle,
        subtitleMaxLines = subtitleMaxLines,
        subtitleTextColor = subtitleTextColor,
        subtitleTextOverflow = subtitleTextOverflow,
        trailingComposable = trailingComposable
    )
}


@Composable
fun TwoLineRangeListItem(
    modifier: Modifier = Modifier,
    leadingComposable: @Composable () -> Unit = {},
    title: String,
    titleTextOverflow: TextOverflow = TextOverflow.Clip,
    lineTwo: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .height(height = 80.dp)
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingComposable()
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = titleTextOverflow
            )
            Spacer(modifier = Modifier.size(4.dp))
            lineTwo()
        }
    }
}


@Composable
fun SingleLineRangeListItem(
    modifier: Modifier = Modifier,
    leadingComposable: @Composable () -> Unit = {},
    title: String,
    titleTextOverflow: TextOverflow = TextOverflow.Clip,
    trailingComposable: @Composable () -> Unit = {},
) {
    Row(
        modifier = modifier
            .padding(vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leadingComposable()
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = titleTextOverflow
        )
        trailingComposable()
    }
}