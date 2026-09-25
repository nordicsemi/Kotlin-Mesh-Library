package no.nordicsemi.android.nrfmesh.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Hive
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Hive
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid


/**
 * Represents the top level navigation items.
 *
 * @property selectedIcon    Icon to show when this item is selected
 * @property unselectedIcon  Icon to show when this item is not selected
 * @property iconTextId      String resource
 * @property titleTextId     String resource
 */
data class MeshTopLevelNavItem(
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    @StringRes val iconTextId: Int,
    @StringRes val titleTextId: Int,
)

/**
 * Top level nodes navigation item.
 */
val NODES = MeshTopLevelNavItem(
    selectedIcon = Icons.Filled.Hive,
    unselectedIcon = Icons.Outlined.Hive,
    iconTextId = R.string.label_nav_bar_nodes,
    titleTextId = R.string.label_nav_bar_nodes
)

/**
 * Top level groups navigation item.
 */
val GROUPS = MeshTopLevelNavItem(
    selectedIcon = Icons.Filled.GroupWork,
    unselectedIcon = Icons.Outlined.GroupWork,
    iconTextId = R.string.label_nav_bar_groups,
    titleTextId = R.string.label_nav_bar_groups,
)

/**
 * Top level proxy navigation item.
 */
val PROXY = MeshTopLevelNavItem(
    selectedIcon = Icons.Filled.Hub,
    unselectedIcon = Icons.Outlined.Hub,
    iconTextId = R.string.label_nav_bar_proxy,
    titleTextId = R.string.label_nav_bar_proxy,
)

/**
 * Top level settings navigation item.
 */
val SETTINGS = MeshTopLevelNavItem(
    selectedIcon = Icons.Filled.Settings,
    unselectedIcon = Icons.Outlined.Settings,
    iconTextId = R.string.label_nav_bar_settings,
    titleTextId = R.string.label_nav_bar_settings,
)

/**
 * Unique key identifying [NODES] top level navigation item.
 */
@Serializable
object NodesKey : NavKey

/**
 * Unique key identifying [GROUPS] top level navigation item.
 */
@Serializable
object GroupsKey : NavKey

/**
 * Unique key identifying [PROXY] top level navigation item.
 */
@Serializable
object ProxyKey : NavKey

/**
 * Unique key identifying [SETTINGS] top level navigation item.
 */
@Serializable
data class SettingsKey(val setting: ClickableSetting? = null) : NavKey

@Serializable
data object FirmwareUpdateKey : NavKey

@OptIn(ExperimentalUuidApi::class)
@Serializable
data class ScannerKey(val uuid: Uuid) : NavKey

/**
 * Map of top level navigation items.
 */
val MESH_TOP_LEVEL_NAV_ITEMS = mapOf(
    NodesKey to NODES,
    GroupsKey to GROUPS,
    ProxyKey to PROXY,
    SettingsKey(setting = null) to SETTINGS,
)