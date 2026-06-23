package no.nordicsemi.android.nrfmesh.network.util

import android.content.Context
import no.nordicsemi.android.feature.config.networkkeys.navigation.AddNetKeysKey
import no.nordicsemi.android.feature.config.networkkeys.navigation.ConfigNetKeysKey
import no.nordicsemi.android.nrfmesh.R
import no.nordicsemi.android.nrfmesh.core.navigation.FirmwareUpdateKey
import no.nordicsemi.android.nrfmesh.core.navigation.GroupsKey
import no.nordicsemi.android.nrfmesh.core.navigation.NavigationState
import no.nordicsemi.android.nrfmesh.core.navigation.NodeKey
import no.nordicsemi.android.nrfmesh.core.navigation.NodesKey
import no.nordicsemi.android.nrfmesh.core.navigation.ProxyKey
import no.nordicsemi.android.nrfmesh.core.navigation.ScannerKey
import no.nordicsemi.android.nrfmesh.core.navigation.SettingsKey
import no.nordicsemi.android.nrfmesh.feature.application.keys.key.navigation.ApplicationKeyContentKey
import no.nordicsemi.android.nrfmesh.feature.application.keys.navigation.ApplicationKeysContentKey
import no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.navigation.AddAppKeysKey
import no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.navigation.ConfigAppKeysKey
import no.nordicsemi.android.nrfmesh.feature.developer.navigation.DeveloperSettingsContentKey
import no.nordicsemi.android.nrfmesh.feature.export.navigation.ExportKey
import no.nordicsemi.android.nrfmesh.feature.groups.group.controls.navigation.GroupControlsKey
import no.nordicsemi.android.nrfmesh.feature.groups.group.navigation.GroupKey
import no.nordicsemi.android.nrfmesh.feature.ivindex.navigation.IvIndexContentKey
import no.nordicsemi.android.nrfmesh.feature.model.dfu.navigation.FirmwareInformationKey
import no.nordicsemi.android.nrfmesh.feature.model.navigation.ModelKey
import no.nordicsemi.android.nrfmesh.feature.network.keys.key.navigation.NetworkKeyContentKey
import no.nordicsemi.android.nrfmesh.feature.network.keys.navigation.NetworkKeysContentKey
import no.nordicsemi.android.nrfmesh.feature.nodes.node.element.navigation.ElementKey
import no.nordicsemi.android.nrfmesh.feature.provisioners.navigation.ProvisionersContentKey
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.navigation.ProvisionerContentKey
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.navigation.GroupRangesContentKey
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.navigation.SceneRangesContentKey
import no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges.navigation.UnicastRangesContentKey
import no.nordicsemi.android.nrfmesh.feature.provisioning.navigation.ProvisioningKey
import no.nordicsemi.android.nrfmesh.feature.scenes.navigation.ScenesContentKey
import no.nordicsemi.android.nrfmesh.feature.scenes.scene.navigation.SceneContentKey
import no.nordicsemi.android.nrfmesh.network.provisioner.navigation.ProvisionerSelectorKey
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal fun MeshNetwork.createKeysForAppTitles(): List<String?> = nodes
    .map { it.name } +
        nodes
            .flatMap { it.elements }
            .map { it.name } +
        nodes
            .flatMap { it.elements }
            .flatMap { it.models }
            .map { it.name } +
        groups
            .map { it.name } +
        provisioners
            .map { it.name } +
        networkKeys
            .map { it.name } +
        applicationKeys
            .map { it.name } +
        scenes
            .map { it.name }

