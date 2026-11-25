package no.nordicsemi.android.nrfmesh.feature.model.generic

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.East
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.North
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedTextField
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.Altitude
import no.nordicsemi.kotlin.mesh.core.messages.FloorNumber
import no.nordicsemi.kotlin.mesh.core.messages.Latitude
import no.nordicsemi.kotlin.mesh.core.messages.LocalLocationStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.LocationUncertainty
import no.nordicsemi.kotlin.mesh.core.messages.Longitude
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalStatus
import no.nordicsemi.kotlin.mesh.core.model.Model
import java.util.Locale

/** Floors that can be selected: the ground floors, unknown and the numbered floors. */
// The summit of Mount Everest, which the global location fields start with.
private const val EVEREST_LATITUDE = "27.988056"
private const val EVEREST_LONGITUDE = "86.925278"
private const val EVEREST_ALTITUDE = "8849"

internal val floors: List<FloorNumber> =
    listOf(FloorNumber.GROUND_FLOOR_0, FloorNumber.GROUND_FLOOR_1, FloorNumber.UNKNOWN) +
            (FloorNumber.MIN..FloorNumber.MAX).mapNotNull { FloorNumber.of(it) }

/**
 * Controls of the Generic Location Setup Server model, which sets the global and local location
 * of the node. This model handles only Set messages, so the status shown is the response to an
 * acknowledged Set. The current locations are read using the Generic Location Server model.
 */
@Composable
internal fun GenericLocationSetupServer(
    model: Model,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val hasBoundKey = model.boundApplicationKeys.isNotEmpty()
    // The values cannot be changed while a message is sent, so that the screen always shows what
    // was sent until the response is received.
    val enabled = hasBoundKey && !messageState.isInProgress()
    // Keeps the location of the node received as a response to a Set.
    val state = rememberLocationState(messageState = messageState)
    var selected by rememberSaveable { mutableStateOf(LocationKind.GLOBAL) }
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_controls)
    )
    LocationSelector(selected = selected, onSelected = { selected = it }, enabled = hasBoundKey)
    // The state of each side is kept while the other one is shown, so that typed values are not lost.
    val stateHolder = rememberSaveableStateHolder()
    StableHeight {
    stateHolder.SaveableStateProvider(key = selected.name) {
        when (selected) {
            LocationKind.GLOBAL -> {
                GlobalControls(
                    model = model,
                    status = state.global,
                    messageState = messageState,
                    enabled = enabled,
                    sendApplicationMessage = sendApplicationMessage
                )
                GlobalStatus(status = state.global)
            }

            LocationKind.LOCAL -> {
                LocalControls(
                    model = model,
                    status = state.local,
                    messageState = messageState,
                    enabled = enabled,
                    sendApplicationMessage = sendApplicationMessage
                )
                LocalStatus(status = state.local)
            }
        }
    }
    }
}

/** Creates the message used to set the global location. */
internal fun globalLocationSetOf(
    latitude: Latitude,
    longitude: Longitude,
    altitude: Altitude,
    acknowledged: Boolean,
): MeshMessage = if (acknowledged) {
    GenericLocationGlobalSet(latitude, longitude, altitude)
} else {
    GenericLocationGlobalSetUnacknowledged(latitude, longitude, altitude)
}

/**
 * Creates the message used to set the local location. The update time and precision of the
 * uncertainty are sent as unknown, as they are not known to the setting node.
 */
internal fun localLocationSetOf(
    north: Short,
    east: Short,
    altitude: Short,
    floor: FloorNumber,
    isMobile: Boolean,
    acknowledged: Boolean,
): MeshMessage {
    val uncertainty = LocationUncertainty.of(isMobile = isMobile)
    return if (acknowledged) {
        GenericLocationLocalSet(north, east, altitude, floor, uncertainty)
    } else {
        GenericLocationLocalSetUnacknowledged(north, east, altitude, floor, uncertainty)
    }
}

/** Displays the status received as the response to an acknowledged Global Location Set. */
@Composable
private fun GlobalStatus(status: GenericLocationGlobalStatus?) {
    val resources = LocalResources.current
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_status)
    )
    StatusItem(
        icon = Icons.Outlined.SwapVert,
        title = stringResource(R.string.label_latitude),
        value = status?.latitude?.describe(resources)
    )
    StatusItem(
        icon = Icons.Outlined.SwapHoriz,
        title = stringResource(R.string.label_longitude),
        value = status?.longitude?.describe(resources)
    )
    StatusItem(
        icon = Icons.Outlined.Terrain,
        title = stringResource(R.string.label_altitude),
        value = status?.altitude?.describe(resources)
    )
}

