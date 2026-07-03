package no.nordicsemi.android.nrfmesh.feature.model.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import no.nordicsemi.android.nrfmesh.core.data.name
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.ModelId
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import no.nordicsemi.kotlin.mesh.core.util.CompanyIdentifier


@Composable
internal fun CommonInformation(model: Model, onRelatedModelsClicked: (Model) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        NameRow(name = model.name())
        ModelIdRow(modelId = model.modelId)
        Company(modelId = model.modelId)
        RelatedModels(model = model, onRelatedModelsClicked = onRelatedModelsClicked)
    }
}

@Composable
private fun NameRow(name: String) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Badge,
        title = stringResource(R.string.label_name),
        subtitle = name
    )
}

@Composable
private fun ModelIdRow(modelId: ModelId) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_model_identifier),
        subtitle = when (modelId) {
            is SigModelId -> modelId.modelIdentifier.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )

            is VendorModelId -> modelId.modelIdentifier.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )
        }
    )
}

@Composable
private fun Company(modelId: ModelId) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.WorkOutline,
        title = stringResource(id = R.string.label_company),
        subtitle = when (modelId) {
            is SigModelId -> "Bluetooth SIG"
            is VendorModelId -> CompanyIdentifier.name(id = modelId.companyIdentifier)
                ?: stringResource(id = R.string.label_unknown)
        }
    )
}

@Composable
private fun RelatedModels(model: Model, onRelatedModelsClicked: (Model) -> Unit) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.DeviceHub,
        title = stringResource(id = R.string.label_related_models),
        subtitle = "${model.relatedModels.size}",
        onClick = dropUnlessResumed { onRelatedModelsClicked(model) }
    )
}