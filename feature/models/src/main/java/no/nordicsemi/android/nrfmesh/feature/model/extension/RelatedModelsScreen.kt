package no.nordicsemi.android.nrfmesh.feature.model.extension

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.KeyIdGenerator
import no.nordicsemi.android.nrfmesh.core.data.name
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import no.nordicsemi.kotlin.mesh.core.util.CompanyIdentifier

@Composable
internal fun RelatedModelsScreen(model: Model, onModelClicked: (Model) -> Unit) {
    val directBaseModels = model.directBaseModels
    val directExtendingModels = model.directExtendingModels
    val directBaseModelsOnTheSameElement = directBaseModels
        .filter { it.parentElement == model.parentElement }
    val directBaseModelsOnOtherElements = directBaseModels
        .filter { it.parentElement != model.parentElement }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        directBaseModels(
            directBaseModelsOnTheSameElement = directBaseModelsOnTheSameElement,
            directBaseModelsOnOtherElements = directBaseModelsOnOtherElements,
            onModelClicked = onModelClicked
        )
        directExtendingModels(
            model = model,
            directExtendingModels = directExtendingModels,
            onModelClicked = onModelClicked
        )
    }
}

private fun LazyListScope.directBaseModels(
    directBaseModelsOnTheSameElement: List<Model>,
    directBaseModelsOnOtherElements: List<Model>,
    onModelClicked: (Model) -> Unit,
) {
    item { SectionTitle(title = stringResource(R.string.label_base_models)) }
    // Direct Base Models section.
    if (directBaseModelsOnTheSameElement.isNotEmpty()) {
        items(
            items = directBaseModelsOnTheSameElement,
            key = { KeyIdGenerator.nextId() },
            itemContent = {
                it.Row(onClick = onModelClicked)
            }
        )
    }
    if (directBaseModelsOnTheSameElement.isEmpty() && directBaseModelsOnOtherElements.isEmpty()) {
        item {
            ElevatedCardItem(
                imageVector = Icons.Outlined.Info,
                title = stringResource(R.string.label_root_model_rationale),
            )
        }
    }
    // Direct Base Models from other Elements.
    directBaseModelsOnOtherElements
        .groupedByElement()
        .forEach { pair ->
            item {
                SectionTitle(
                    title = stringResource(
                        R.string.label_base_models_from_other_elements,
                        pair.first.name ?: stringResource(
                            R.string.label_unknown_element_with_name,
                            pair.first.index + 1
                        )
                    )
                )
            }
            items(
                items = pair.second,
                key = { KeyIdGenerator.nextId() },
                itemContent = {
                    it.Row(onClick = onModelClicked)
                }
            )
        }
}

private fun LazyListScope.directExtendingModels(
    model: Model,
    directExtendingModels: List<Model>,
    onModelClicked: (Model) -> Unit,
) {
    // Directs Extending Models section
    val directExtendingModelsOnTheSameElement =
        directExtendingModels.filter { it.parentElement == model.parentElement }
    item { SectionTitle(title = stringResource(R.string.label_extending_models)) }
    if (directExtendingModelsOnTheSameElement.isNotEmpty()) {
        items(
            items = directExtendingModelsOnTheSameElement,
            key = { KeyIdGenerator.nextId() },
            itemContent = { it.Row(onClick = onModelClicked) }
        )
    } else {
        item {
            ElevatedCardItem(
                imageVector = Icons.Outlined.Info,
                title = stringResource(R.string.label_no_extending_models_rationale),
            )
        }
    }
    // Direct Extending Models from other Elements.
    directExtendingModelsFromOtherElements(
        model = model,
        directExtendingModels = directExtendingModels,
        onModelClicked = onModelClicked
    )
    // Other related models per element.
    otherRelatedModelsPerElement(
        model = model,
        directBaseModels = model.directBaseModels,
        directExtendingModels = directExtendingModels,
        onModelClicked = onModelClicked
    )
}

private fun LazyListScope.directExtendingModelsFromOtherElements(
    model: Model,
    directExtendingModels: List<Model>,
    onModelClicked: (Model) -> Unit,
) {
    val directExtendingModelsOnOtherElements =
        directExtendingModels.filter { it.parentElement != model.parentElement }
    directExtendingModelsOnOtherElements.groupedByElement().forEach { pair ->
        item {
            SectionTitle(
                title = stringResource(
                    R.string.label_extending_models_from_other_elements,
                    pair.first.name ?: stringResource(
                        R.string.label_unknown_element_with_name,
                        pair.first.index + 1
                    )
                )
            )
        }
        items(
            items = pair.second,
            key = { KeyIdGenerator.nextId() },
            itemContent = { it.Row(onClick = onModelClicked) })
    }
}

private fun LazyListScope.otherRelatedModelsPerElement(
    model: Model,
    directBaseModels: List<Model>,
    directExtendingModels: List<Model>,
    onModelClicked: (Model) -> Unit,
) {
    val relatedModels = model.relatedModels
        .filter { !directBaseModels.contains(it) && !directExtendingModels.contains(it) }
    val relatedModelsPerElement = relatedModels.groupedByElement()
    relatedModelsPerElement.forEach { pair ->
        item {
            SectionTitle(
                title = stringResource(
                    R.string.label_related_models_on_element, pair.first.name ?: stringResource(
                        R.string.label_unknown_element_with_name,
                        pair.first.index + 1
                    )
                )
            )
        }
        items(
            items = pair.second,
            key = { KeyIdGenerator.nextId() },
            itemContent = { it.Row(onClick = onModelClicked) }
        )
    }
}

private fun List<Model>.groupedByElement(): List<Pair<Element, List<Model>>> {
    val map = mutableMapOf<Element, MutableList<Model>>()
    forEach { model ->
        model.parentElement?.let { element ->
            map.getOrPut(element) { mutableListOf() }.add(model)
        }
    }

    return map
        // Map from a map to a list of pairs.
        .map { (element, models) -> element to models }
        // Sort by Element index.
        .sortedBy { it.first.index }
}

@Composable
fun Model.Row(onClick: ((Model) -> Unit)? = null) {
    ElevatedCardItem(
        imageVector = Icons.Outlined.Widgets,
        title = name ?: name(),
        subtitle = when (modelId) {
            is SigModelId -> "Bluetooth SIG"
            is VendorModelId -> CompanyIdentifier.name(
                id = (modelId as VendorModelId).companyIdentifier
            ) ?: stringResource(R.string.label_unknown_vendor)
        },
        // TODO should look in to navigating in to the element screen in large screen devices
        //onClick = dropUnlessResumed {
        //    onClick?.invoke(this)
        //}
    )
}