package no.nordicsemi.android.nrfmesh.feature.developer

import no.nordicsemi.android.nrfmesh.core.ui.BottomInsetSpacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BuildCircle
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.data.DeveloperSettings
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle

@Composable
internal fun DeveloperSettingsScreen(
    developerSettings: DeveloperSettings,
    onQuickProvisioningEnabled: (Boolean) -> Unit,
    onAlwaysReconfigure: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        SectionTitle(
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp),
            title = stringResource(id = R.string.label_developer_settings)
        )
        QuickProvisioning(
            quickProvisioning = developerSettings.quickProvisioning,
            onQuickProvisioningEnabled = onQuickProvisioningEnabled
        )
        AlwaysReconfigure(
            alwaysReconfigure = developerSettings.alwaysReconfigure,
            onAlwaysReconfigure = onAlwaysReconfigure
        )
        BottomInsetSpacer(spacing = 8.dp)
    }
}

@Composable
private fun QuickProvisioning(
    quickProvisioning: Boolean,
    onQuickProvisioningEnabled: (Boolean) -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.ElectricBolt,
        title = stringResource(R.string.label_quick_provisioning),
        titleAction = {
            Switch(
                modifier = Modifier.padding(horizontal = 16.dp),
                checked = quickProvisioning,
                onCheckedChange = onQuickProvisioningEnabled
            )
        },
        supportingText = stringResource(R.string.label_quick_provisioning_rationale)
    )
}


@Composable
private fun AlwaysReconfigure(
    alwaysReconfigure: Boolean,
    onAlwaysReconfigure: (Boolean) -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.BuildCircle,
        title = stringResource(R.string.label_always_reconfigure),
        titleAction = {
            Switch(
                modifier = Modifier.padding(horizontal = 16.dp),
                checked = alwaysReconfigure,
                onCheckedChange = onAlwaysReconfigure
            )
        },
        supportingText = stringResource(R.string.label_always_reconfigure_rationale)
    )
}
