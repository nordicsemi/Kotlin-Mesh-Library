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

    @Test
    fun testSigModelIdFromUInt() {
        val modelId = 0x1000u.decode()
        assertIs<SigModelId>(modelId)
        assertEquals(0x1000u.toUShort(), modelId.modelIdentifier)
        assertEquals(0x1000u, modelId.id)
        assertTrue(modelId.isBluetoothSigAssigned)
    }

    @Test
    fun testVendorModelIdFromUInt() {
        val modelId = 0x00590001u.decode()
        assertIs<VendorModelId>(modelId)
        assertEquals(0x0059u.toUShort(), modelId.companyIdentifier)
        assertEquals(0x0001u.toUShort(), modelId.modelIdentifier)
        assertEquals(0x00590001u, modelId.id)
        assertFalse(modelId.isBluetoothSigAssigned)
    }

    @Test
    fun testDecodeFromHexString() {
        assertEquals(SigModelId(0x1001u), "1001".decode())
        assertEquals(VendorModelId(0x00590002u), "00590002".decode())
    }

    @Test(expected = NumberFormatException::class)
    fun testDecodeInvalidHexStringFails() {
        "XYZ".decode()
    }

    @Test
    fun testVendorModelIdFromComponents() {
        val modelId = VendorModelId(modelIdentifier = 0x0001u, companyIdentifier = 0x0059u)
        assertEquals(0x00590001u, modelId.id)
        assertEquals(0x0059u.toUShort(), modelId.companyIdentifier)
        assertEquals(0x0001u.toUShort(), modelId.modelIdentifier)
    }

    @Test
    fun testToHex() {
        assertEquals("1000", SigModelId(0x1000u).toHex())
        assertEquals("0001", SigModelId(0x0001u).toHex())
        assertEquals("ABCD", SigModelId(0xABCDu).toHex())
        assertEquals("00590001", VendorModelId(0x00590001u).toHex())
        assertEquals("FE590001", VendorModelId(0xFE590001u).toHex())
    }

    @Test
    fun testToHexWithPrefix() {
        assertEquals("0x1000", SigModelId(0x1000u).toHex(prefix0x = true))
        assertEquals("0x00590001", VendorModelId(0x00590001u).toHex(prefix0x = true))
    }

    @Test
    fun testSigModelIdEquality() {
        assertEquals(SigModelId(0x1000u), SigModelId(0x1000u))
        assertEquals(SigModelId(0x1000u).hashCode(), SigModelId(0x1000u).hashCode())
        assertNotEquals(SigModelId(0x1000u), SigModelId(0x1001u))
    }

    @Test
    fun testVendorModelIdEquality() {
        assertEquals(VendorModelId(0x00590001u), VendorModelId(0x00590001u))
        assertEquals(VendorModelId(0x00590001u).hashCode(), VendorModelId(0x00590001u).hashCode())
        assertNotEquals(VendorModelId(0x00590001u), VendorModelId(0x00590002u))
        assertNotEquals(VendorModelId(0x00590001u), VendorModelId(0x005A0001u))
    }

    @Test
    fun testSigAndVendorModelIdsAreNotEqual() {
        // Same 16-bit model identifier, but one is vendor defined.
        assertNotEquals<ModelId>(SigModelId(0x0001u), VendorModelId(0x00590001u))
        assertNotEquals<ModelId>(VendorModelId(0x00590001u), SigModelId(0x0001u))
    }

    @Test
    fun testToString() {
        assertEquals("SigModelId(0x1000)", SigModelId(0x1000u).toString())
        assertEquals(
            "VendorModelId(0x0001, companyIdentifier: 0x0059)",
            VendorModelId(0x00590001u).toString()
        )
    }

    @Test
    fun testSerialization() {
        assertEquals("\"1000\"", Json.encodeToString(ModelId.serializer(), SigModelId(0x1000u)))
        assertEquals(
            "\"00590001\"",
            Json.encodeToString(ModelId.serializer(), VendorModelId(0x00590001u))
        )
    }

    @Test
    fun testDeserialization() {
        assertEquals(
            SigModelId(0x1000u),
            Json.decodeFromString(ModelId.serializer(), "\"1000\"")
        )
        assertEquals(
            VendorModelId(0x00590001u),
            Json.decodeFromString(ModelId.serializer(), "\"00590001\"")
        )
    }
}
