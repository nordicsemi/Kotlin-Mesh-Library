package no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges

import no.nordicsemi.android.nrfmesh.core.ui.bottomInsetPadding
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.ui.AddressRangeLegendsForRanges
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedHexTextField
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.provisioners.R
import no.nordicsemi.kotlin.mesh.core.model.GroupRange
import no.nordicsemi.kotlin.mesh.core.model.Range
import no.nordicsemi.kotlin.mesh.core.model.SceneRange
import no.nordicsemi.kotlin.mesh.core.model.UnicastRange

@Composable
fun RangesScreen(
    title: String,
    ranges: List<Range>,
    otherRanges: List<Range>,
    overlaps: Boolean,
    isValidBound: (UShort) -> Boolean,
    addRange: (start: UShort, end: UShort) -> Unit,
    onRangeUpdated: (UShort, UShort) -> Unit,
    onSwiped: (Range) -> Unit,
    resolve: () -> Unit,
    save: () -> Unit,
) {
    var showAddRangeDialog by remember { mutableStateOf(false) }
    var rangeToEdit by remember { mutableStateOf<Range?>(null) }
    Column {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = title
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                MeshOutlinedButton(
                    buttonIcon = Icons.Outlined.Add,
                    text = stringResource(R.string.label_add_range),
                    onClick = {
                        rangeToEdit = null
                        showAddRangeDialog = true
                    }
                )
                AnimatedVisibility(visible = overlaps) {
                    MeshOutlinedButton(
                        border = BorderStroke(width = 1.dp, color = Color.Red),
                        buttonIcon = Icons.Outlined.AutoFixHigh,
                        buttonIconTint = Color.Red,
                        text = stringResource(R.string.label_resolve),
                        textColor = Color.Red,
                        onClick = resolve
                    )
                }
                MeshOutlinedButton(
                    enabled = !overlaps,
                    buttonIcon = Icons.Outlined.Gavel,
                    text = stringResource(R.string.label_allocate),
                    onClick = save
                )
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.6f, false),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = bottomInsetPadding().calculateBottomPadding()
            )
        ) {
            if (ranges.isNotEmpty()) {
                items(items = ranges, key = { "${it.low}_${it.high}_${it::class.simpleName}" }) { range ->
                    // Hold the current state from the Swipe to Dismiss composable
                    // val currentItem by rememberUpdatedState(newValue = range)
                    val dismissState = rememberSwipeToDismissBoxState()
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.Settled,
                                    SwipeToDismissBoxValue.StartToEnd,
                                    SwipeToDismissBoxValue.EndToStart,
                                        -> Color.Red
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color = color, shape = CardDefaults.elevatedShape)
                                    .padding(horizontal = 16.dp),
                                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                    Alignment.CenterStart
                                else Alignment.CenterEnd
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "null"
                                )
                            }
                        },
                        onDismiss = { onSwiped(range) },
                        content = {
                            OutlinedCard {
                                AllocatedRange(
                                    imageVector = range.toImageVector(),
                                    title = "${
                                        range.low.toHexString(
                                            format = HexFormat {
                                                number.prefix = "0x"
                                                upperCase = true
                                            }
                                        )
                                    } - ${
                                        range.high.toHexString(
                                            format = HexFormat {
                                                number.prefix = "0x"
                                                upperCase = true
                                            }
                                        )
                                    }",
                                    range = range,
                                    otherRanges = otherRanges.filter { it.overlap(range) != null },
                                    onClick = {
                                        rangeToEdit = it
                                        showAddRangeDialog = true
                                    }
                                )
                            }
                        }
                    )
                }
            } else {
                item {
                    MeshNoItemsAvailable(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Outlined.AutoAwesome,
                        title = stringResource(R.string.no_ranges_currently_added)
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 8.dp),
            text = stringResource(R.string.label_overlapping_ranges_rationale),
            style = MaterialTheme.typography.labelMedium
        )
        AddressRangeLegendsForRanges()
        Spacer(modifier = Modifier.size(16.dp))

        if (showAddRangeDialog) {
            rangeToEdit?.let { range ->
                UpdateRangeDialog(
                    onDismissRequest = { showAddRangeDialog = false },
                    range = range,
                    isValidBound = isValidBound,
                    onConfirmClicked = onRangeUpdated
                )
            } ?: AddRangeDialog(
                isValidBound = isValidBound,
                onDismissRequest = { showAddRangeDialog = false },
                onConfirmClicked = { start, end -> addRange(start, end) }
            )
        }
    }
}

