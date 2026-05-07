package no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.feature.provisioners.R
import no.nordicsemi.kotlin.mesh.core.model.Range
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastRange
import no.nordicsemi.kotlin.mesh.core.model.minus
import no.nordicsemi.kotlin.mesh.core.model.overlaps
import no.nordicsemi.kotlin.mesh.core.model.plus
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
internal fun UnicastRangesScreen(
    snackbarHostState: SnackbarHostState,
    unicastRanges: List<UnicastRange>,
    otherUnicastRanges: List<UnicastRange>,
    allocate: (List<Range>) -> Unit,
    removeAllRanges: () -> Unit,
    save: () -> Unit,
    navigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ranges = remember {
        mutableStateListOf<Range>()
            .apply { addAll(elements = unicastRanges) }
    }
    val overlaps by remember {
        derivedStateOf { ranges.overlaps(other = otherUnicastRanges) }
    }
    RangesScreen(
        title = stringResource(id = R.string.label_unicast_ranges),
        ranges = ranges,
        otherRanges = otherUnicastRanges,
        overlaps = overlaps,
        addRange = { start, end ->
            val range = UnicastAddress(address = start)..UnicastAddress(address = end)
            val list = ranges
                .toList()
                .plus(other = range)
            ranges.clear()
            ranges.addAll(list)
        },
        onRangeUpdated = { start, end ->
            val newRange =
                UnicastAddress(address = start)..UnicastAddress(address = end)
            val list = ranges
                .toList()
                .plus(other = newRange)
            ranges.clear()
            ranges.addAll(list)
        },
        onSwiped = {
            val list = ranges
                .toList()
                .minus(other = it)
            ranges.clear()
            ranges.addAll(list)
        },
        isValidBound = { UnicastAddress.isValid(address = it) },
        resolve = {
            val list = ranges
                .toList()
                .minus(other = otherUnicastRanges)
            ranges.clear()
            ranges.addAll(list)
        },
        save = {
            runCatching {
                removeAllRanges()
                allocate(ranges)
            }.onSuccess {
                save()
                navigateBack()
            }.onFailure {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = it.message ?: context.getString(R.string.label_failed_to_allocate_ranges),
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    )
}