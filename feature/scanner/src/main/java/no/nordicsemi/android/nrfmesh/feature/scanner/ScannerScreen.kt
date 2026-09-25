package no.nordicsemi.android.nrfmesh.feature.scanner

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.scanner.R.drawable
import no.nordicsemi.android.common.scanner.rememberFilterState
import no.nordicsemi.android.common.scanner.view.DeviceListItem
import no.nordicsemi.android.common.scanner.view.ScannerView
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.kotlin.ble.client.android.ScanResult
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshProxyService
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.util.networkIdentity
import no.nordicsemi.kotlin.mesh.core.util.nodeIdentity
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ScannerScreen(
    networkState: MeshNetworkState,
    uuid: Uuid,
    onScanResultSelected: (ScanResult) -> Unit
) {
    when (networkState) {
        MeshNetworkState.Loading -> {}
        is MeshNetworkState.Success -> {
            ScannerContent(
                network = networkState.network,
                uuid = uuid,
                onScanResultSelected = onScanResultSelected
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ScannerContent(
    network: MeshNetwork,
    uuid: Uuid,
    onScanResultSelected: (ScanResult) -> Unit,
) {
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_proxies),
        style = MaterialTheme.typography.titleMedium,
    )
    ScannerView(
        modifier = Modifier.padding(horizontal = 16.dp),
        state = rememberFilterState(filter = { ServiceUuid(uuid = uuid) }),
        onScanningStateChanged = {},
        deviceItem = { scanResult ->
            scanResult.advertisingData.serviceData[MeshProxyService.uuid]
                ?.takeIf { it.isNotEmpty() }
                ?.run {
                    nodeIdentity()?.matches(nodes = network.nodes)
                        ?.let {
                            DeviceListItem(
                                iconPainter = rememberVectorPainter(Icons.Outlined.WavingHand),
                                title = it.name,
                                subtitle = it.primaryUnicastAddress.address.toHexString(
                                    format = HexFormat {
                                        number.prefix = "Address: 0x"
                                        upperCase = true
                                    }
                                )
                            )
                        } ?: run {
                        networkIdentity()?.matches(networkKeys = network.networkKeys)
                            ?.let { netKey ->
                                DeviceListItem(
                                    iconPainter = painterResource(drawable.ic_mesh),
                                    title = scanResult.advertisingData.name
                                        ?: scanResult.peripheral.name
                                        ?: stringResource(R.string.label_unknown_device),
                                    subtitle = netKey.name
                                )
                            }
                    }
                }
        },
        onScanResultSelected = onScanResultSelected
    )
}