package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Background shown behind an item that is swiped to be deleted.
 *
 * @param dismissState State of the swipe to dismiss box.
 * @param isDeletable  Whether the item can be deleted. Items that can't be deleted are shown in gray.
 */
@Composable
fun SwipeToDeleteBackground(
    dismissState: SwipeToDismissBoxState,
    isDeletable: Boolean = true,
) {
    val color by animateColorAsState(if (isDeletable) errorColor else Color.Gray)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = color, shape = CardDefaults.elevatedShape)
            .padding(horizontal = 16.dp),
        contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
            Alignment.CenterStart
        else Alignment.CenterEnd
    ) {
        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
    }
}
