package no.nordicsemi.kotlin.mesh.core.model

import no.nordicsemi.kotlin.mesh.core.model.ModelId.Companion.decode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class ModelIdTest {

    @Test
    fun testVendorModelIdEqualsItself() {
        val vendor = VendorModelId(companyIdentifier = 0x0059u, modelIdentifier = 0x0001u)
        assertEquals(expected = vendor, actual = vendor)
    }

    @Test
    fun testVendorModelIdEqualsEqualValuedCopy() {
        val vendor = VendorModelId(companyIdentifier = 0x0059u, modelIdentifier = 0x0001u)
        val copy = VendorModelId(id = 0x0059_0001u)
        assertEquals(expected = vendor, actual = copy)
        assertEquals(expected = vendor.hashCode(), actual = copy.hashCode())
        assertEquals(expected = setOf(vendor), actual = setOf(copy))
    }

    @Test
    fun testVendorModelIdDoesNotEqualSigModelIdWithSameModelIdentifier() {
        val vendor = VendorModelId(modelIdentifier = 0x0001u, companyIdentifier = 0x0059u)
        val sig = SigModelId(modelIdentifier = 0x0001u)
        assertNotEquals<ModelId>(illegal = sig, actual = vendor)
        assertNotEquals<ModelId>(illegal = vendor, actual = sig)
    }

    @Test
    fun testVendorModelIdWithCompanyZeroDoesNotEqualSigModelId() {
        val vendor = VendorModelId(modelIdentifier = 0x1234u, companyIdentifier = 0x0000u)
        val sig = SigModelId(modelIdentifier = 0x1234u)
        assertNotEquals<ModelId>(illegal = sig, actual = vendor)
        assertNotEquals<ModelId>(illegal = vendor, actual = sig)
    }

    @Test
    fun testToHexOfVendorModelIdWithCompanyZero() {
        val vendor = VendorModelId(modelIdentifier = 0x1234u, companyIdentifier = 0x0000u)
        assertEquals(expected = "00001234", actual = vendor.toHex())
    }

    @Test
    fun testDecodeVendorModelIdWithCompanyZero() {
        val decoded = "00001234".decode() // 0x0000 company ID: Ericsson AB
        assertIs<VendorModelId>(value = decoded)
        assertEquals(expected = 0x0000u, actual = decoded.companyIdentifier)
        assertEquals(expected = 0x1234u, actual = decoded.modelIdentifier)
    }
}