/** Displays the status received as the response to an acknowledged Local Location Set. */
@Composable
private fun LocalStatus(status: GenericLocationLocalStatus?) {
    val resources = LocalResources.current
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_status)
    )
    StatusItem(
        icon = Icons.Outlined.North,
        title = stringResource(R.string.label_north),
        value = status?.north?.describeCoordinate(resources)
    )
    StatusItem(
        icon = Icons.Outlined.East,
        title = stringResource(R.string.label_east),
        value = status?.east?.describeCoordinate(resources)
    )
    StatusItem(
        icon = Icons.Outlined.Terrain,
        title = stringResource(R.string.label_altitude),
        value = status?.altitude?.describeLocalAltitude(resources)
    )
    StatusItem(
        icon = Icons.Outlined.Apartment,
        title = stringResource(R.string.label_floor),
        value = status?.floor?.describe(resources)
    )
    StatusItem(
        icon = Icons.Outlined.MyLocation,
        title = stringResource(R.string.label_movement),
        value = status?.uncertainty?.let {
            resources.getString(if (it.isMobile) R.string.label_mobile else R.string.label_stationary)
        }
    )
    StatusItem(
        icon = Icons.Outlined.Timer,
        title = stringResource(R.string.label_update_time),
        value = status?.let {
            "${it.uncertainty.updateTimeMillis.thousandthsToString()} s"
        }
    )
    StatusItem(
        icon = Icons.Outlined.Straighten,
        title = stringResource(R.string.label_precision),
        value = status?.let {
            "${it.uncertainty.precisionMillimeters.thousandthsToString()} m"
        }
    )
}

/** A status card with the title on the left and the value on the right. */
@Composable
private fun StatusItem(icon: ImageVector, title: String, value: String?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = icon,
        title = title,
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = value ?: stringResource(R.string.label_unknown).uppercase()
            )
        }
    )
}

@Composable
private fun GlobalControls(
    model: Model,
    status: GenericLocationGlobalStatus?,
    messageState: MessageState,
    enabled: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    var latitude by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(EVEREST_LATITUDE))
    }
    var longitude by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(EVEREST_LONGITUDE))
    }
    var altitude by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(EVEREST_ALTITUDE))
    }
    var acknowledged by rememberSaveable { mutableStateOf(true) }
    val context = LocalContext.current
    var usePhoneLocation by rememberSaveable { mutableStateOf(false) }
    var phoneResult by remember { mutableStateOf<PhoneLocationResult?>(null) }
    var hasPermission by remember { mutableStateOf(context.hasLocationPermission()) }

    fun show(location: PhoneLocation) {
        latitude = TextFieldValue("%.6f".format(Locale.US, location.latitude))
        longitude = TextFieldValue("%.6f".format(Locale.US, location.longitude))
        location.altitudeMeters?.let { altitude = TextFieldValue(it.toString()) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        hasPermission = granted.values.any { it } || context.hasLocationPermission()
        if (hasPermission) {
            usePhoneLocation = true
        } else {
            phoneResult = PhoneLocationResult.PermissionDenied
        }
    }

    fun requestPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    // The fields keep their values while a message is sent, so that they show what was sent.
    val isSending by rememberUpdatedState(newValue = messageState.isInProgress())

    // Keeps the fields in sync with the location of the phone while the switch is on.
    LaunchedEffect(key1 = usePhoneLocation, key2 = hasPermission) {
        if (usePhoneLocation && hasPermission) {
            phoneResult = null
            context.phoneLocationUpdates().collect { result ->
                phoneResult = result
                if (result is PhoneLocationResult.Found) {
                    if (!isSending) show(result.location)
                } else {
                    usePhoneLocation = false
                }
            }
        }
    }

    // Shows the location reported by the node when it is read.
    LaunchedEffect(key1 = status) {
        status?.let {
            latitude = TextFieldValue(it.latitude.degrees?.let { v -> "%.6f".format(Locale.US, v) } ?: "")
            longitude = TextFieldValue(it.longitude.degrees?.let { v -> "%.6f".format(Locale.US, v) } ?: "")
            altitude = TextFieldValue(it.altitude.meters?.toString() ?: "")
        }
    }
    val parsedLatitude = latitude.text.toDoubleOrNull()?.let { Latitude.fromDegrees(it) }
    val parsedLongitude = longitude.text.toDoubleOrNull()?.let { Longitude.fromDegrees(it) }
    val parsedAltitude = altitude.text.toShortOrNull()?.let { Altitude(it) }
    ControlsCard(
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                MeshSingleLineListItem(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Outlined.GpsFixed,
                    title = stringResource(R.string.label_use_phone_location),
                    trailingComposable = {
                        Switch(
                            enabled = enabled,
                            checked = usePhoneLocation,
                            onCheckedChange = { checked ->
                                if (!checked) {
                                    usePhoneLocation = false
                                    phoneResult = null
                                } else if (hasPermission) {
                                    usePhoneLocation = true
                                } else {
                                    requestPermission()
                                }
                            }
                        )
                    }
                )
                PhoneLocationMessage(
                    isFetching = usePhoneLocation && phoneResult == null,
                    result = phoneResult
                )
                NumberField(
                    label = stringResource(R.string.label_latitude),
                    value = latitude,
                    onValueChanged = { latitude = it },
                    enabled = enabled,
                    readOnly = usePhoneLocation,
                    isError = parsedLatitude == null,
                    isDecimal = true,
                    suffix = "°",
                    leadingIcon = Icons.Outlined.SwapVert
                )
                NumberField(
                    label = stringResource(R.string.label_longitude),
                    value = longitude,
                    onValueChanged = { longitude = it },
                    enabled = enabled,
                    readOnly = usePhoneLocation,
                    isError = parsedLongitude == null,
                    isDecimal = true,
                    suffix = "°",
                    leadingIcon = Icons.Outlined.SwapHoriz
                )
                NumberField(
                    label = stringResource(R.string.label_altitude_meters),
                    value = altitude,
                    onValueChanged = { altitude = it },
                    enabled = enabled,
                    readOnly = usePhoneLocation,
                    isError = parsedAltitude == null,
                    leadingIcon = Icons.Outlined.Terrain
                )
                AcknowledgedItem(
                    enabled = enabled,
                    acknowledged = acknowledged,
                    onAcknowledgedChange = { acknowledged = it }
                )
            }
        },
        actions = {
            MeshOutlinedButton(
                text = stringResource(R.string.label_set),
                buttonIcon = Icons.Outlined.Upload,
                enabled = enabled && parsedLatitude != null && parsedLongitude != null &&
                        parsedAltitude != null,
                isOnClickActionInProgress = messageState.isInProgress() &&
                        (messageState.message is GenericLocationGlobalSet ||
                                messageState.message is GenericLocationGlobalSetUnacknowledged),
                onClick = {
                    if (parsedLatitude != null && parsedLongitude != null && parsedAltitude != null) {
                        sendApplicationMessage(
                            model,
                            globalLocationSetOf(
                                latitude = parsedLatitude,
                                longitude = parsedLongitude,
                                altitude = parsedAltitude,
                                acknowledged = acknowledged
                            )
                        )
                    }
                }
            )
        }
    )
}

