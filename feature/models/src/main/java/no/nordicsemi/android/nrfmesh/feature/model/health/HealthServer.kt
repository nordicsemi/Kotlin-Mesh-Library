package no.nordicsemi.android.nrfmesh.feature.model.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.ui.view.NordicSliderDefaults
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.data.model.HealthFaults
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedHexTextField
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedTextField
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionSet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionStatus
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFault
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultClear
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultStatus
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultTest
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultTestUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodSet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodStatus
import no.nordicsemi.kotlin.mesh.core.model.Model

/** Company identifier of Nordic Semiconductor, used when the node does not report one. */
internal const val NORDIC_COMPANY_ID: UShort = 0x0059u

/**
 * Controls of the Health Server model. Like in the iOS app, the Attention Timer, the Fast Period
 * Divisor and the faults are read when the screen is opened, one after the other. The refresh
 * button reads them again.
 *
 * @param onReadingAll Called with true while all states are being read, so that the controls of the
 *                     other sections of the screen stay disabled until the whole batch is done.
 */
@Composable
internal fun HealthServer(
    model: Model,
    messageState: MessageState,
    healthFaults: HealthFaults?,
    onReadingAll: (Boolean) -> Unit,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val hasBoundKey = model.boundApplicationKeys.isNotEmpty()
    val defaultCompanyId = remember(model) {
        companyIdText(model.parentElement?.parentNode?.companyIdentifier ?: NORDIC_COMPANY_ID)
    }
    var attention by rememberSaveable { mutableStateOf<UByte?>(null) }
    var period by rememberSaveable { mutableStateOf<UByte?>(null) }
    var testId by rememberSaveable { mutableStateOf<UByte?>(null) }
    var faultCodes by rememberSaveable { mutableStateOf<List<Int>?>(null) }
    var faultsCompanyId by remember { mutableStateOf(TextFieldValue(text = defaultCompanyId)) }
    // Index of the state being read while all states are being read, -1 otherwise.
    var readingIndex by rememberSaveable { mutableIntStateOf(if (hasBoundKey) 0 else -1) }
    val isReadingAll = readingIndex >= 0
    val enabled = hasBoundKey && !isReadingAll && !messageState.isInProgress()

    // Faults published by the node, or received while the screen was not shown.
    LaunchedEffect(key1 = healthFaults) {
        healthFaults?.let {
            testId = it.testId
            faultCodes = it.faults.map { fault -> fault.code.toInt() }
        }
    }
    LaunchedEffect(key1 = isReadingAll) { onReadingAll(isReadingAll) }
    DisposableEffect(key1 = Unit) { onDispose { onReadingAll(false) } }

    LaunchedEffect(key1 = Unit) {
        if (readingIndex == 0) {
            sendApplicationMessage(
                model,
                healthReadMessages(parseCompanyId(faultsCompanyId.text)).first()
            )
        }
    }

    LaunchedEffect(key1 = messageState) {
        when (messageState) {
            is Completed -> {
                val response = messageState.response
                when (response) {
                    is HealthAttentionStatus -> attention = response.attentionTimer
                    is HealthPeriodStatus -> period = response.fastPeriodDivisor
                    is HealthFaultStatus -> {
                        testId = response.testId
                        faultCodes = response.faults.map { it.code.toInt() }
                    }
                    else -> {}
                }
                val message = messageState.message
                val steps = healthReadMessages(parseCompanyId(faultsCompanyId.text))
                when {
                    // Unacknowledged sets do not get a response, so the value is read back.
                    message is HealthAttentionSetUnacknowledged ->
                        sendApplicationMessage(model, HealthAttentionGet())

                    message is HealthPeriodSetUnacknowledged ->
                        sendApplicationMessage(model, HealthPeriodGet())

                    readingIndex in steps.indices && response != null &&
                            response.opCode == healthResponseOpCodes[readingIndex] -> {
                        readingIndex++
                        if (readingIndex in steps.indices) {
                            sendApplicationMessage(model, steps[readingIndex])
                        } else {
                            readingIndex = -1
                        }
                    }
                }
            }

            is Failed -> readingIndex = -1
            else -> {}
        }
    }

    val unknown = stringResource(R.string.label_unknown)
    val inProgress = messageState.isInProgress()
    val sending = messageState.message
    var attentionValue by rememberSaveable { mutableFloatStateOf(0f) }
    var attentionAcknowledged by rememberSaveable { mutableStateOf(true) }
    var periodValue by rememberSaveable { mutableFloatStateOf(0f) }
    var periodAcknowledged by rememberSaveable { mutableStateOf(true) }
    var testIdText by remember { mutableStateOf(TextFieldValue(text = "0")) }
    var testCompanyId by remember { mutableStateOf(TextFieldValue(text = defaultCompanyId)) }
    var testAcknowledged by rememberSaveable { mutableStateOf(true) }
    val parsedTestId = parseTestId(testIdText.text)
    val parsedTestCompanyId = parseCompanyId(testCompanyId.text)
    val parsedFaultsCompanyId = parseCompanyId(faultsCompanyId.text)

    // Attention Timer
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_attention_timer)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.label_attention_timer),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = attentionValue,
                    onValueChange = { attentionValue = it },
                    valueRange = 0f..255f,
                    steps = 254,
                    enabled = enabled,
                    colors = NordicSliderDefaults.colors()
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_seconds_value, attentionValue.toInt()),
                    textAlign = TextAlign.End
                )
                AcknowledgedItem(
                    enabled = enabled,
                    acknowledged = attentionAcknowledged,
                    onAcknowledgedChange = { attentionAcknowledged = it }
                )
            }
        },
        actions = {
            OutlinedButton(
                onClick = {
                    sendApplicationMessage(
                        model,
                        attentionSetOf(attentionValue.toInt(), attentionAcknowledged)
                    )
                },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_set)) }
            )
        }
    )
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_attention_timer_status)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.label_current),
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = attention?.let { stringResource(R.string.label_seconds_value, it.toInt()) }
                    ?: unknown
            )
        },
        actions = {
            OutlinedButton(
                onClick = { sendApplicationMessage(model, HealthAttentionGet()) },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_get)) }
            )
        }
    )

    // Fast Period Divisor
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_fast_period_divisor)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Speed,
        title = stringResource(R.string.label_fast_period_divisor),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = periodValue,
                    onValueChange = { periodValue = it },
                    valueRange = 0f..15f,
                    steps = 14,
                    enabled = enabled,
                    colors = NordicSliderDefaults.colors()
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = fastPeriodLabel(periodValue.toInt().toUByte()),
                    textAlign = TextAlign.End
                )
                AcknowledgedItem(
                    enabled = enabled,
                    acknowledged = periodAcknowledged,
                    onAcknowledgedChange = { periodAcknowledged = it }
                )
            }
        },
        actions = {
            OutlinedButton(
                onClick = {
                    sendApplicationMessage(
                        model,
                        periodSetOf(periodValue.toInt(), periodAcknowledged)
                    )
                },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_set)) }
            )
        }
    )
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_fast_period_divisor_status)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Speed,
        title = stringResource(R.string.label_current),
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = period?.let { fastPeriodLabel(it) } ?: unknown
            )
        },
        actions = {
            OutlinedButton(
                onClick = { sendApplicationMessage(model, HealthPeriodGet()) },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_get)) }
            )
        }
    )

    // Health Fault Test
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_health_fault_test)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Science,
        title = stringResource(R.string.label_health_fault_test),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                MeshOutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.label_test_id)) },
                    placeholder = { Text(text = stringResource(R.string.label_test_id_placeholder)) },
                    value = testIdText,
                    onValueChanged = { testIdText = it },
                    enabled = enabled,
                    isError = parsedTestId == null && testIdText.text.isNotEmpty(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                MeshOutlinedHexTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.label_company_id_hex)) },
                    placeholder = { Text(text = stringResource(R.string.label_company_id_placeholder)) },
                    value = testCompanyId,
                    onValueChanged = { testCompanyId = it },
                    enabled = enabled,
                    isError = parsedTestCompanyId == null && testCompanyId.text.isNotEmpty()
                )
                AcknowledgedItem(
                    enabled = enabled,
                    acknowledged = testAcknowledged,
                    onAcknowledgedChange = { testAcknowledged = it }
                )
            }
        },
        actions = {
            OutlinedButton(
                onClick = {
                    if (parsedTestId != null && parsedTestCompanyId != null) {
                        sendApplicationMessage(
                            model,
                            faultTestOf(parsedTestId, parsedTestCompanyId, testAcknowledged)
                        )
                    }
                },
                enabled = enabled && parsedTestId != null && parsedTestCompanyId != null,
                content = { Text(text = stringResource(R.string.label_test)) }
            )
        }
    )

    // Fault Status
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_fault_status)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.BugReport,
        title = stringResource(R.string.label_faults),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                MeshOutlinedHexTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(text = stringResource(R.string.label_company_id_hex)) },
                    placeholder = { Text(text = stringResource(R.string.label_company_id_placeholder)) },
                    value = faultsCompanyId,
                    onValueChanged = { faultsCompanyId = it },
                    enabled = enabled,
                    isError = parsedFaultsCompanyId == null && faultsCompanyId.text.isNotEmpty()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.label_most_recent_test_id))
                    Text(text = testId?.toString() ?: unknown)
                }
            }
        },
        actions = {
            OutlinedButton(
                onClick = {
                    parsedFaultsCompanyId?.let {
                        sendApplicationMessage(model, HealthFaultClear(companyIdentifier = it))
                    }
                },
                enabled = enabled && parsedFaultsCompanyId != null,
                content = { Text(text = stringResource(R.string.label_clear_faults)) }
            )
            OutlinedButton(
                onClick = {
                    parsedFaultsCompanyId?.let {
                        sendApplicationMessage(model, HealthFaultGet(companyIdentifier = it))
                    }
                },
                enabled = enabled && parsedFaultsCompanyId != null,
                content = { Text(text = stringResource(R.string.label_get)) }
            )
        }
    )
    faultCodes.orEmpty().map { HealthFault.from(it.toUByte()) }.forEach { fault ->
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.Warning,
            title = when (fault) {
                is HealthFault.Vendor ->
                    stringResource(R.string.label_fault_vendor, fault.code.toInt())

                is HealthFault.Reserved ->
                    stringResource(R.string.label_fault_reserved, fault.code.toInt())

                else -> fault.label()
            }
        )
    }
}

