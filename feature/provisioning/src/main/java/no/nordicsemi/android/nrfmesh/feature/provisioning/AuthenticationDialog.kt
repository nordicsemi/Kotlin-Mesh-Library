package no.nordicsemi.android.nrfmesh.feature.provisioning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedTextField
import no.nordicsemi.kotlin.mesh.provisioning.AuthAction

@Composable
fun AuthenticationDialog(
    action: AuthAction,
    onOkClicked: (AuthAction, String) -> Unit,
    onCancelClicked: () -> Unit,
) {
    var showAuthDialog by rememberSaveable { mutableStateOf(true) }
    var authValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = ""))
    }
    if (showAuthDialog) {
        MeshAlertDialog(
            onDismissRequest = {
                showAuthDialog = !showAuthDialog
                onCancelClicked()
            },
            title = stringResource(R.string.label_authentication_required),
            icon = Icons.Outlined.Pin,
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    AuthRow(
                        action = action,
                        authValue = authValue,
                        onValueChanged = { authValue = it }
                    )
                    Spacer(modifier = Modifier.size(size = 16.dp))
                    when {
                        action !is AuthAction.DisplayNumber &&
                                action !is AuthAction.DisplayAlphaNumeric -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 100.dp),
                                    onClick = {
                                        showAuthDialog = !showAuthDialog
                                        onCancelClicked()
                                    },
                                    content = {
                                        Text(text = stringResource(id = R.string.label_cancel))
                                    }
                                )
                                Spacer(modifier = Modifier.size(size = 8.dp))
                                Button(
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 100.dp),
                                    enabled = shouldEnable(action, authValue.text),
                                    onClick = { onOkClicked(action, authValue.text.trim()) },
                                    content = { Text(text = stringResource(id = R.string.label_ok)) }
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}

private fun shouldEnable(action: AuthAction, authValue: String): Boolean {
    return when (action) {
        is AuthAction.DisplayAlphaNumeric,
        is AuthAction.DisplayNumber,
            -> true

        is AuthAction.ProvideAlphaNumeric -> authValue.length <= action.maxNumberOfCharacters.toInt()
        is AuthAction.ProvideNumeric -> authValue.length <= action.maxNumberOfDigits.toInt()
        is AuthAction.ProvideStaticKey ->
            when (action.length) {
                16 -> authValue.length == 32
                32 -> authValue.length == 64
                else -> false
            }
    }
}

@Composable
private fun AuthRow(
    action: AuthAction,
    authValue: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
) {
    when (action) {
        is AuthAction.DisplayAlphaNumeric -> DisplayAlphaNumeric(action = action)

        is AuthAction.DisplayNumber -> DisplayNumber(action = action)

        is AuthAction.ProvideAlphaNumeric -> ProvideAlphaNumeric(
            authValue = authValue,
            length = action.maxNumberOfCharacters.toInt(),
            onValueChanged = onValueChanged
        )

        is AuthAction.ProvideNumeric -> ProvideNumeric(
            authValue = authValue,
            length = action.maxNumberOfDigits.toInt(),
            onValueChanged = onValueChanged
        )

        is AuthAction.ProvideStaticKey -> ProvideStaticKey(
            length = action.length,
            authValue = authValue,
            onValueChanged = onValueChanged
        )
    }
}

@Composable
private fun DisplayAlphaNumeric(
    action: AuthAction.DisplayAlphaNumeric,
) {
    val message = stringResource(id = R.string.label_display_alpha_numeric_rationale, action.text)
    val start = message.indexOf(action.text)
    val end = start + action.text.length
    val input = AnnotatedString.Builder(message)
        .apply {
            addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = start,
                end = end
            )
        }.toAnnotatedString()
    Text(text = input, style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.size(size = 16.dp))
}

@Composable
private fun DisplayNumber(
    action: AuthAction.DisplayNumber,
) {
    val number = action.number.toString()
    val message = stringResource(id = R.string.label_display_alpha_numeric_rationale, number)
    val start = message.indexOf(number)
    val end = start + number.length
    val input = AnnotatedString.Builder(message)
        .apply {
            addStyle(
                style = SpanStyle(fontWeight = FontWeight.Bold),
                start = start,
                end = end
            )
        }.toAnnotatedString()
    Text(text = input, style = MaterialTheme.typography.bodyMedium)
    Spacer(modifier = Modifier.size(size = 16.dp))
}

@Composable
private fun ProvideAlphaNumeric(
    length: Int,
    authValue: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
) {
    Text(
        text = stringResource(id = R.string.label_provide_alpha_numeric_rationale),
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.size(size = 16.dp))
    MeshOutlinedTextField(
        modifier = Modifier.padding(vertical = 8.dp),
        externalLeadingIcon = {
            Icon(
                modifier = Modifier.padding(horizontal = 16.dp),
                imageVector = Icons.Outlined.Password,
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.6f)
            )
        },
        value = authValue,
        onValueChanged = {
            if (it.text.length <= length)
                onValueChanged(it)
        },
        label = { TextFieldValue(text = stringResource(R.string.label_auth_value)) },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false
        ),
        placeholder = { TextFieldValue(text = stringResource(R.string.label_e_g_12e4s6)) },
        supportingText = {
            Text(
                text = "${authValue.text.length} / $length",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    )
}

@Composable
private fun ProvideNumeric(
    length: Int,
    authValue: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
) {
    Text(
        text = stringResource(id = R.string.label_provide_numeric_rationale),
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.size(size = 16.dp))
    MeshOutlinedTextField(
        modifier = Modifier.padding(vertical = 8.dp),
        externalLeadingIcon = {
            Icon(
                modifier = Modifier.padding(end = 16.dp),
                imageVector = Icons.Outlined.Password,
                contentDescription = null,
                tint = LocalContentColor.current.copy(alpha = 0.6f)
            )
        },
        value = authValue,
        onValueChanged = {
            if (it.text.length <= length)
                onValueChanged(it)
        },
        label = { Text(text = stringResource(R.string.label_auth_value)) },
        keyboardOptions = KeyboardOptions(
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Number
        ),
        placeholder = { Text(text = stringResource(R.string.label_e_g_123456)) },
        supportingText = {
            Text(
                text = "${authValue.text.length} / $length",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    )
}

@Composable
private fun ProvideStaticKey(
    length: Int,
    authValue: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
) {
    Text(
        text = stringResource(id = R.string.label_provide_static_key_rationale, length * 2),
        style = MaterialTheme.typography.bodyMedium
    )
    Spacer(modifier = Modifier.size(size = 16.dp))
    MeshOutlinedTextField(
        modifier = Modifier.padding(vertical = 8.dp),
        value = authValue,
        onValueChanged = {
            when (length) {
                16 -> if (it.text.length <= 32) onValueChanged(it)
                else -> if (it.text.length <= 64) onValueChanged(it)
            }
        },
        label = { TextFieldValue(text = stringResource(R.string.label_auth_value)) },
        regex = when (length) {
            16 -> Regex("[0-9A-Fa-f]{0,32}")
            else -> Regex("[0-9A-Fa-f]{0,64}")
        },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            autoCorrectEnabled = false,
        ),
        placeholder = { TextFieldValue(text = stringResource(R.string.label_empty)) },
        supportingText = {
            Text(
                text = "${authValue.text.length} / ${length * 2}",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        }
    )
}