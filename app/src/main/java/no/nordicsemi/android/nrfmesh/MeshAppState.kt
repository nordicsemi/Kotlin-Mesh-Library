package no.nordicsemi.android.nrfmesh

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import no.nordicsemi.android.feature.config.networkkeys.navigation.AddNetKeysKey
import no.nordicsemi.android.feature.config.networkkeys.navigation.ConfigNetKeysKey
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.NavigationState
import no.nordicsemi.android.nrfmesh.core.navigation.NodeKey
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.android.nrfmesh.feature.application.keys.key.navigation.ApplicationKeyContentKey
import no.nordicsemi.android.nrfmesh.feature.application.keys.navigation.ApplicationKeysContentKey
import no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.navigation.AddAppKeysKey
import no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.navigation.ConfigAppKeysKey
import no.nordicsemi.android.nrfmesh.feature.developer.navigation.DeveloperSettingsContentKey
import no.nordicsemi.android.nrfmesh.feature.groups.group.controls.navigation.GroupControlsKey
import no.nordicsemi.android.nrfmesh.feature.groups.group.navigation.GroupKey
import no.nordicsemi.android.nrfmesh.feature.ivindex.navigation.IvIndexContentKey
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

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberMeshAppState(
    snackbarHostState: SnackbarHostState,
    navigationState: NavigationState,
): MeshAppState = remember(navigationState) {
    MeshAppState(
        navigationState = navigationState,
        snackbarHostState = snackbarHostState
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Stable
class MeshAppState(
    navigationState: NavigationState,
    snackbarHostState: SnackbarHostState,
) : AppState(
    navigationState = navigationState,
    snackbarHostState = snackbarHostState
) {
    val showBackButton: Boolean
        @Composable get() = when (navigationState.currentKey) {
            is ProvisionersContentKey,
            is NetworkKeysContentKey,
            is ApplicationKeysContentKey,
            is ScenesContentKey,
            is IvIndexContentKey,
            is DeveloperSettingsContentKey,
                -> isCompactWidth

            is ProvisionerContentKey,
            is UnicastRangesContentKey,
            is GroupRangesContentKey,
            is SceneRangesContentKey,
            is NetworkKeyContentKey,
            is ApplicationKeyContentKey,
            is SceneContentKey,
            is GroupKey,
            is GroupControlsKey,
            is NodeKey,
            is ConfigNetKeysKey,
            is AddNetKeysKey,
            is ConfigAppKeysKey,
            is AddAppKeysKey,
            is ElementKey,
            is ModelKey,
            is ProvisioningKey,
                -> true

            else -> false
        }

    val isCompactWidth: Boolean
        @Composable
        get() = isCompactWidth()
}