@Composable
private fun AcknowledgedItem(
    enabled: Boolean,
    acknowledged: Boolean,
    onAcknowledgedChange: (Boolean) -> Unit,
) {
    MeshSingleLineListItem(
        modifier = Modifier.fillMaxSize(),
        imageVector = Icons.Outlined.Verified,
        title = stringResource(R.string.label_acknowledged),
        trailingComposable = {
            Switch(
                enabled = enabled,
                checked = acknowledged,
                onCheckedChange = onAcknowledgedChange,
            )
        }
    )
}

/** Response op codes of the messages returned by [healthReadMessages], in the same order. */
internal val healthResponseOpCodes = listOf(
    HealthAttentionStatus.opCode,
    HealthPeriodStatus.opCode,
    HealthFaultStatus.opCode
)

/**
 * The messages sent one after the other when the states of the Health Server are read. The faults
 * are read only when a valid [companyId] is given.
 */
internal fun healthReadMessages(companyId: UShort?): List<MeshMessage> = listOfNotNull(
    HealthAttentionGet(),
    HealthPeriodGet(),
    companyId?.let { HealthFaultGet(companyIdentifier = it) }
)

/** Creates the message used to set the Attention Timer. */
internal fun attentionSetOf(seconds: Int, acknowledged: Boolean): MeshMessage {
    val timer = seconds.coerceIn(0, 255).toUByte()
    return if (acknowledged) {
        HealthAttentionSet(attentionTimer = timer)
    } else {
        HealthAttentionSetUnacknowledged(attentionTimer = timer)
    }
}

