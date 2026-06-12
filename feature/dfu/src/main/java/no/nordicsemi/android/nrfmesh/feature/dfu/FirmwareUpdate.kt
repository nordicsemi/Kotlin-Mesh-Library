package no.nordicsemi.android.nrfmesh.feature.dfu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.smp.SmpContent

@Composable
fun FirmwareUpdate() {
    var option by rememberSaveable { mutableStateOf(FirmwareUpdateOptions.SMP) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        SectionTitle(title = stringResource(R.string.label_firmware_update))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            FirmwareUpdateOptions.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    modifier = Modifier.defaultMinSize(minWidth = 60.dp),
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = FirmwareUpdateOptions.entries.size
                    ),
                    onClick = { option = entry },
                    selected = entry == option,
                    icon = {
                        SegmentedButtonDefaults.Icon(active = entry == option) {
                            Icon(
                                imageVector = entry.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = entry.description(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                )
            }
        }
        option.Content()
    }
}

@Composable
fun FirmwareUpdateOptions.Content() {
    when (this) {
        FirmwareUpdateOptions.SMP -> SmpContent()
        FirmwareUpdateOptions.BLOB -> BlobContent()
        FirmwareUpdateOptions.HTTPS -> HttpsContent()
    }
}

@Composable
fun BlobContent() {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_dfu_over_blob_rationale),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
fun HttpsContent() {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_dfu_over_https_rationale),
        style = MaterialTheme.typography.bodySmall
    )
}