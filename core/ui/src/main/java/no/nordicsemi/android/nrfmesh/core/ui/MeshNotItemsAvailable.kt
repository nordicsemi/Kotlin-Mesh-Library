package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MeshNoItemsAvailable(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    title: String,
    rationale: String? = null,
    onClickText: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            modifier = Modifier.size(96.dp),
            imageVector = imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.surfaceTint
        )
        Text(
            modifier = Modifier.padding(all = 16.dp),
            textAlign = TextAlign.Center,
            text = title,
        )
        if (!rationale.isNullOrEmpty())
            Text(
                modifier = Modifier.padding(all = 16.dp),
                textAlign = TextAlign.Center,
                text = rationale,
            )
        if (onClickText != null && onClick != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onClick,
                    content = { Text(text = onClickText) }
                )
            }
        }
    }
}