/** Creates the message used to set the Fast Period Divisor. */
internal fun periodSetOf(divisor: Int, acknowledged: Boolean): MeshMessage {
    val value = divisor.coerceIn(0, 15).toUByte()
    return if (acknowledged) {
        HealthPeriodSet(fastPeriodDivisor = value)
    } else {
        HealthPeriodSetUnacknowledged(fastPeriodDivisor = value)
    }
}

/** Creates the message used to start a test. */
internal fun faultTestOf(testId: UByte, companyId: UShort, acknowledged: Boolean): MeshMessage =
    if (acknowledged) {
        HealthFaultTest(testId = testId, companyIdentifier = companyId)
    } else {
        HealthFaultTestUnacknowledged(testId = testId, companyIdentifier = companyId)
    }

/** The Fast Period Divisor shown as the power of two it stands for. */
internal fun fastPeriodLabel(divisor: UByte): String = (1 shl divisor.toInt().coerceAtMost(15)).toString()

/** Parses a 16 bit hexadecimal company identifier, e.g. 0059. */
internal fun parseCompanyId(text: String): UShort? = text.trim()
    .takeIf { it.length in 1..4 }
    ?.toUShortOrNull(radix = 16)

/** Formats a company identifier as four hexadecimal digits. */
internal fun companyIdText(companyId: UShort): String =
    companyId.toString(radix = 16).uppercase().padStart(4, '0')

/** Parses a test identifier, which is a value between 0 and 255. */
internal fun parseTestId(text: String): UByte? = text.trim().toUByteOrNull()

private val camelCaseBoundary = Regex("(?<=[a-z])(?=[A-Z])")

/** A readable name of a specified fault, e.g. "Battery Low Warning". */
internal fun HealthFault.label(): String = (this::class.simpleName ?: "")
    .split(camelCaseBoundary)
    .joinToString(separator = " ")
