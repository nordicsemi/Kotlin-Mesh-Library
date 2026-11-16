package no.nordicsemi.android.nrfmesh.feature.localnode

import no.nordicsemi.android.nrfmesh.core.common.NORDIC_SEMICONDUCTOR_COMPANY_ID
import no.nordicsemi.android.nrfmesh.core.common.VendorModelIds
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Tests which control, if any, is shown for the models of the local node. */
class ControlTypeTest {

    private fun sig(id: UShort) = Model(modelId = SigModelId(modelIdentifier = id))

    @Test
    fun testGenericOnOffModels() {
        assertEquals(
            ControlType.ON_OFF_CLIENT,
            sig(Model.GENERIC_ON_OFF_CLIENT_MODEL_ID).controlType()
        )
        assertEquals(
            ControlType.ON_OFF_SERVER,
            sig(Model.GENERIC_ON_OFF_SERVER_MODEL_ID).controlType()
        )
    }

    @Test
    fun testGenericLevelModels() {
        assertEquals(
            ControlType.LEVEL_CLIENT,
            sig(Model.GENERIC_LEVEL_CLIENT_MODEL_ID).controlType()
        )
        assertEquals(
            ControlType.LEVEL_SERVER,
            sig(Model.GENERIC_LEVEL_SERVER_MODEL_ID).controlType()
        )
    }

    @Test
    fun testOtherSigModelsHaveNoControl() {
        assertNull(sig(Model.SENSOR_CLIENT_MODEL_ID).controlType())
        assertNull(sig(Model.GENERIC_POWER_ON_OFF_CLIENT_MODEL_ID).controlType())
    }

    @Test
    fun testNordicSimpleOnOffClientIsAnOnOffClient() {
        val model = Model(
            modelId = VendorModelId(
                modelIdentifier = VendorModelIds.SIMPLE_ON_OFF_CLIENT_MODEL_ID,
                companyIdentifier = NORDIC_SEMICONDUCTOR_COMPANY_ID
            )
        )
        assertEquals(ControlType.ON_OFF_CLIENT, model.controlType())
    }

    @Test
    fun testVendorModelOfAnotherCompanyHasNoControl() {
        val model = Model(
            modelId = VendorModelId(
                modelIdentifier = VendorModelIds.SIMPLE_ON_OFF_CLIENT_MODEL_ID,
                companyIdentifier = 0x0001u
            )
        )
        assertNull(model.controlType())
    }

    @Test
    fun testOtherNordicVendorModelHasNoControl() {
        val model = Model(
            modelId = VendorModelId(
                modelIdentifier = 0x00FFu,
                companyIdentifier = NORDIC_SEMICONDUCTOR_COMPANY_ID
            )
        )
        assertNull(model.controlType())
    }
}
