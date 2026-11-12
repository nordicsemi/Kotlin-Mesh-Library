package no.nordicsemi.android.nrfmesh.core.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.VpnKey
import no.nordicsemi.kotlin.mesh.crypto.Crypto
import kotlin.uuid.Uuid

/**
 * Network properties
 *
 * @property networkKeys     Number of network keys.
 * @property applicationKeys Number of application keys.
 * @property groups          Number of groups.
 * @property virtualGroups   Number of virtual groups.
 * @property scenes          Number of scenes.
 */
sealed interface NetworkProperties {
    val networkKeys: Int
    val applicationKeys: Int
    val groups: Int
    val virtualGroups: Int
    val scenes: Int
}

/**
 * Defines the network configuration used by the Network Wizard
 */
sealed class Configuration : NetworkProperties {

    /**
     * Generate network keys.
     */
    abstract fun generateNetworkKeys(): List<ByteArray>

    /**
     * Generate application keys.
     */
    abstract fun generateApplicationKeys(): List<ByteArray>

    /**
     * Generate the label UUIDs of the virtual groups.
     */
    abstract fun generateVirtualGroupUuids(): List<Uuid>

    /**
     * Empty configuration.
     */
    data object Empty : Configuration(), NetworkProperties {
        override val networkKeys = 1
        override val applicationKeys = 0
        override val groups = 0
        override val virtualGroups = 0
        override val scenes = 0

        override fun generateNetworkKeys(): List<ByteArray> = listOf(Crypto.generateRandomKey())

        override fun generateApplicationKeys(): List<ByteArray> = emptyList()

        override fun generateVirtualGroupUuids(): List<Uuid> = emptyList()
    }

    /**
     * Custom configuration.
     */
    data class Custom(
        override val networkKeys: Int = 1,
        override val applicationKeys: Int = 1,
        override val groups: Int = 3,
        override val virtualGroups: Int = 1,
        override val scenes: Int = 4,
    ) : Configuration(), NetworkProperties {

        override fun generateNetworkKeys() = List(size = networkKeys) {
            Crypto.generateRandomKey()
        }

        override fun generateApplicationKeys() = List(size = networkKeys) {
            Crypto.generateRandomKey()
        }

        override fun generateVirtualGroupUuids() = List(size = virtualGroups) { Uuid.random() }
    }

    /**
     * Debug configuration.
     */
    data class Debug(
        override val networkKeys: Int = 1,
        override val applicationKeys: Int = 1,
        override val groups: Int = 3,
        override val virtualGroups: Int = 1,
        override val scenes: Int = 4,
    ) : Configuration(), NetworkProperties {

        override fun generateNetworkKeys() = generateStaticKeys(size = networkKeys)

        override fun generateApplicationKeys() = generateStaticKeys(size = applicationKeys)

        /**
         * Generates static label UUIDs incremented by 1, from 1 to [virtualGroups], that is
         * 00000000-0000-0000-0000-000000000001, 00000000-0000-0000-0000-000000000002 and so on.
         * This is used for creating debug networks.
         */
        override fun generateVirtualGroupUuids() = List(size = virtualGroups) { index ->
            Uuid.fromULongs(mostSignificantBits = 0uL, leastSignificantBits = (index + 1).toULong())
        }

        /**
         * Generate static keys incremented by 1. This is used for creating debug networks
         */
        private fun generateStaticKeys(size: Int) = List(size = size) { sizeIndex ->
            ByteArray(size = 16) { byteIndex ->
                if (byteIndex == 15) (sizeIndex + 1).toByte()
                else 0.toByte()
            }
        }
    }
}


/**
 * Returns the icon for the configuration.
 */
fun Configuration.icon() = when (this) {
    is Configuration.Empty -> Icons.Outlined.CheckBoxOutlineBlank
    is Configuration.Custom -> Icons.Outlined.DashboardCustomize
    is Configuration.Debug -> Icons.Outlined.BugReport
}

/**
 * Returns the description for the configuration.
 */
fun Configuration.description(): String {
    return when (this) {
        is Configuration.Empty -> "Empty"
        is Configuration.Custom -> "Custom"
        is Configuration.Debug -> "Debug"
    }
}

/**
 * Configuration property.
 */
enum class ConfigurationProperty {
    NETWORK_KEYS,
    APPLICATION_KEYS,
    GROUPS,
    VIRTUAL_GROUPS,
    SCENES
}

/**
 * Returns the description for the configuration property.
 */
fun ConfigurationProperty.description(): String = when (this) {
    ConfigurationProperty.NETWORK_KEYS -> "Network Keys"
    ConfigurationProperty.APPLICATION_KEYS -> "Application Keys"
    ConfigurationProperty.GROUPS -> "Groups"
    ConfigurationProperty.VIRTUAL_GROUPS -> "Virtual Groups"
    ConfigurationProperty.SCENES -> "Scenes"
}

/**
 * Returns the icon for the configuration property.
 */
fun ConfigurationProperty.icon() = when (this) {
    ConfigurationProperty.NETWORK_KEYS,
    ConfigurationProperty.APPLICATION_KEYS,
        -> Icons.Outlined.VpnKey

    ConfigurationProperty.GROUPS,
    ConfigurationProperty.VIRTUAL_GROUPS,
        -> Icons.Outlined.GroupWork

    ConfigurationProperty.SCENES -> Icons.Outlined.AutoAwesome
}

/**
 * Action
 */
enum class Action {
    /**
     * Add action.
     */
    ADD,

    /**
     * Remove action.
     */
    REMOVE
}