@file:OptIn(ExperimentalMaterial3Api::class)

package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties


/**
 * Common Alert Dialog composable to maintain consistency.
 *
 * @param onDismissRequest              Called when the user tries to dismiss the Dialog by clicking
 *                                      outside or pressing the back button. This is not called when
 *                                      the dismiss button is clicked.
 * @param confirmButtonText             Button which is meant to confirm a proposed action, thus
 *                                      resolving what triggered the dialog. The dialog does not set
 *                                      up any events for this button so they need to be set up by
 *                                      the caller.
 * @param dismissButtonText             Button which is meant to dismiss the dialog. The dialog does
 *                                      not set up any events for this button so they need to be
 *                                      setup by the caller. Set null to hide the dismiss button.
 * @param icon                          Optional icon that will appear above the [title] or above
 *                                      the [text], in case a title was not provided.
 * @param title                         Title which should specify the purpose of the dialog. The
 *                                      title is not mandatory, because there may be sufficient
 *                                      information inside the [text].
 * @param error                         When true the confirm button will be disabled
 * @param content                       Content of the dialog body.
 */
// TODO needs to be revisited
@Composable
fun MeshAlertDialog(
    onDismissRequest: () -> Unit = {},
    confirmButtonText: String = stringResource(id = R.string.confirm),
    onConfirmClick: () -> Unit,
    dismissButtonText: String? = stringResource(id = R.string.cancel),
    onDismissClick: () -> Unit = {},
    icon: ImageVector? = null,
    iconColor: Color = MaterialTheme.colorScheme.error,
    title: String? = null,
    error: Boolean = false,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = !isCompactWidth(),
        decorFitsSystemWindows = !isCompactWidth()
    ),
    content: @Composable () -> Unit = {},
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.85f),
        properties = properties,
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(
                enabled = !error,
                onClick = { onConfirmClick() }
            ) { Text(text = confirmButtonText) }
        },
        dismissButton = {
            dismissButtonText?.let {
                TextButton(onClick = { onDismissClick() }) { Text(text = it) }
            }
        },
        icon = {
            icon?.let { Icon(imageVector = it, contentDescription = null, tint = iconColor) }
        },
        title = { title?.let { Text(text = it) } },
        text = content
    )
}

/**
 * Common Alert Dialog composable to maintain consistency.
 *
 * @param onDismissRequest              Called when the user tries to dismiss the Dialog by clicking
 *                                      outside or pressing the back button. This is not called when
 *                                      the dismiss button is clicked.
 * @param confirmButtonText             Button which is meant to confirm a proposed action, thus
 *                                      resolving what triggered the dialog. The dialog does not set
 *                                      up any events for this button so they need to be set up by
 *                                      the caller.
 * @param dismissButtonText             Button which is meant to dismiss the dialog. The dialog does
 *                                      not set up any events for this button so they need to be set
 *                                      up by the caller.
 * @param icon                          Optional icon that will appear above the [title] or above
 *                                      the [text], in case a title was not provided.
 * @param title                         Title which should specify the purpose of the dialog. The
 *                                      title is not mandatory, because there may be sufficient
 *                                      information inside the [text].
 * @param text                          Text which presents the details regarding the dialog's
 *                                      purpose.
 */
@Composable
fun MeshAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButtonText: String = stringResource(id = R.string.confirm),
    onConfirmClick: () -> Unit,
    dismissButtonText: String? = stringResource(id = R.string.cancel),
    onDismissClick: () -> Unit = {},
    icon: ImageVector? = null,
    iconColor: Color = AlertDialogDefaults.iconContentColor,
    title: String? = null,
    text: String? = null,
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(0.85f),
        properties = DialogProperties(
            usePlatformDefaultWidth = !isCompactWidth(),
            decorFitsSystemWindows = !isCompactWidth()
        ),
        onDismissRequest = { onDismissRequest() },
        confirmButton = {
            TextButton(onClick = { onConfirmClick() }) { Text(text = confirmButtonText) }
        },
        dismissButton = {
            dismissButtonText?.let {
                TextButton(onClick = { onDismissClick() }) { Text(text = it) }
            }
        },
        icon = {
            icon?.let { Icon(imageVector = it, contentDescription = null, tint = iconColor) }
        },
        title = { title?.let { Text(text = it) } },
        text = { text?.let { Text(text = it) } }
    )
}

/**
 * Common Alert Dialog composable to maintain consistency.
 *
 * @param onDismissRequest              Called when the user tries to dismiss the Dialog by clicking
 *                                      outside or pressing the back button. This is not called when
 *                                      the dismiss button is clicked.
 * @param content                       Content of the Mesh alert dialog.
 */
@Composable
fun MeshAlertDialog(
    onDismissRequest: () -> Unit,
    icon: ImageVector? = null,
    iconColor: Color = AlertDialogDefaults.iconContentColor,
    title: String? = null,
    text: String? = null,
    content: @Composable () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        content = {
            Surface(
                modifier = Modifier
                    .wrapContentWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = AlertDialogDefaults.TonalElevation
            ) {
                Column(
                    modifier = Modifier.padding(PaddingValues(all = 24.dp))
                ) {
                    icon?.let {
                        CompositionLocalProvider(
                            LocalContentColor provides AlertDialogDefaults.iconContentColor
                        ) {
                            Box(
                                Modifier
                                    .padding(PaddingValues(bottom = 16.dp))
                                    .align(Alignment.CenterHorizontally)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor
                                )
                            }
                        }
                    }
                    title?.let {
                        CompositionLocalProvider(
                            LocalContentColor provides AlertDialogDefaults.titleContentColor
                        ) {
                            val textStyle = MaterialTheme.typography.headlineSmall
                            ProvideTextStyle(textStyle) {
                                Box(
                                    // Align the title to the center when an icon is present.
                                    Modifier
                                        .padding(PaddingValues(bottom = 16.dp))
                                        .align(
                                            if (icon == null) {
                                                Alignment.Start
                                            } else {
                                                Alignment.CenterHorizontally
                                            }
                                        )
                                ) {
                                    Text(text = it)
                                }
                            }
                        }
                    }
                    text?.let {
                        CompositionLocalProvider(
                            LocalContentColor provides AlertDialogDefaults.textContentColor
                        ) {
                            val textStyle = MaterialTheme.typography.bodyMedium
                            ProvideTextStyle(textStyle) {
                                Box(
                                    Modifier
                                        .weight(weight = 1f, fill = false)
                                        .padding(PaddingValues(bottom = 24.dp))
                                        .align(Alignment.Start)
                                ) {
                                    Text(text = it)
                                }
                            }
                        }
                    }
                    content()
                }
            }
        }
    )
}

@Composable
fun MeshMessageStatusDialog(
    text: String,
    showDismissButton: Boolean,
    onDismissRequest: () -> Unit,
    usePlatformDefaultWidth: Boolean = !isCompactWidth(),
) {
    AlertDialog(
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = usePlatformDefaultWidth,
            decorFitsSystemWindows = !isCompactWidth()
        ),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            if (showDismissButton) {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(android.R.string.cancel))
                }
            }
        },
        icon = { Icon(imageVector = Icons.Outlined.Mail, contentDescription = null) },
        title = { Text(text = stringResource(R.string.title_message_status)) },
        text = { Text(text = text) }
    )
}