@OptIn(ExperimentalUuidApi::class)
internal fun title(
    context: Context,
    network: MeshNetwork,
    navigationState: NavigationState,
    isCompactWidth: Boolean,
): String = when (val key = navigationState.currentKey) {
    is ProvisioningKey -> context.getString(R.string.label_add_node)
    is NodesKey -> context.getString(R.string.label_nodes)
    is NodeKey -> network.node(uuid = Uuid.parse(uuidString = key.nodeUuid))?.name
        ?: context.getString(R.string.label_unknown)

    is ConfigNetKeysKey -> if (isCompactWidth) context.getString(R.string.label_network_keys)
    else network.node(uuid = Uuid.parse(uuidString = key.uuid))?.name
        ?: context.getString(R.string.label_unknown)

    is AddNetKeysKey -> if (isCompactWidth) context.getString(R.string.label_network_keys)
    else network.node(uuid = Uuid.parse(uuidString = key.uuid))?.name
        ?: context.getString(R.string.label_unknown)

    is ConfigAppKeysKey -> if (isCompactWidth) context.getString(R.string.label_application_keys)
    else network.node(uuid = Uuid.parse(uuidString = key.uuid))?.name
        ?: context.getString(R.string.label_unknown)

    is AddAppKeysKey -> if (isCompactWidth) context.getString(R.string.label_application_keys)
    else network.node(uuid = Uuid.parse(uuidString = key.uuid))?.name
        ?: context.getString(R.string.label_unknown)

    is ElementKey -> if (isCompactWidth)
        network.element(elementAddress = key.address)?.name
            ?: context.getString(R.string.label_unknown)
    else network.node(address = key.address)?.name ?: context.getString(R.string.label_unknown)

    is ModelKey -> {
        val address = key.address
        if (isCompactWidth) {
            val node = network.node(address = address)
                ?: return context.getString(R.string.label_unknown)
            val element = node.element(address = address)
                ?: return context.getString(R.string.label_unknown)
            val modelId = element.model(key.modelId)
                ?: return context.getString(R.string.label_unknown)
            modelId.name ?: context.getString(R.string.label_unknown)
        } else network.element(elementAddress = address)?.name
            ?: context.getString(R.string.label_unknown)
    }

    is FirmwareInformationKey -> if (isCompactWidth) {
        key.model.name ?: context.getString(R.string.label_unknown)
    } else context.getString(R.string.label_unknown)

    is GroupsKey -> context.getString(R.string.label_groups)
    is GroupKey -> network.group(address = key.address)?.name
        ?: context.getString(R.string.label_unknown)

    is GroupControlsKey -> if (isCompactWidth) {
        network.group(address = key.address)?.name ?: context.getString(R.string.label_unknown)
    } else context.getString(R.string.label_groups)

    is ProxyKey -> context.getString(R.string.label_proxy)
    is ScannerKey -> context.getString(
        when (navigationState.previousKey) {
            ProxyKey -> R.string.label_proxy
            FirmwareUpdateKey -> R.string.label_proxy
            else -> R.string.label_unknown
        }
    )

    is SettingsKey,
    is ExportKey,
    is ProvisionerSelectorKey,
        -> context.getString(R.string.label_settings)

    is ProvisionersContentKey -> if (isCompactWidth) context.getString(R.string.label_provisioners)
    else context.getString(R.string.label_settings)

    is ProvisionerContentKey -> if (isCompactWidth) {
        network.provisioner(uuid = Uuid.parse(uuidString = key.uuid))?.name
            ?: context.getString(R.string.label_unknown)
    } else context.getString(R.string.label_provisioners)

    is UnicastRangesContentKey -> {
        if (isCompactWidth) {
            network.provisioner(uuid = Uuid.parse(uuidString = key.uuid))?.name
                ?: context.getString(R.string.label_unknown)
        } else context.getString(R.string.label_provisioners)
    }

    is GroupRangesContentKey -> {
        if (isCompactWidth) {
            network.provisioner(uuid = Uuid.parse(uuidString = key.uuid))?.name
                ?: context.getString(R.string.label_unknown)
        } else context.getString(R.string.label_provisioners)
    }

    is SceneRangesContentKey -> {
        if (isCompactWidth) {
            network.provisioner(uuid = Uuid.parse(uuidString = key.uuid))?.name
                ?: context.getString(R.string.label_unknown)
        } else context.getString(R.string.label_provisioners)
    }

    is NetworkKeysContentKey -> if (isCompactWidth) context.getString(R.string.label_network_keys)
    else context.getString(R.string.label_settings)

    is NetworkKeyContentKey -> if (isCompactWidth) {
        network.networkKey(index = key.keyIndex)?.name ?: context.getString(R.string.label_unknown)
    } else context.getString(R.string.label_network_keys)

    is ApplicationKeysContentKey -> if (isCompactWidth) context.getString(R.string.label_application_keys)
    else context.getString(R.string.label_settings)

    is ApplicationKeyContentKey -> if (isCompactWidth) {
        network.applicationKey(index = key.keyIndex)?.name
            ?: context.getString(R.string.label_unknown)
    } else context.getString(R.string.label_application_keys)

    is ScenesContentKey -> if (isCompactWidth) context.getString(R.string.label_scenes)
    else context.getString(R.string.label_settings)

    is SceneContentKey -> if (isCompactWidth) {
        network.scene(number = key.number)?.name ?: context.getString(R.string.label_unknown)
    } else context.getString(R.string.label_scenes)

    is IvIndexContentKey -> if (isCompactWidth)
        context.getString(R.string.label_iv_index)
    else context.getString(R.string.label_settings)

    is DeveloperSettingsContentKey -> if (isCompactWidth)
        context.getString(R.string.label_developer_settings)
    else context.getString(R.string.label_settings)

    else -> context.getString(R.string.label_unknown)
}