@Composable
private fun AddRangeDialog(
    isValidBound: (UShort) -> Boolean,
    onDismissRequest: () -> Unit,
    onConfirmClicked: (start: UShort, end: UShort) -> Unit,
) {
    var low by remember { mutableStateOf<UShort?>(null) }
    var high by remember { mutableStateOf<UShort?>(null) }
    var start by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    var end by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(""))
    }
    val scope = rememberCoroutineScope()
    var startInFocus by rememberSaveable { mutableStateOf(false) }
    var endInFocus by rememberSaveable { mutableStateOf(false) }
    var invalidStart by rememberSaveable { mutableStateOf(false) }
    var invalidEnd by rememberSaveable { mutableStateOf(false) }
    var supportingErrorTextStart by rememberSaveable { mutableStateOf("") }
    var supportingErrorTextEnd by rememberSaveable { mutableStateOf("") }
    MeshAlertDialog(
        onDismissRequest = { onDismissRequest() },
        onConfirmClick = {
            val startValue = start.text.trim()
            val endValue = end.text.trim()
            if (startValue.isNotBlank() && endValue.isNotBlank()) {
                scope.launch {
                    onDismissRequest()
                }.invokeOnCompletion {
                    onConfirmClicked(startValue.toUShort(radix = 16), endValue.toUShort(radix = 16))
                }
            }
        },
        onDismissClick = { onDismissRequest() },
        icon = Icons.Outlined.SwapHoriz,
        title = stringResource(R.string.title_new_range),
        error = invalidStart ||
                invalidEnd ||
                start.text.isBlank() ||
                end.text.trim().isBlank(),
        content = {
            Column {
                Text(text = stringResource(R.string.label_new_range_rationale))
                Spacer(modifier = Modifier.size(16.dp))
                MeshOutlinedHexTextField(
                    modifier = Modifier.clickable {
                        startInFocus = true
                        endInFocus = false
                    },
                    showPrefix = true,
                    onFocus = startInFocus,
                    value = start,
                    onValueChanged = {
                        startInFocus = true
                        endInFocus = false
                        start = it
                        if (start.text.trim().isNotEmpty()) {
                            runCatching {
                                invalidStart = !isValidBound(start.text.toUShort(radix = 16))
                                if (!invalidStart) {
                                    supportingErrorTextStart = ""
                                    invalidStart = false
                                }
                            }.onSuccess {
                                if (!invalidStart) {
                                    low = start.text.toUShort(radix = 16)
                                    if (high != null && low!! > high!!) {
                                        supportingErrorTextStart =
                                            "Lower bound must be <= upper bound"
                                        invalidStart = true
                                    }
                                }
                            }.onFailure { throwable ->
                                supportingErrorTextStart = throwable.message ?: ""
                                invalidStart = true
                            }
                        }
                    },
                    label = { Text(text = stringResource(R.string.label_lower_bound)) },
                    internalTrailingIcon = {
                        IconButton(
                            enabled = start.text.isNotBlank(),
                            onClick = {
                                start = TextFieldValue("")
                                startInFocus = true
                                endInFocus = false
                                invalidStart = false
                            }
                        ) { Icon(imageVector = Icons.Outlined.Clear, contentDescription = null) }
                    },
                    regex = Regex("^[0-9A-Fa-f]{0,4}$"),
                    isError = invalidStart,
                    supportingText = {
                        if (invalidStart)
                            Text(
                                text = supportingErrorTextStart,
                                color = MaterialTheme.colorScheme.error
                            )
                    }
                )
                Spacer(modifier = Modifier.size(8.dp))
                MeshOutlinedHexTextField(
                    modifier = Modifier.clickable {
                        startInFocus = false
                        endInFocus = true
                    },
                    showPrefix = true,
                    onFocus = endInFocus,
                    value = end,
                    onValueChanged = {
                        startInFocus = false
                        endInFocus = true
                        end = it
                        if (end.text.trim().isNotEmpty()) {
                            runCatching {
                                invalidEnd = !isValidBound(end.text.toUShort(radix = 16))
                                if (!invalidEnd) {
                                    supportingErrorTextEnd = ""
                                    invalidEnd = false
                                }
                            }.onSuccess {
                                if (!invalidEnd) {
                                    high = end.text.toUShort(radix = 16)
                                    if (low != null && high!! < low!!) {
                                        supportingErrorTextEnd =
                                            "Upper bound must be >= to lower bound"
                                        invalidEnd = true
                                    }
                                }
                            }.onFailure { throwable ->
                                supportingErrorTextEnd = throwable.message ?: ""
                                invalidEnd = true
                            }
                        }
                    },
                    label = { Text(text = stringResource(R.string.label_upper_bound)) },
                    internalTrailingIcon = {
                        IconButton(
                            enabled = end.text.isNotBlank(),
                            onClick = {
                                end = TextFieldValue("")
                                startInFocus = false
                                endInFocus = true
                                invalidEnd = false
                            }
                        ) { Icon(imageVector = Icons.Outlined.Clear, contentDescription = null) }
                    },
                    regex = Regex("^[0-9A-Fa-f]{0,4}$"),
                    isError = invalidEnd,
                    supportingText = {
                        if (invalidEnd)
                            Text(
                                text = supportingErrorTextEnd,
                                color = MaterialTheme.colorScheme.error
                            )
                    }
                )
            }
        }
    )
}

