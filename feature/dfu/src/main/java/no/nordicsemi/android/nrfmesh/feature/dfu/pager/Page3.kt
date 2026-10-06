package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.DisabledByDefault
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.NoTransfer
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.SafetyCheck
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TransferWithinAStation
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.ui.view.NordicSliderDefaults
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItemTextField
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.android.nrfmesh.feature.dfu.util.DfuParameters
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePolicy
import no.nordicsemi.kotlin.mesh.core.messages.TransferMode
import no.nordicsemi.kotlin.mesh.core.model.DistributionMulticastAddress
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun Page3() {
    val viewModel = hiltViewModel<Page3ViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FirmwareDistributionParameters(
        dfuParameters = uiState.dfuParameters,
        onTtlChanged = viewModel::onTtlChanged,
        onTimeoutBaseChanged = viewModel::onTimeoutChanged,
        onTransferModeChanged = viewModel::onTransferModeChanged,
        onUpdatePolicyChanged = viewModel::onUpdatePolicyChanged
    )
    MulticastDistribution(
        address = uiState.dfuParameters.address,
        group = uiState.groups,
        onMulticastAddressChanged = viewModel::onMulticastAddressChanged
    )
}

@Composable
private fun FirmwareDistributionParameters(
    dfuParameters: DfuParameters,
    onTtlChanged: (UByte) -> Unit,
    onTimeoutBaseChanged: (UShort) -> Unit,
    onTransferModeChanged: (TransferMode) -> Unit,
    onUpdatePolicyChanged: (FirmwareUpdatePolicy) -> Unit,
) {
    SectionTitle(
        modifier = Modifier.padding(top = 8.dp),
        title = stringResource(id = R.string.label_distribution_parameters)
    )
    TtlRow(ttl = dfuParameters.ttl, onTtlChanged = onTtlChanged)
    TimeoutSlider(
        timeout = dfuParameters.timeoutBase,
        ttl = dfuParameters.ttl,
        onTimeoutBaseChanged = onTimeoutBaseChanged
    )
    TransferMode(mode = dfuParameters.mode, onTransferModeChanged = onTransferModeChanged)
    Policy(policy = dfuParameters.policy, onUpdatePolicyChanged = onUpdatePolicyChanged)
}

