package no.nordicsemi.kotlin.mesh.core.model

import kotlin.test.BeforeTest
import kotlin.test.DefaultAsserter.assertNotNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ModelTest {
    private lateinit var node: Node

    @BeforeTest
    fun setUp() {
        node = Node(name = "Test Node", address = 0x0001, elements = 0)
            .apply {
                add(
                    elements = listOf(
                        Element(
                            _models = mutableListOf(
                                Model(modelId = SigModelId(modelIdentifier = Model.CONFIGURATION_SERVER_MODEL_ID)),
                                Model(modelId = SigModelId(modelIdentifier = Model.HEALTH_SERVER_MODEL_ID)),
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_LEVEL_SERVER_MODEL_ID)),
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_ON_OFF_SERVER_MODEL_ID)),
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_DEFAULT_TRANSITION_TIME_SERVER_MODEL_ID)),
                                // Extends Generic OnOff Server model:
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_POWER_ON_OFF_SERVER_MODEL_ID)),
                                // Extends Generic Power OnOff Server model:
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID)),
                                // Extends Generic Power OnOff Server and Generic Level Server models:
                                Model(modelId = SigModelId(modelIdentifier = Model.LIGHT_LIGHTNESS_SERVER_MODEL_ID)),
                                // Extends Light Lightness Server model:
                                Model(modelId = SigModelId(modelIdentifier = Model.LIGHT_LIGHTNESS_SETUP_SERVER_MODEL_ID)),
                            ),
                            location = Location.UNKNOWN
                        ),
                        Element(
                            _models = mutableListOf(
                                // Base model:
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_ON_OFF_SERVER_MODEL_ID)),
                                // Extends Generic OnOff Server on this Element
                                // and Light Lightness Server on Element 0:
                                Model(modelId = SigModelId(modelIdentifier = Model.LIGHT_LC_SERVER_MODEL_ID)),
                                // Extends Light LC Server model:
                                Model(modelId = SigModelId(modelIdentifier = Model.LIGHT_LC_SETUP_SERVER_MODEL_ID)),
                            ),
                            location = Location.UNKNOWN
                        ),
                        Element(
                            _models = mutableListOf(
                                // Base models
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_LEVEL_SERVER_MODEL_ID)),
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_ON_OFF_SERVER_MODEL_ID)),
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_DEFAULT_TRANSITION_TIME_SERVER_MODEL_ID)),
                                // Extends Generic OnOff Server model:
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_POWER_ON_OFF_SERVER_MODEL_ID)),
                                // Extends Generic Power OnOff Server model:
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID)),
                                // Extends Generic Power OnOff Server and Generic Level Server models:
                                Model(modelId = SigModelId(modelIdentifier = Model.LIGHT_LIGHTNESS_SERVER_MODEL_ID)),
                                // Extends Light Lightness Server model:
                                Model(modelId = SigModelId(modelIdentifier = Model.LIGHT_LIGHTNESS_SETUP_SERVER_MODEL_ID)),
                            ),
                            location = Location.UNKNOWN
                        ),
                        Element(
                            _models = mutableListOf(
                                // Base model:
                                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_ON_OFF_SERVER_MODEL_ID)),
                                // Extends Generic OnOff Server on this Element
                                // and Light Lightness Server on Element 0:
                                Model(modelId = SigModelId(modelIdentifier = Model.LIGHT_LC_SERVER_MODEL_ID)),
                                // Extends Light LC Server model:
                                Model(modelId = SigModelId(modelIdentifier = Model.LIGHT_LC_SETUP_SERVER_MODEL_ID))
                            ),
                            location = Location.UNKNOWN
                        )
                    )
                )
            }
    }

    @Test
    fun testConfigServerModel() {
        val configServerModel =
            node.elements[0].model(modelId = Model.CONFIGURATION_SERVER_MODEL_ID.toUInt())
        assertNotNull(message = "Configuration Server model not found", configServerModel)

        val otherModels = node.elements
            .flatMap { it.models }
            .filter { it.modelId.id != Model.CONFIGURATION_SERVER_MODEL_ID.toUInt() }

        assertFalse(actual = otherModels.any { it.extendsDirectly(model = configServerModel!!) })
        assertFalse(actual = otherModels.any { configServerModel!!.extendsDirectly(model = it) })
        assertFalse(actual = otherModels.any { it.extends(model = configServerModel!!) })
        assertFalse(actual = otherModels.any { configServerModel!!.extends(model = it) })
    }

    @Test
    fun testGenericPowerOnOffServerModelId() {
        val powerOnOffSetupServer = node.elements[0]
            .model(modelId = Model.GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID.toUInt())
        assertNotNull(
            message = "Generic Power OnOff Setup Server model not found",
            actual = powerOnOffSetupServer
        )

        val otherModels = node.elements
            .flatMap { it.models }
            .filter { it.modelId.id != Model.GENERIC_POWER_ON_OFF_SETUP_SERVER_MODEL_ID.toUInt() }

        // Generic Power OnOff Setup Server model extends:
        // - Generic Power OnOff Server model
        // - Default Transition Time Server model
        val directBaseModels = otherModels
            .filter { powerOnOffSetupServer!!.extendsDirectly(model = it) }
        assertEquals(expected = 2, actual = directBaseModels.size)

        // Additionally, Power OnOff Server model extends:
        // - Generic OnOff Server
        val baseModels = otherModels
            .filter { powerOnOffSetupServer!!.extends(model = it) }
        assertEquals(expected = 3, actual = baseModels.size)
    }

    @Test
    fun testLightLCServer() {
        val lightLCServer = node.elements[3].model(modelId = Model.LIGHT_LC_SERVER_MODEL_ID.toUInt())
        assertNotNull(message = "Light LC Server model not found", actual = lightLCServer)

        val extendedModels = lightLCServer!!.baseModels
        assertEquals(expected = 5, actual = extendedModels.size)

        val extendingModels = lightLCServer.extendingModels
        assertEquals(expected = 1, actual = extendingModels.size)
    }

    @Test
    fun testLightLightnessServer() {
        val lightLightnessServer = node.elements[0]
            .model(modelId = Model.LIGHT_LIGHTNESS_SERVER_MODEL_ID.toUInt())
        assertNotNull(
            message = "Light Lightness Server model not found",
            actual = lightLightnessServer
        )
        val extendedModels = lightLightnessServer!!.baseModels
        assertEquals(expected = 3, actual = extendedModels.size)

        val extendingModels = lightLightnessServer.extendingModels
        assertEquals(expected = 3, actual = extendingModels.size)
    }
}