@Composable
private fun UpdateRangeDialog(
    onDismissRequest: () -> Unit,
    range: Range,
    isValidBound: (UShort) -> Boolean,
    onConfirmClicked: (UShort, UShort) -> Unit,
) {
    var low by remember { mutableStateOf<UShort?>(null) }
    var high by remember { mutableStateOf<UShort?>(null) }
    var start by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                range.low.toHexString(format = HexFormat.UpperCase)
            )
        )
    }
    var end by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                range.high.toHexString(format = HexFormat.UpperCase)
            )
        )
    }
    var startInFocus by rememberSaveable { mutableStateOf(false) }
    var endInFocus by rememberSaveable { mutableStateOf(false) }
    var invalidStart by rememberSaveable { mutableStateOf(false) }
    var invalidEnd by rememberSaveable { mutableStateOf(false) }
    var supportingErrorTextStart by rememberSaveable { mutableStateOf("") }
    var supportingErrorTextEnd by rememberSaveable { mutableStateOf("") }
    MeshAlertDialog(
        onDismissRequest = onDismissRequest,
        onConfirmClick = {
            if (start.text.trim().isNotBlank() && end.text.trim().isNotBlank()) {
                val startValue = start.text.trim().toUShort(radix = 16)
                val endValue = end.text.trim().toUShort(radix = 16)
                runCatching { onConfirmClicked(startValue, endValue) }
                    .onSuccess { onDismissRequest() }
            }
        },
        onDismissClick = { onDismissRequest() },
        icon = Icons.Outlined.SwapHoriz,
        title = stringResource(R.string.title_edit_range),
        error = invalidStart || invalidEnd ||
                start.text
                    .trim()
                    .isBlank() ||
                end.text
                    .trim()
                    .isBlank(),
        content = {
            Column {
                Text(
                    text = stringResource(R.string.label_new_range_rationale)
                )
                Spacer(modifier = Modifier.size(16.dp))
                MeshOutlinedHexTextField(
                    modifier = Modifier.clickable {
                        startInFocus = true
                        endInFocus = false
                    },
                    label = { Text(text = stringResource(R.string.label_lower_bound)) },
                    showPrefix = true,
                    onFocus = startInFocus,
                    value = start,
                    onValueChanged = {
                        startInFocus = true
                        endInFocus = false
                        start = it
                        if (start.text.trim().isNotEmpty()) {
                            runCatching {
                                invalidStart = !isValidBound(start.text.toUShort(radix = 16))
                                if (!invalidStart) {
                                    supportingErrorTextStart = ""
                                    invalidStart = false
                                }
                            }.onSuccess {
                                if (!invalidStart) {
                                    low = start.text.toUShort(radix = 16)
                                    if (high != null && low!! > high!!) {
                                        supportingErrorTextStart =
                                            "Lower bound must be <= upper bound"
                                        invalidStart = true
                                    }
                                }
                            }.onFailure { throwable ->
                                supportingErrorTextStart = throwable.message ?: ""
                                invalidStart = true
                            }
                        }
                        if (start.text.trim().isNotEmpty()) {
                            runCatching {
                                invalidStart = !isValidBound(start.text.toUShort(radix = 16))
                            }.onFailure { throwable ->
                                supportingErrorTextStart = throwable.message ?: ""
                                invalidStart = true
                            }
                        }

                    },
                    internalTrailingIcon = {
                        IconButton(
                            enabled = start.text.isNotBlank(),
                            onClick = {
                                start = TextFieldValue("")
                                startInFocus = true
                                endInFocus = false
                                invalidStart = false
                            }
                        ) { Icon(imageVector = Icons.Outlined.Clear, contentDescription = null) }
                    },
                    regex = Regex("^[0-9A-Fa-f]{0,4}$"),
                    isError = invalidStart,
                    supportingText = {
                        if (invalidStart)
                            Text(
                                text = supportingErrorTextStart,
                                color = MaterialTheme.colorScheme.error
                            )
                    }
                )
                Spacer(modifier = Modifier.size(16.dp))
                MeshOutlinedHexTextField(
                    modifier = Modifier.clickable {
                        startInFocus = false
                        endInFocus = true
                    },
                    label = { Text(text = stringResource(R.string.label_lower_bound)) },
                    showPrefix = true,
                    onFocus = endInFocus,
                    value = end,
                    onValueChanged = {
                        startInFocus = false
                        endInFocus = true
                        end = it
                        if (end.text.trim().isNotEmpty()) {
                            runCatching {
                                invalidEnd = !isValidBound(end.text.toUShort(radix = 16))
                            }.onFailure { throwable ->
                                supportingErrorTextEnd = throwable.message ?: ""
                                invalidEnd = true
                            }
                        }
                    },
                    internalTrailingIcon = {
                        IconButton(
                            enabled = end.text.isNotBlank(),
                            onClick = {
                                end = TextFieldValue("")
                                startInFocus = false
                                endInFocus = true
                                invalidEnd = false
                            }
                        ) { Icon(imageVector = Icons.Outlined.Clear, contentDescription = null) }
                    },
                    regex = Regex("[0-9A-Fa-f]{0,4}"),
                    isError = invalidEnd,
                    supportingText = {
                        if (invalidEnd)
                            Text(
                                text = supportingErrorTextEnd,
                                color = MaterialTheme.colorScheme.error
                            )
                    }
                )
            }
        }
    )
}

@Composable
internal fun Range.toImageVector() = when (this) {
    is UnicastRange -> Icons.Outlined.Lan
    is GroupRange -> Icons.Outlined.GroupWork
    is SceneRange -> Icons.Outlined.AutoAwesome
}