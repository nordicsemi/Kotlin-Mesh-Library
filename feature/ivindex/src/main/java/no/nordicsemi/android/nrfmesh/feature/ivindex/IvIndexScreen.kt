package no.nordicsemi.android.nrfmesh.feature.ivindex

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.outlined.ChangeCircle
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedTextField
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.kotlin.mesh.core.exception.IvIndexTooSmall
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import java.text.DateFormat
import java.util.Date

@Composable
fun IvIndexScreen(
    isIvIndexChangeAllowed: Boolean,
    ivIndex: IvIndex,
    onIvIndexChanged: (UInt, Boolean) -> Unit,
    onIvIndexTestModeToggled: (Boolean) -> Unit,
    testMode: Boolean,
) {
    Column(
        modifier = Modifier.verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        SectionTitle(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp),
            title = stringResource(R.string.label_iv_index)
        )
        IvIndex(
            ivIndex = ivIndex,
            isIvIndexChangeAllowed = isIvIndexChangeAllowed,
            onIvIndexChanged = onIvIndexChanged
        )
        IvUpdateActive(ivIndex = ivIndex)
        IvIndexUpdateTestMode(
            testMode = testMode,
            onIvIndexTestModeToggled = onIvIndexTestModeToggled
        )
    }
}

@Composable
private fun IvIndex(
    ivIndex: IvIndex,
    isIvIndexChangeAllowed: Boolean,
    onIvIndexChanged: (UInt, Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showIvIndexDialog by rememberSaveable { mutableStateOf(false) }
    var ivIndexValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = ivIndex.index.toString(),
                selection = TextRange(ivIndex.index.toString().length)
            )
        )
    }
    var ivIndexUpdateState by rememberSaveable { mutableStateOf(ivIndex.isIvUpdateActive) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.FormatListNumbered,
        title = stringResource(R.string.label_iv_index),
        subtitle = ivIndex.index.toString(),
        body = {
            Text(
                modifier = Modifier.padding(start = 42.dp),
                text = stringResource(R.string.label_iv_index_update_rationale)
            )
        },
        actions = {
            MeshOutlinedButton(
                text = stringResource(R.string.label_change_iv_index),
                enabled = isIvIndexChangeAllowed,
                buttonIcon = Icons.Outlined.ChangeCircle,
                onClick = { showIvIndexDialog = true }
            )
        }
    )

    if (showIvIndexDialog) {
        MeshAlertDialog(
            onDismissRequest = { showIvIndexDialog = !showIvIndexDialog },
            onConfirmClick = {
                if(ivIndexValue.text.isBlank()){
                    isError = true
                    errorMessage = context.getString(R.string.label_iv_index_empty_error)
                } else {
                    runCatching {
                        onIvIndexChanged(
                            ivIndexValue.text.toUIntOrNull() ?: 0u,
                            ivIndexUpdateState
                        )
                    }.onSuccess {
                        showIvIndexDialog = !showIvIndexDialog
                    }.onFailure {
                        isError = !isError
                        errorMessage = if (it is IvIndexTooSmall) {
                            context.getString(R.string.label_iv_index_too_small_error)
                        } else {
                            context.getString(R.string.label_unknown_error)
                        }
                    }
                }
            },
            onDismissClick = { showIvIndexDialog = !showIvIndexDialog },
            icon = Icons.Outlined.FormatListNumbered,
            iconColor = AlertDialogDefaults.iconContentColor,
            title = stringResource(R.string.label_change_iv_index),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    MeshOutlinedTextField(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        externalLeadingIcon = {
                            Icon(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .padding(end = 8.dp),
                                imageVector = Icons.Outlined.FormatListNumbered,
                                contentDescription = null
                            )
                        },
                        value = ivIndexValue,
                        onValueChanged = { ivIndexValue = it },
                        label = { Text(text = context.getString(R.string.label_iv_index)) },
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Number
                        ),
                        isError = isError,
                        supportingText = {
                            if (isError) {
                                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                    MeshSingleLineListItem(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
                        title = stringResource(R.string.label_iv_update_state),
                        trailingComposable = {
                            Switch(
                                checked = ivIndexUpdateState,
                                onCheckedChange = { isChecked ->
                                    ivIndexUpdateState = isChecked
                                }
                            )
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun IvUpdateActive(ivIndex: IvIndex) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.AutoMirrored.Filled.DirectionsRun,
        title = stringResource(R.string.label_iv_update_state),
        subtitle = ivIndex.toSubtitle()
    )
}

@Composable
private fun IvIndexUpdateTestMode(testMode: Boolean, onIvIndexTestModeToggled: (Boolean) -> Unit) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Science,
        title = stringResource(R.string.label_iv_update_test_mode),
        subtitle = if (testMode) "Active" else "Inactive",
        titleAction = {
            Switch(
                modifier = Modifier.padding(end = 16.dp),
                checked = testMode,
                onCheckedChange = { onIvIndexTestModeToggled(it) }
            )
        }
    )
}

fun IvIndex.toSubtitle(): String = buildString {
    append(
        when (isIvUpdateActive) {
            true -> "IV Update Active"
            false -> "Normal Operation"
        }
    )
    append(", ")
    append(
        DateFormat.getDateTimeInstance().format(
            Date(transitionDate.toEpochMilliseconds())
        )
    )
}