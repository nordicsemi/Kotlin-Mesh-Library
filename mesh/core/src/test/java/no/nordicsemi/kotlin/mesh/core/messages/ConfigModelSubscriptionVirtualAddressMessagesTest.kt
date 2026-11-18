package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionVirtualAddressAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionVirtualAddressDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionVirtualAddressOverwrite
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

/**
 * Tests that the virtual address subscription messages are decoded with the model they were
 * encoded with. The parameters are the element address (2 bytes), the label UUID (16 bytes), and
 * the model identifier (2 bytes), preceded by the company identifier (2 bytes) for a vendor model.
 */
class ConfigModelSubscriptionVirtualAddressMessagesTest {

    private val label = Uuid.parse("00000000-0000-0000-0000-000000000001")
    private val element = UnicastAddress(address = 0x0001u)

    @Test
    fun testSigModelIsDecodedFromTheEndOfTheParameters() {
        // The model identifier must not be read from inside the label.
        val label = Uuid.parse("11112222-3333-4444-5555-666677778888")
        val parameters = element.address.toLittleEndian() + label.toByteArray() + byteArrayOf(
            0x00, 0x10
        )
        assertEquals(20, parameters.size)

        val add = ConfigModelSubscriptionVirtualAddressAdd.init(parameters)!!
        val delete = ConfigModelSubscriptionVirtualAddressDelete.init(parameters)!!
        val overwrite = ConfigModelSubscriptionVirtualAddressOverwrite.init(parameters)!!

        listOf(
            add.modelIdentifier to add.companyIdentifier,
            delete.modelIdentifier to delete.companyIdentifier,
            overwrite.modelIdentifier to overwrite.companyIdentifier,
        ).forEach { (model, company) ->
            assertEquals(0x1000.toUShort(), model)
            assertNull(company)
        }
    }

    @Test
    fun testVendorModelIsDecoded() {
        val parameters = element.address.toLittleEndian() + label.toByteArray() +
                byteArrayOf(0x59, 0x00, 0x34, 0x12)
        assertEquals(22, parameters.size)

        val add = ConfigModelSubscriptionVirtualAddressAdd.init(parameters)!!
        val delete = ConfigModelSubscriptionVirtualAddressDelete.init(parameters)!!
        val overwrite = ConfigModelSubscriptionVirtualAddressOverwrite.init(parameters)!!

        listOf(
            add.modelIdentifier to add.companyIdentifier,
            delete.modelIdentifier to delete.companyIdentifier,
            overwrite.modelIdentifier to overwrite.companyIdentifier,
        ).forEach { (model, company) ->
            assertEquals(0x1234.toUShort(), model)
            assertEquals(0x0059.toUShort(), company)
        }
    }

    @Test
    fun testEncodedMessagesAreDecodedToTheSameMessage() {
        listOf(null, 0x0059.toUShort()).forEach { company ->
            val add = ConfigModelSubscriptionVirtualAddressAdd(
                virtualLabel = label,
                elementAddress = element,
                modelIdentifier = 0x1000u,
                companyIdentifier = company
            )
            val delete = ConfigModelSubscriptionVirtualAddressDelete(
                virtualLabel = label,
                elementAddress = element,
                modelIdentifier = 0x1000u,
                companyIdentifier = company
            )
            val overwrite = ConfigModelSubscriptionVirtualAddressOverwrite(
                virtualLabel = label,
                elementAddress = element,
                modelIdentifier = 0x1000u,
                companyIdentifier = company
            )

            ConfigModelSubscriptionVirtualAddressAdd.init(add.parameters)!!.let {
                assertContentEquals(add.parameters, it.parameters)
                assertEquals(0x1000.toUShort(), it.modelIdentifier)
                assertEquals(company, it.companyIdentifier)
            }
            ConfigModelSubscriptionVirtualAddressDelete.init(delete.parameters)!!.let {
                assertContentEquals(delete.parameters, it.parameters)
                assertEquals(0x1000.toUShort(), it.modelIdentifier)
                assertEquals(company, it.companyIdentifier)
            }
            ConfigModelSubscriptionVirtualAddressOverwrite.init(overwrite.parameters)!!.let {
                assertContentEquals(overwrite.parameters, it.parameters)
                assertEquals(0x1000.toUShort(), it.modelIdentifier)
                assertEquals(company, it.companyIdentifier)
            }
        }
    }

    @Test
    fun testParametersWithAnInvalidSizeAreRejected() {
        val parameters = element.address.toLittleEndian() + label.toByteArray()
        listOf(parameters, parameters + byteArrayOf(0, 0, 0, 0, 0)).forEach {
            assertNull(ConfigModelSubscriptionVirtualAddressAdd.init(it))
            assertNull(ConfigModelSubscriptionVirtualAddressDelete.init(it))
            assertNull(ConfigModelSubscriptionVirtualAddressOverwrite.init(it))
        }
    }

    private fun UShort.toLittleEndian() = byteArrayOf(
        (toInt() and 0xFF).toByte(),
        ((toInt() shr 8) and 0xFF).toByte()
    )
}
