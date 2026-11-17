package no.nordicsemi.android.nrfmesh.feature.localnode

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import no.nordicsemi.android.common.theme.nordicGrass
import no.nordicsemi.android.nrfmesh.core.common.name
import no.nordicsemi.android.nrfmesh.core.common.publishKey
import no.nordicsemi.android.nrfmesh.core.common.publishDestination
import no.nordicsemi.android.nrfmesh.core.data.model.GenericLevelServer
import no.nordicsemi.android.nrfmesh.core.data.model.GenericOnOffServer
import no.nordicsemi.android.nrfmesh.core.data.model.GenericState
import no.nordicsemi.android.nrfmesh.core.data.name
import no.nordicsemi.android.nrfmesh.core.ui.GenericLevelCard
import no.nordicsemi.android.nrfmesh.core.ui.GenericOnOffCard
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.ModelInfoItem
import no.nordicsemi.android.nrfmesh.core.ui.ModelInfoBottomSheet
import no.nordicsemi.android.nrfmesh.core.ui.ModelControlCard
import no.nordicsemi.android.nrfmesh.core.ui.ModelControlCardDefaults
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.kotlin.mesh.core.model.FixedGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun LocalNodeScreen(
    uiState: LocalNodeUiState,
    onOnOff: (Model, Boolean) -> Unit,
    onLevelStep: (Model, Boolean) -> Unit,
    navigateToModel: (Model) -> Unit,
) {
    if (uiState.sections.isEmpty()) {
        MeshNoItemsAvailable(
            modifier = Modifier.fillMaxSize(),
            imageVector = Icons.Outlined.Lightbulb,
            title = stringResource(R.string.label_no_local_node),
            rationale = stringResource(R.string.label_no_local_node_rationale)
        )
        return
    }
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        uiState.sections.forEach { section ->
            item(
                key = "element-${section.element.index}",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                SectionTitle(
                    title = section.element.name
                        ?: stringResource(R.string.label_element_n, section.element.index + 1)
                )
            }
            items(
                items = section.models,
                key = { "${section.element.index}-${it.modelId.id}" }
            ) { model ->
                ModelControlCard(
                    model = model,
                    isEnabled = uiState.isEnabled,
                    onOnOff = onOnOff,
                    onLevelStep = onLevelStep,
                    navigateToModel = navigateToModel
                )
            }
        }
    }
}

@Composable
private fun ModelControlCard(
    model: Model,
    isEnabled: Boolean,
    onOnOff: (Model, Boolean) -> Unit,
    onLevelStep: (Model, Boolean) -> Unit,
    navigateToModel: (Model) -> Unit,
) {
    var showInfo by remember { mutableStateOf(false) }
    val name = when (model.modelId) {
        is VendorModelId -> stringResource(R.string.label_simple_on_off_client)
        else -> model.name().takeIf { it.isNotBlank() }
            ?: stringResource(R.string.label_unknown_model)
    }
    // The content of each card is centered, so the cards share the same height.
    val cardModifier = Modifier.height(130.dp)
    val onClick = { showInfo = true }
    when (model.controlType()) {
        ControlType.ON_OFF_CLIENT -> GenericOnOffCard(
            modifier = cardModifier,
            enabled = isEnabled,
            // The simple vendor model uses the primary color, while the Generic OnOff Client uses
            // the same color as the Generic OnOff Server.
            tint = when (model.modelId) {
                is VendorModelId -> MaterialTheme.colorScheme.primary
                else -> nordicGrass
            },
            onOn = { onOnOff(model, true) },
            onOff = { onOnOff(model, false) },
            onClick = onClick
        )

        ControlType.LEVEL_CLIENT -> GenericLevelCard(
            modifier = cardModifier,
            enabled = isEnabled,
            onMinus = { onLevelStep(model, false) },
            onPlus = { onLevelStep(model, true) },
            onClick = onClick
        )

        ControlType.ON_OFF_SERVER -> OnOffServerCard(
            modifier = cardModifier,
            model = model,
            onClick = onClick
        )

        ControlType.LEVEL_SERVER -> LevelServerCard(
            modifier = cardModifier,
            model = model,
            onClick = onClick
        )

        null -> Unit
    }
    if (showInfo) {
        ModelSheet(
            model = model,
            name = name,
            onConfigure = {
                showInfo = false
                navigateToModel(model)
            },
            onDismissRequest = { showInfo = false }
        )
    }
}

/**
 * A bottom sheet with the groups the model is subscribed to and publishes to, along with the
 * application keys used, and a button to configure the model.
 */
