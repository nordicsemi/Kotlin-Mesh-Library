package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CompositionDataPage0Test {

    // Page 0, CID 0x0059, PID 0, VID 0, CRPL 10, Features 0x0007.
    private val header = byteArrayOf(
        0x00, 0x59, 0x00, 0x00, 0x00, 0x00, 0x00, 0x0A, 0x00, 0x07, 0x00
    )

    private fun ints(vararg bytes: Int) = ByteArray(bytes.size) { bytes[it].toByte() }

    @Test
    fun testParseWithoutElements() {
        val page = assertNotNull(Page0.init(header))
        assertEquals(0x0059u.toUShort(), page.companyIdentifier)
        assertEquals(0, page.elements.size)
    }

    @Test
    fun testParseWithElement() {
        // Location 0x0001, 1 SIG model (0x1000), 0 vendor models.
        val page = assertNotNull(Page0.init(header + ints(0x01, 0x00, 0x01, 0x00, 0x00, 0x10)))
        assertEquals(1, page.elements.size)
    }

    @Test
    fun testParseRejectsTruncatedElementHeader() {
        assertNull(Page0.init(header + ints(0x01, 0x00)))
    }

    @Test
    fun testParseRejectsTruncatedModelList() {
        // The element claims 2 SIG models but carries only 1.
        assertNull(Page0.init(header + ints(0x01, 0x00, 0x02, 0x00, 0x00, 0x10)))
    }
}
