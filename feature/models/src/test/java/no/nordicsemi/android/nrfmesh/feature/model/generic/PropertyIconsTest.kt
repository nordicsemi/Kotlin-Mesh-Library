package no.nordicsemi.android.nrfmesh.feature.model.generic

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.SensorOccupied
import androidx.compose.material.icons.outlined.WbSunny
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PropertyIconsTest {

    @Test
    fun testIconMatchesTheQuantityOfTheProperty() {
        assertEquals(Icons.Outlined.Thermostat, propertyIconFor("Present Ambient Temperature"))
        assertEquals(Icons.Outlined.WbSunny, propertyIconFor("UV Index"))
    }

    @Test
    fun testVoltageAndCurrentHaveDifferentIcons() {
        assertNotEquals(
            propertyIconFor("Present Input Voltage"),
            propertyIconFor("Present Input Current")
        )
    }

    @Test
    fun testUnknownPropertyUsesTheGenericIcon() {
        assertEquals(Icons.Outlined.SensorOccupied, propertyIconFor("0x1234"))
        assertEquals(Icons.Outlined.SensorOccupied, propertyIconFor("Device Firmware Revision"))
    }
}
