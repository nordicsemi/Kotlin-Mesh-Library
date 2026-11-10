package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.ui.view.WarningView


@Composable
fun PlaceHolder(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    text: String,
) {
    OutlinedCard(
        modifier = modifier
            .navigationBarsPadding()
            .padding(top = 48.dp, end = 16.dp, bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp,
            bottomEnd = 16.dp,
            bottomStart = 16.dp
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            WarningView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(all = 8.dp),
                painterResource = rememberVectorPainter(imageVector),
                title = {
                    Text(
                        text = text,
                        textAlign = TextAlign.Center
                    )
                },
                hint = {}
            )
        }
    }
}