@Composable
private fun ModelSheet(
    model: Model,
    name: String,
    onConfigure: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    val element = model.parentElement
    val network = element?.parentNode?.network
    val keys = model.boundApplicationKeys.joinToString { it.name }.takeIf { it.isNotEmpty() }
    val publication = model.publish?.takeUnless { it.isCanceled }
    // Fixed group addresses, such as All Nodes, are subscribed to by default so are not shown.
    val subscriptions = model.subscribe.filterNot { it is FixedGroupAddress }
    val elementName = element?.name ?: stringResource(
        R.string.label_element_n,
        (element?.index ?: 0) + 1
    )
    val nodeName = element?.parentNode?.name
    ModelInfoBottomSheet(
        title = name,
        subtitles = listOfNotNull(
            nodeName?.let { stringResource(R.string.label_element_in_node, elementName, it) }
                ?: elementName,
            element?.unicastAddress?.let {
                stringResource(R.string.label_element_address, it.toHexString())
            }
        ),
        onDismissRequest = onDismissRequest,
        actions = {
            MeshIconButton(
                buttonIcon = Icons.Outlined.Settings,
                buttonIconTint = MaterialTheme.colorScheme.primary,
                onClick = onConfigure
            )
        }
    ) {
        if (subscriptions.isEmpty() && publication == null) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                text = stringResource(
                    R.string.label_model_not_configured,
                    name,
                    element?.name ?: stringResource(
                        R.string.label_element_n,
                        (element?.index ?: 0) + 1
                    )
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (subscriptions.isNotEmpty()) {
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                title = stringResource(R.string.label_model_subscribed)
            )
            subscriptions.forEach { address ->
                ModelInfoItem(
                    imageVector = Icons.Outlined.GroupWork,
                    title = (address as? MeshAddress)?.let { groupLabel(network, it) } ?: "?",
                    subtitle = keys
                )
            }
        }
        publication?.let {
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                title = stringResource(R.string.label_model_publishes_to)
            )
            ModelInfoItem(
                imageVector = Icons.Outlined.Flag,
                title = (it.address as MeshAddress).let { destination ->
                    // Only groups are shown with their address, as the name of an element in a node
                    // already identifies it.
                    if (destination is UnicastAddress) {
                        model.publishDestination() ?: destination.toHexString()
                    } else groupLabel(network, destination)
                },
                subtitle = model.publishKey()?.name
                    ?: stringResource(R.string.label_key_index, it.index.toInt())
            )
        }
    }
}

/**
 * Returns the name of the group with its address in brackets, the name of a fixed group or the hex
 * value of the given address.
 */
private fun groupLabel(network: MeshNetwork?, address: MeshAddress): String = when (address) {
    is FixedGroupAddress -> address.name()
    else -> network?.group(address = address.address)?.name
        ?.let { "$it (${address.toHexString()})" }
        ?: address.toHexString()
}

/**
 * A card with a bulb that lights up while the Generic OnOff Server model of the local node is on.
 * As the Mesh Model specification defines, the state turns on as soon as a transition starts, and
 * turns off when the transition ends.
 */
@Composable
private fun OnOffServerCard(
    modifier: Modifier,
    model: Model,
    onClick: () -> Unit,
) {
    var state by remember { mutableStateOf(GenericState(value = false)) }
    DisposableEffect(model) {
        val server = model.eventHandler as? GenericOnOffServer
        server?.observer = { state = it }
        onDispose { server?.observer = null }
    }
    var isOn by remember { mutableStateOf(state.value) }
    LaunchedEffect(state) {
        val transition = state.transition
        when {
            transition == null -> isOn = state.value
            transition.targetValue -> isOn = true
            else -> {
                isOn = true
                delay(transition.remainingTime + 100.milliseconds)
                isOn = false
            }
        }
    }
    val tint by animateColorAsState(
        targetValue = if (isOn) nordicGrass else nordicGrass.copy(alpha = 0.4f),
        label = "OnOffServerTint"
    )
    ModelControlCard(
        modifier = modifier,
        icon = ModelControlCardDefaults.OnOffIcon,
        tint = tint,
        onClick = onClick
    )
}

/**
 * A card with an icon and a bar that fill up according to the Generic Level Server level of the
 * local node.
 */
@Composable
private fun LevelServerCard(
    modifier: Modifier,
    model: Model,
    onClick: () -> Unit,
) {
    var state by remember { mutableStateOf(GenericState(value = Short.MIN_VALUE)) }
    DisposableEffect(model) {
        val server = model.eventHandler as? GenericLevelServer
        server?.observer = { state = it }
        onDispose { server?.observer = null }
    }
    val transition = state.transition
    val target = transition?.targetValue ?: when {
        state.animation == null -> state.value
        state.animation!!.speed > 0 -> Short.MAX_VALUE
        else -> Short.MIN_VALUE
    }
    val fraction by animateFloatAsState(
        targetValue = (target.toInt() - Short.MIN_VALUE) / (UShort.MAX_VALUE.toFloat()),
        animationSpec = tween(
            durationMillis = transition?.remainingTime?.inWholeMilliseconds?.toInt() ?: 0,
            easing = LinearEasing
        ),
        label = "LevelServerFraction"
    )
    val level = fraction.coerceIn(0f, 1f)
    ModelControlCard(
        modifier = modifier,
        icon = ModelControlCardDefaults.LevelIcon,
        tint = ModelControlCardDefaults.LevelTint.copy(alpha = 0.4f + 0.6f * level),
        onClick = onClick,
        controls = {
            LinearProgressIndicator(
                modifier = Modifier.width(80.dp),
                progress = { level },
                color = ModelControlCardDefaults.LevelTint,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    )
}