@Composable
private fun TtlRow(ttl: UByte, onTtlChanged: (UByte) -> Unit) {
    ElevatedCardItemTextField(
        modifier = Modifier.padding(top = 8.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(id = R.string.label_ttl),
        subtitle = "$ttl",
        onValueChanged = { onTtlChanged(it.toUByte()) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        ),
        regex = Regex(pattern = "^(?:[1-9]|[1-9]\\d|1[01]\\d|12[0-7])?$")
    )
}

/**
 * A slider which selects a timeout base from a fixed set of values and shows the resulting timeout.
 *
 * timeout = 10 s * (timeoutBase + 2) + 100 ms * ttl
 *
 * @param timeout         The selected timeout base.
 * @param ttl                 The TTL, which adds 100 ms per hop to the timeout.
 * @param onTimeoutBaseChanged Called when the user selects another timeout base.
 */
@Composable
fun TimeoutSlider(
    timeout: UShort,
    ttl: UByte,
    onTimeoutBaseChanged: (UShort) -> Unit,
) {
    val allowedValues: List<UShort> = listOf(
        0u, 1u, 2u, 3u, 4u, 10u, 16u, 22u, 28u, 58u, 118u, 178u, 238u, 298u, 358u,
        718u, 1078u, 1438u, 1798u, 8638u, 65535u,
    ).map { it.toUShort() }

    val index = allowedValues.indexOf(timeout).coerceAtLeast(0)
    val timeoutValue = rememberSaveable (inputs = arrayOf(timeout, ttl)) {
        formatTimeout(timeoutBase = timeout, ttl = ttl)
    }

    ElevatedCardItem(
        modifier = Modifier.padding(top = 8.dp),
        imageVector = Icons.Outlined.AccessTime,
        title = stringResource(id = R.string.label_timeout),
        subtitle = "Timeout base: $timeout",
        supportingText = stringResource(R.string.label_client_timeout_rationale),
        body = {
            Slider(
                modifier = Modifier.padding(start = 42.dp),
                value = index.toFloat(),
                onValueChange = {
                    val selected = allowedValues[it.roundToInt()]
                    if (selected != timeout) onTimeoutBaseChanged(selected)
                },
                valueRange = 0f..allowedValues.lastIndex.toFloat(),
                // Discrete positions: one tick per allowed value.
                steps = allowedValues.size - 2,
                colors = NordicSliderDefaults.colors()
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .sizeIn(minWidth = 80.dp),
                text = timeoutValue,
                textAlign = TextAlign.End
            )
        }
    )
}

private fun formatTimeout(timeoutBase: UShort, ttl: UByte): String {
    val timeout = (10L * (timeoutBase.toLong() + 2)).seconds + (100L * ttl.toLong()).milliseconds
    return timeout.toComponents { days, hours, minutes, seconds, nanoseconds ->
        val ms = nanoseconds / 1_000_000
        buildList {
            if (days > 0) add("$days d")
            if (hours > 0) add("$hours hrs")
            if (minutes > 0) add("$minutes min")
            if (seconds > 0) add("$seconds sec")
        }.joinToString(", ")
    }
}

@Composable
private fun TransferMode(mode: TransferMode, onTransferModeChanged: (TransferMode) -> Unit) {
    val modes = listOf(TransferMode.PUSH, TransferMode.PULL)
    ElevatedCardItem(
        modifier = Modifier.padding(top = 8.dp),
        imageVector = Icons.Outlined.TransferWithinAStation,
        title = stringResource(id = R.string.label_mode),
        titleAction = {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(end = 16.dp)) {
                modes.forEachIndexed { index, transferMode ->
                    if (transferMode != TransferMode.NO_ACTIVE_TRANSFER) {
                        SegmentedButton(
                            modifier = Modifier.width(100.dp),
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = modes.size
                            ),
                            onClick = { onTransferModeChanged(transferMode) },
                            selected = transferMode == mode,
                            icon = {
                                SegmentedButtonDefaults.Icon(active = transferMode == mode) {
                                    Icon(
                                        imageVector = transferMode.icon(),
                                        contentDescription = null,
                                        modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = transferMode.debugDescription,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun Policy(
    policy: FirmwareUpdatePolicy,
    onUpdatePolicyChanged: (FirmwareUpdatePolicy) -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(top = 8.dp),
        imageVector = Icons.Outlined.Policy,
        title = stringResource(id = R.string.label_policy),
        actions = {
            SingleChoiceSegmentedButtonRow {
                FirmwareUpdatePolicy.entries.forEachIndexed { index, updatePolicy ->
                    SegmentedButton(
                        modifier = Modifier.width(135.dp),
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = FirmwareUpdatePolicy.entries.size
                        ),
                        onClick = { onUpdatePolicyChanged(updatePolicy) },
                        selected = updatePolicy == policy,
                        icon = {
                            SegmentedButtonDefaults.Icon(active = updatePolicy == policy) {
                                Icon(
                                    imageVector = updatePolicy.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = updatePolicy.description(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun MulticastDistribution(
    address: DistributionMulticastAddress,
    group: List<Group>,
    onMulticastAddressChanged: (DistributionMulticastAddress) -> Unit,
) {
    SectionTitle(
        modifier = Modifier.padding(top = 8.dp),
        title = stringResource(id = R.string.label_distribution_multicast)
    )
    ElevatedCardItem(
        modifier = Modifier
            .padding(top = 8.dp),
        imageVector = Icons.Outlined.DisabledByDefault,
        title = stringResource(R.string.label_disabled),
        subtitle = stringResource(R.string.label_disabled_multicast_rationale),
        titleAction = {
            RadioButton(
                selected = address is UnassignedAddress,
                onClick = { onMulticastAddressChanged(UnassignedAddress) }
            )
        }
    )
    group.forEach {
        ElevatedCardItem(
            modifier = Modifier
                .padding(top = 8.dp),
            imageVector = Icons.Outlined.GroupWork,
            title = it.name,
            titleAction = {
                RadioButton(
                    selected = it.address == address,
                    onClick = { onMulticastAddressChanged(it.address as DistributionMulticastAddress) }
                )
            }
        )
    }
    Text(
        modifier = Modifier.padding(horizontal = 8.dp).padding(top = 8.dp),
        text = stringResource(R.string.label_multicast_distribution_rationale),
        style = MaterialTheme.typography.bodySmall
    )
}

private fun TransferMode.icon() = when (this) {
    TransferMode.PUSH -> Icons.Outlined.Upload
    TransferMode.PULL -> Icons.Outlined.Download
    else -> Icons.Outlined.NoTransfer
}

private fun FirmwareUpdatePolicy.icon() = when (this) {
    FirmwareUpdatePolicy.VERIFY_AND_APPLY -> Icons.Outlined.SafetyCheck
    FirmwareUpdatePolicy.VERIFY_ONLY -> Icons.Outlined.Verified
}

private fun FirmwareUpdatePolicy.description() = when (this) {
    FirmwareUpdatePolicy.VERIFY_AND_APPLY -> "Verify & Apply"
    FirmwareUpdatePolicy.VERIFY_ONLY -> "Verify"
}