package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.json.Json
import no.nordicsemi.kotlin.mesh.core.model.ModelId.Companion.decode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ModelIdTest {

    private companion object {
        /** Nordic Semiconductor company identifier. */
        const val NORDIC: UShort = 0x0059u
    }

    private fun ModelId.toJson() = Json.encodeToString(ModelId.serializer(), this)
    private fun String.fromJson() = Json.decodeFromString(ModelId.serializer(), this)

    private fun assertSigModelId(modelIdentifier: UShort, hex: String) {
        val modelId = SigModelId(modelIdentifier)
        assertTrue(modelId.isBluetoothSigAssigned)
        assertEquals(modelIdentifier.toUInt(), modelId.id)
        assertEquals(hex, modelId.toHex())
        assertEquals("0x$hex", modelId.toHex(prefix0x = true))
        assertEquals("\"$hex\"", modelId.toJson())
        assertEquals(modelId, hex.decode())
        assertEquals(modelId, "\"$hex\"".fromJson())
    }

    private fun assertVendorModelId(companyIdentifier: UShort, modelIdentifier: UShort, hex: String) {
        val modelId = VendorModelId(modelIdentifier, companyIdentifier)
        assertFalse(modelId.isBluetoothSigAssigned)
        assertEquals(companyIdentifier, modelId.companyIdentifier)
        assertEquals(modelIdentifier, modelId.modelIdentifier)
        assertEquals(hex, modelId.toHex())
        assertEquals("0x$hex", modelId.toHex(prefix0x = true))
        assertEquals("\"$hex\"", modelId.toJson())
        assertEquals(modelId, hex.decode())
        assertEquals(modelId, "\"$hex\"".fromJson())
    }

    // region Bluetooth SIG models

    @Test
    fun foundationModels() {
        assertSigModelId(Model.CONFIGURATION_SERVER_MODEL_ID, "0000")
        assertSigModelId(Model.CONFIGURATION_CLIENT_MODEL_ID, "0001")
        assertSigModelId(Model.HEALTH_SERVER_MODEL_ID, "0002")
        assertSigModelId(Model.HEALTH_CLIENT_MODEL_ID, "0003")
        assertSigModelId(Model.REMOTE_PROVISIONING_SERVER_MODEL_ID, "0004")
        assertSigModelId(Model.PRIVATE_BEACON_SERVER_MODEL_ID, "000A")
        assertSigModelId(Model.SAR_CONFIGURATION_CLIENT_MODEL_ID, "000F")
        assertSigModelId(Model.SOLICITATION_PDU_RPL_CONFIGURATION_CLIENT_MODEL_ID, "0015")
    }

    @Test
    fun genericModels() {
        assertSigModelId(Model.GENERIC_ON_OFF_SERVER_MODEL_ID, "1000")
        assertSigModelId(Model.GENERIC_ON_OFF_CLIENT_MODEL_ID, "1001")
        assertSigModelId(Model.GENERIC_LEVEL_SERVER_MODEL_ID, "1002")
        assertSigModelId(Model.GENERIC_POWER_LEVEL_SETUP_SERVER_MODEL_ID, "100A")
        assertSigModelId(Model.GENERIC_PROPERTY_CLIENT_MODEL_ID, "1015")
    }

    @Test
    fun sensorModels() {
        assertSigModelId(Model.SENSOR_SERVER_MODEL_ID, "1100")
        assertSigModelId(Model.SENSOR_SETUP_SERVER_MODEL_ID, "1101")
        assertSigModelId(Model.SENSOR_CLIENT_MODEL_ID, "1102")
    }

    @Test
    fun timeAndSceneModels() {
        assertSigModelId(Model.TIME_SERVER_MODEL_ID, "1200")
        assertSigModelId(Model.SCENE_SERVER_MODEL_ID, "1203")
        assertSigModelId(Model.SCENE_CLIENT_MODEL_ID, "1205")
        assertSigModelId(Model.SCHEDULER_CLIENT_MODEL_ID, "1208")
    }

    @Test
    fun lightingModels() {
        assertSigModelId(Model.LIGHT_LIGHTNESS_SERVER_MODEL_ID, "1300")
        assertSigModelId(Model.LIGHT_CTL_SERVER_MODEL_ID, "1303")
        assertSigModelId(Model.LIGHT_HSL_HUE_SERVER_MODEL_ID, "130A")
        assertSigModelId(Model.LIGHT_LC_CLIENT_MODEL_ID, "1311")
    }

    @Test
    fun blobTransferAndFirmwareModels() {
        assertSigModelId(Model.BLOB_TRANSFER_SERVER_MODEL_ID, "1400")
        assertSigModelId(Model.BLOB_TRANSFER_CLIENT_MODEL_ID, "1401")
        assertSigModelId(Model.FIRMWARE_UPDATE_SERVER_MODEL_ID, "1402")
        assertSigModelId(Model.FIRMWARE_DISTRIBUTION_CLIENT_MODEL_ID, "1405")
    }

    @Test
    fun sigModelIdWithMaxValue() {
        assertSigModelId(0xFFFFu, "FFFF")
    }

    @Test
    fun sigModelIdFromUInt() {
        val modelId = 0x1000u.decode()
        assertIs<SigModelId>(modelId)
        assertEquals(Model.GENERIC_ON_OFF_SERVER_MODEL_ID, modelId.modelIdentifier)
    }

    // endregion

    // region Vendor models

    @Test
    fun nordicSimpleOnOffModels() {
        assertVendorModelId(NORDIC, 0x0000u, "00590000")
        assertVendorModelId(NORDIC, 0x0001u, "00590001")
    }

    @Test
    fun nordicRssiModels() {
        assertVendorModelId(NORDIC, 0x0005u, "00590005")
        assertVendorModelId(NORDIC, 0x0006u, "00590006")
        assertVendorModelId(NORDIC, 0x0007u, "00590007")
    }

    @Test
    fun nordicThingy52Models() {
        assertVendorModelId(NORDIC, 0x0008u, "00590008")
        assertVendorModelId(NORDIC, 0x0009u, "00590009")
    }

    @Test
    fun nordicChatAndDistanceMeasurementModels() {
        assertVendorModelId(NORDIC, 0x000Au, "0059000A")
        assertVendorModelId(NORDIC, 0x000Bu, "0059000B")
        assertVendorModelId(NORDIC, 0x000Cu, "0059000C")
    }

    @Test
    fun nordicLePairingModels() {
        assertVendorModelId(NORDIC, 0x000Du, "0059000D")
        assertVendorModelId(NORDIC, 0x000Eu, "0059000E")
    }

    @Test
    fun vendorModelIdWithZeroCompanyIdentifier() {
        assertVendorModelId(0x0000u, 0x1234u, "00001234")
        // The 8 digit length is what tells it apart from a SIG model.
        assertIs<VendorModelId>("00001234".decode())
    }

    @Test
    fun vendorModelIdWithHexLettersInBothIdentifiers() {
        assertVendorModelId(0xFE59u, 0xABCDu, "FE59ABCD")
        assertEquals(VendorModelId(0xFE59ABCDu), "fe59abcd".decode())
    }

    @Test
    fun vendorModelIdWithMaxValue() {
        assertVendorModelId(0xFFFFu, 0xFFFFu, "FFFFFFFF")
    }

    @Test
    fun vendorModelIdFromComponents() {
        val modelId = VendorModelId(modelIdentifier = 0x0001u, companyIdentifier = 0x0059u)
        assertEquals(VendorModelId(0x00590001u), modelId)
        assertEquals(0x00590001u, modelId.id)
    }

    @Test
    fun vendorModelIdFromUInt() {
        val modelId = 0x00590001u.decode()
        assertIs<VendorModelId>(modelId)
        assertEquals(NORDIC, modelId.companyIdentifier)
        assertEquals(0x0001u.toUShort(), modelId.modelIdentifier)
    }

    // endregion

    // region Decoding

    @Test
    fun decodeAcceptsZeroXPrefix() {
        assertEquals(SigModelId(0x1000u), "0x1000".decode())
        assertEquals(SigModelId(0xABCDu), "0XABCD".decode())
        assertEquals(VendorModelId(0x00590001u), "0x00590001".decode())
    }

    @Test
    fun decodeRoundTripsToHexWithPrefix() {
        listOf(SigModelId(0xABCDu), VendorModelId(0x00001234u), VendorModelId(0xFE590001u))
            .forEach { assertEquals(it, it.toHex(prefix0x = true).decode()) }
    }

    @Test(expected = NumberFormatException::class)
    fun decodeFailsForNonHexDigits() {
        val _ = "ZZZZ".decode()
    }

    @Test(expected = NumberFormatException::class)
    fun decodeFailsForLengthOtherThanFourOrEight() {
        val _ = "123456".decode()
    }

    // endregion

    // region Equality

    @Test
    fun sigModelIdsAreEqualWhenIdentifiersMatch() {
        assertEquals(SigModelId(0x1000u), SigModelId(0x1000u))
        assertEquals(SigModelId(0x1000u).hashCode(), SigModelId(0x1000u).hashCode())
        assertNotEquals(SigModelId(0x1000u), SigModelId(0x1001u))
    }

    @Test
    fun vendorModelIdsAreEqualWhenModelAndCompanyMatch() {
        assertEquals(VendorModelId(0x00590001u), VendorModelId(0x00590001u))
        assertEquals(VendorModelId(0x00590001u).hashCode(), VendorModelId(0x00590001u).hashCode())
        assertNotEquals(VendorModelId(0x00590001u), VendorModelId(0x00590002u))
        assertNotEquals(VendorModelId(0x00590001u), VendorModelId(0x005A0001u))
    }

    @Test
    fun sigAndVendorModelIdsWithSameModelIdentifierAreNotEqual() {
        assertNotEquals<ModelId>(SigModelId(0x0001u), VendorModelId(0x00590001u))
        assertNotEquals<ModelId>(VendorModelId(0x00590001u), SigModelId(0x0001u))
        assertNotEquals<ModelId>(SigModelId(0x1234u), VendorModelId(0x00001234u))
    }

    // endregion

    // region toString

    @Test
    fun sigModelIdToString() {
        assertEquals("SigModelId(0x1000)", SigModelId(0x1000u).toString())
    }

    @Test
    fun vendorModelIdToString() {
        assertEquals(
            "VendorModelId(0x0001, companyIdentifier: 0x0059)",
            VendorModelId(0x00590001u).toString()
        )
    }

    // endregion
}
