package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Gap kept below the last item of a screen, in addition to the bottom system inset. It matches the
 * bottom padding of the detail pane placeholder so list and detail panes line up.
 */
val BottomContentPadding: Dp = 16.dp

/**
 * Empty space as tall as the bottom system inset (navigation bar / tablet taskbar).
 *
 * Place it as the last child of a scrollable so that the content can be scrolled clear of the
 * system bars while still being drawn edge to edge. Pass the parent's `spacedBy` value as
 * [spacing] so that it is not added on top of [extra].
 */
@Composable
fun BottomInsetSpacer(
    modifier: Modifier = Modifier,
    extra: Dp = BottomContentPadding,
    spacing: Dp = 0.dp,
) {
    val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Spacer(modifier = modifier.height(height = inset + extra - spacing))
}

/**
 * Content padding for lazy lists that keeps the last item clear of the bottom system inset.
 */
@Composable
fun bottomInsetPadding(extra: Dp = BottomContentPadding): PaddingValues {
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(bottom = bottom + extra)
}
