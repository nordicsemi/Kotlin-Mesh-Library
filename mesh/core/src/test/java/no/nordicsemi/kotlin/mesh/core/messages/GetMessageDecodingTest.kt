package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericBatteryGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCLightOnOffGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCModeGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCOccupancyModeGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessGet
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Get messages carry no parameters. A received Access PDU hands the parameters over as an empty
 * byte array, so the messages must be decoded from an empty array.
 */
class GetMessageDecodingTest {

    private val initializers: Map<String, BaseMeshMessageInitializer> = mapOf(
        "GenericOnOffGet" to GenericOnOffGet.Initializer,
        "GenericLevelGet" to GenericLevelGet.Initializer,
        "GenericBatteryGet" to GenericBatteryGet.Initializer,
        "GenericLocationGlobalGet" to GenericLocationGlobalGet.Initializer,
        "GenericLocationLocalGet" to GenericLocationLocalGet.Initializer,
        "GenericManufacturerPropertiesGet" to GenericManufacturerPropertiesGet.Initializer,
        "GenericAdminPropertiesGet" to GenericAdminPropertiesGet.Initializer,
        "GenericUserPropertiesGet" to GenericUserPropertiesGet.Initializer,
        "LightLCModeGet" to LightLCModeGet.Initializer,
        "LightLCOccupancyModeGet" to LightLCOccupancyModeGet.Initializer,
        "LightLCLightOnOffGet" to LightLCLightOnOffGet.Initializer,
        "LightLightnessGet" to LightLightnessGet.Initializer,
        "LightLightnessDefaultGet" to LightLightnessDefaultGet.Initializer,
    )

    @Test
    fun testGetMessagesAreDecodedFromEmptyParameters() {
        initializers.forEach { (name, initializer) ->
            assertNotNull(initializer.init(byteArrayOf()), "$name was not decoded")
        }
    }

    @Test
    fun testGetMessagesWithParametersAreNotDecoded() {
        initializers.forEach { (name, initializer) ->
            assertNull(initializer.init(byteArrayOf(0x01)), "$name was decoded with parameters")
        }
    }
}
