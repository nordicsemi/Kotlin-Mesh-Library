package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.mesh.core.model.MasterSecurity
import no.nordicsemi.kotlin.mesh.core.model.Publish
import no.nordicsemi.kotlin.mesh.core.model.PublishPeriod
import no.nordicsemi.kotlin.mesh.core.model.Retransmit
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class ConfigModelPublicationVirtualAddressTest {

    private val label = Uuid.parse("0073e7e4-d8b9-440f-af84-15df4c56c0e1")

    private fun virtualSet(modelIdentifier: UShort, companyIdentifier: UShort? = null) =
        ConfigModelPublicationVirtualAddressSet(
            elementAddress = UnicastAddress(address = 0x0017u),
            companyIdentifier = companyIdentifier,
            modelIdentifier = modelIdentifier,
            publish = Publish(
                address = VirtualAddress(uuid = label),
                index = 3u,
                ttl = 0xFFu,
                period = PublishPeriod.disabled,
                credentials = MasterSecurity,
                retransmit = Retransmit.disabled
            )
        )

    @Test
    fun testSigModelIdentifierIsLittleEndian() {
        val params = virtualSet(modelIdentifier = 0x1000u).parameters
        assertEquals(25, params.size)
        // Model Identifier 0x1000 is sent as 00 10.
        assertContentEquals(byteArrayOf(0x00, 0x10), params.copyOfRange(23, 25))
    }

    @Test
    fun testVendorModelIdentifiersAreLittleEndian() {
        val params = virtualSet(modelIdentifier = 0x0002u, companyIdentifier = 0x0059u).parameters
        assertEquals(27, params.size)
        assertContentEquals(byteArrayOf(0x59, 0x00, 0x02, 0x00), params.copyOfRange(23, 27))
    }

    @Test
    fun testVirtualSetRoundTrip() {
        val message = virtualSet(modelIdentifier = 0x1000u)
        val parsed = assertIs<ConfigModelPublicationVirtualAddressSet>(
            ConfigModelPublicationVirtualAddressSet.init(message.parameters)
        )
        assertEquals(0x1000u.toUShort(), parsed.modelIdentifier)
        assertEquals(message.publish.address, parsed.publish.address)
    }

    /** Status of a publication with the virtual address hash 0xA02B for model 0x0002. */
    private val virtualStatusParameters = byteArrayOf(
        0x00,                   // Success
        0x17, 0x00,             // Element address
        0x2B, 0xA0.toByte(),    // Virtual address hash 0xA02B
        0x03, 0x00,             // Application key index
        0xFF.toByte(),          // TTL
        0x00, 0x00,             // Period, Retransmit
        0x02, 0x00              // Model identifier
    )

    @Test
    fun testStatusWithVirtualAddressHashIsParsed() {
        val status = assertIs<ConfigModelPublicationStatus>(
            ConfigModelPublicationStatus.init(virtualStatusParameters)
        )
        assertEquals(0xA02Bu.toUShort(), status.virtualAddressHash)
        assertIs<VirtualAddress>(status.publish.address)
        assertEquals(3u.toUShort(), status.publish.index)
        assertEquals(0x0002u.toUShort(), status.modelIdentifier)
    }

    @Test
    fun testStatusWithGroupAddressHasNoVirtualAddressHash() {
        val params = virtualStatusParameters.copyOf().also {
            it[3] = 0x00
            it[4] = 0xC0.toByte() // 0xC000
        }
        val status = assertIs<ConfigModelPublicationStatus>(
            ConfigModelPublicationStatus.init(params)
        )
        assertNull(status.virtualAddressHash)
    }
}
