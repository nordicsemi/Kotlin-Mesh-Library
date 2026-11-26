package no.nordicsemi.android.nrfmesh.feature.model.generic

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SensorOccupied
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

/** The keywords of a property name, and the icon shown for a property whose name has one. */
private val propertyIcons: List<Pair<Set<String>, ImageVector>> = listOf(
    setOf("temperature") to Icons.Outlined.Thermostat,
    setOf("humidity", "precipitation", "rainfall") to Icons.Outlined.WaterDrop,
    setOf("uv", "irradiance", "illuminance", "luminous", "sun") to Icons.Outlined.WbSunny,
    setOf("pressure", "speed", "velocity") to Icons.Outlined.Speed,
    setOf("wind", "air") to Icons.Outlined.Air,
    setOf("noise", "sound") to Icons.AutoMirrored.Outlined.VolumeUp,
    setOf("battery") to Icons.Outlined.BatteryFull,
    setOf("voltage") to Icons.Outlined.ElectricBolt,
    setOf("current") to Icons.Outlined.Waves,
    setOf("power", "energy") to Icons.Outlined.Bolt,
    setOf("motion", "occupancy", "people") to Icons.AutoMirrored.Outlined.DirectionsRun,
    setOf("time", "date", "hours", "duration") to Icons.Outlined.Schedule,
    setOf("light", "lamp", "lumen", "luminaire") to Icons.Outlined.Lightbulb,
)

/** Returns an icon matching the quantity the property describes, or a generic one. */
internal fun propertyIconFor(propertyName: String): ImageVector {
    val words = propertyName.lowercase().split(Regex("[^a-z]+")).toSet()
    return propertyIcons.firstOrNull { (keywords, _) -> keywords.any { it in words } }?.second
        ?: Icons.Outlined.SensorOccupied
}
