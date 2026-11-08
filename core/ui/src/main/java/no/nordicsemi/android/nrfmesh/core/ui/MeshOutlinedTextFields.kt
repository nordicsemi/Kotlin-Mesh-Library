package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun MeshOutlinedTextField(
    modifier: Modifier = Modifier,
    onFocus: Boolean = false,
    externalLeadingIcon: @Composable () -> Unit = {},
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    value: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
    internalTrailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    regex: Regex? = null,
    isError: Boolean = regex != null && !regex.matches(value.text),
    supportingText: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    val requester = remember { FocusRequester() }
    Row(
        modifier = modifier.height(height = 80.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        externalLeadingIcon()
        OutlinedTextField(
            modifier = Modifier
                .weight(1f)
                .focusRequester(requester),
            enabled = enabled,
            prefix = prefix,
            value = value,
            onValueChange = { value ->
                regex?.let {
                    if(it.matches(value.text)) {
                        onValueChanged(value)
                    }
                } ?: run {
                    onValueChanged(value)
                }
            },
            label = label,
            placeholder = placeholder,
            trailingIcon = internalTrailingIcon,
            readOnly = readOnly,
            isError = isError,
            supportingText = supportingText,
            singleLine = true,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            maxLines = 1
        )
        content()
    }
    LaunchedEffect(onFocus) {
        if (onFocus) requester.requestFocus()
    }
}

@Composable
fun MeshOutlinedHexTextField(
    modifier: Modifier = Modifier,
    showPrefix: Boolean = true,
    enabled: Boolean = true,
    onFocus: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    value: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
    internalTrailingIcon: @Composable (() -> Unit)? = null,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Characters,
        autoCorrectEnabled = false,
        keyboardType = KeyboardType.Text
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    regex: Regex? = null,
    isError: Boolean = regex != null && !regex.matches(value.text),
    supportingText: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    MeshOutlinedTextField(
        modifier = modifier,
        enabled = enabled,
        onFocus = onFocus,
        label = label,
        placeholder = placeholder,
        prefix = {
            if (showPrefix)
                Text(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    text = stringResource(R.string.label_hex_prefix)
                )
        },
        value = value,
        onValueChanged = onValueChanged,
        internalTrailingIcon = internalTrailingIcon,
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        regex = regex,
        isError = isError,
        supportingText = supportingText,
        content = content
    )
}