/**
 * A card for the controls of a location, laid out like an elevated card item without its header:
 * the body and the actions are padded the same way.
 */
@Composable
private fun ControlsCard(
    body: @Composable ColumnScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    OutlinedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp)
                .padding(horizontal = 16.dp),
            content = body
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
            content = actions
        )
    }
}

/** Describes what happened when the location of the phone was read. */
@Composable
private fun PhoneLocationMessage(isFetching: Boolean, result: PhoneLocationResult?) {
    val lines = buildList {
        if (isFetching) add(stringResource(R.string.label_phone_location_fetching))
        when (result) {
            is PhoneLocationResult.Found -> {
                add(
                    stringResource(
                        if (result.location.isApproximate) R.string.label_phone_location_approximate
                        else R.string.label_phone_location_found
                    )
                )
                if (result.location.altitudeMeters == null) {
                    add(stringResource(R.string.label_phone_location_no_altitude))
                }
            }

            PhoneLocationResult.PermissionDenied ->
                add(stringResource(R.string.label_phone_location_denied))

            PhoneLocationResult.ProviderDisabled ->
                add(stringResource(R.string.label_phone_location_disabled))

            PhoneLocationResult.Unavailable ->
                add(stringResource(R.string.label_phone_location_unavailable))

            null -> {}
        }
    }
    if (lines.isNotEmpty()) {
        Text(
            text = lines.joinToString(separator = ". "),
            style = MaterialTheme.typography.bodySmall,
            color = if (result == null || result is PhoneLocationResult.Found) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalControls(
    model: Model,
    status: GenericLocationLocalStatus?,
    messageState: MessageState,
    enabled: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val resources = LocalResources.current
    var north by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var east by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var altitude by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var floorIndex by rememberSaveable { mutableIntStateOf(0) }
    var isFloorExpanded by rememberSaveable { mutableStateOf(false) }
    var isMobile by rememberSaveable { mutableStateOf(false) }
    var acknowledged by rememberSaveable { mutableStateOf(true) }
    // Shows the location reported by the node when it is read.
    LaunchedEffect(key1 = status) {
        status?.let {
            fun Short.orEmpty(notConfigured: Short) =
                if (this == notConfigured) "" else toString()
            north = TextFieldValue(it.north.orEmpty(LocalLocationStatusMessage.LOCAL_NOT_CONFIGURED))
            east = TextFieldValue(it.east.orEmpty(LocalLocationStatusMessage.LOCAL_NOT_CONFIGURED))
            altitude = TextFieldValue(it.altitude.orEmpty(Altitude.NOT_CONFIGURED))
            floors.indexOf(it.floor).takeIf { index -> index >= 0 }?.let { index -> floorIndex = index }
            isMobile = it.uncertainty.isMobile
        }
    }
    val parsedNorth = north.text.toShortOrNull()
    val parsedEast = east.text.toShortOrNull()
    val parsedAltitude = altitude.text.toShortOrNull()
    ControlsCard(
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                NumberField(
                    label = stringResource(R.string.label_north_decimeters),
                    value = north,
                    onValueChanged = { north = it },
                    enabled = enabled,
                    isError = parsedNorth == null,
                    leadingIcon = Icons.Outlined.North
                )
                NumberField(
                    label = stringResource(R.string.label_east_decimeters),
                    value = east,
                    onValueChanged = { east = it },
                    enabled = enabled,
                    isError = parsedEast == null,
                    leadingIcon = Icons.Outlined.East
                )
                NumberField(
                    label = stringResource(R.string.label_altitude_decimeters),
                    value = altitude,
                    onValueChanged = { altitude = it },
                    enabled = enabled,
                    isError = parsedAltitude == null,
                    leadingIcon = Icons.Outlined.Terrain
                )
                ExposedDropdownMenuBox(
                    expanded = isFloorExpanded,
                    onExpandedChange = { isFloorExpanded = it && enabled },
                ) {
                    MeshOutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        externalLeadingIcon = {
                            Icon(
                                modifier = Modifier.padding(end = 16.dp),
                                imageVector = Icons.Outlined.Apartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        label = { Text(text = stringResource(R.string.label_floor)) },
                        value = TextFieldValue(floors[floorIndex].describe(resources)),
                        onValueChanged = {},
                        readOnly = true,
                        enabled = enabled,
                        isError = false,
                        internalTrailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFloorExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = isFloorExpanded,
                        onDismissRequest = { isFloorExpanded = false },
                    ) {
                        floors.forEachIndexed { index, floor ->
                            DropdownMenuItem(
                                text = { Text(text = floor.describe(resources)) },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                onClick = {
                                    floorIndex = index
                                    isFloorExpanded = false
                                }
                            )
                        }
                    }
                }
                MeshSingleLineListItem(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Outlined.MyLocation,
                    title = stringResource(R.string.label_mobile),
                    trailingComposable = {
                        Switch(enabled = enabled, checked = isMobile, onCheckedChange = { isMobile = it })
                    }
                )
                AcknowledgedItem(
                    enabled = enabled,
                    acknowledged = acknowledged,
                    onAcknowledgedChange = { acknowledged = it }
                )
            }
        },
        actions = {
            MeshOutlinedButton(
                text = stringResource(R.string.label_set),
                buttonIcon = Icons.Outlined.Upload,
                enabled = enabled && parsedNorth != null && parsedEast != null &&
                        parsedAltitude != null,
                isOnClickActionInProgress = messageState.isInProgress() &&
                        (messageState.message is GenericLocationLocalSet ||
                                messageState.message is GenericLocationLocalSetUnacknowledged),
                onClick = {
                    if (parsedNorth != null && parsedEast != null && parsedAltitude != null) {
                        sendApplicationMessage(
                            model,
                            localLocationSetOf(
                                north = parsedNorth,
                                east = parsedEast,
                                altitude = parsedAltitude,
                                floor = floors[floorIndex],
                                isMobile = isMobile,
                                acknowledged = acknowledged
                            )
                        )
                    }
                }
            )
        }
    )
}

@Composable
private fun NumberField(
    label: String,
    value: TextFieldValue,
    onValueChanged: (TextFieldValue) -> Unit,
    enabled: Boolean,
    isError: Boolean,
    readOnly: Boolean = false,
    isDecimal: Boolean = false,
    suffix: String? = null,
    leadingIcon: ImageVector? = null,
) {
    MeshOutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        externalLeadingIcon = {
            leadingIcon?.let {
                Icon(
                    modifier = Modifier.padding(end = 16.dp),
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        label = { Text(text = label) },
        suffix = suffix?.let { { Text(text = it) } },
        value = value,
        onValueChanged = onValueChanged,
        enabled = enabled,
        readOnly = readOnly,
        focusable = !readOnly,
        isError = isError && value.text.isNotEmpty(),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
        )
    )
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
