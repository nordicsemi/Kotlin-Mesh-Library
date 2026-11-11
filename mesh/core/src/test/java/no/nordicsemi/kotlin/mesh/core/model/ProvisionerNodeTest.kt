package no.nordicsemi.kotlin.mesh.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

class ProvisionerNodeTest {

    private fun network() = MeshNetwork(
        name = "Test Network",
        networkKeys = mutableListOf(NetworkKey())
    )

    private fun provisioner(name: String, range: UnicastRange) = Provisioner(
        name = name,
        allocatedUnicastRanges = mutableListOf(range)
    )

    @Test
    fun testPrimaryElementIsCreatedOnEveryAccess() {
        assertNotSame(Element.primaryElement, Element.primaryElement)
    }

    @Test
    fun testPrimaryElementContainsMandatoryModels() {
        val modelIds = Element.primaryElement.models.map { it.modelId.id }
        assertEquals(
            expected = listOf(
                Model.CONFIGURATION_SERVER_MODEL_ID,
                Model.CONFIGURATION_CLIENT_MODEL_ID,
                Model.HEALTH_SERVER_MODEL_ID,
                Model.HEALTH_CLIENT_MODEL_ID
            ).map { it.toInt() },
            actual = modelIds.map { it.toInt() }
        )
    }

    @Test
    fun testNonLocalProvisionersDoNotShareThePrimaryElement() {
        val network = network()
        val local = provisioner("Local", UnicastRange(0x0001, 0x0FFF))
        val second = provisioner("Second", UnicastRange(0x1000, 0x1FFF))
        val third = provisioner("Third", UnicastRange(0x2000, 0x2FFF))
        network.add(provisioner = local, address = UnicastAddress(0x0001u))
        network.add(provisioner = second, address = UnicastAddress(0x1000u))
        network.add(provisioner = third, address = UnicastAddress(0x2000u))

        val secondNode = assertNotNull(second.node)
        val thirdNode = assertNotNull(third.node)
        assertNotSame(secondNode.primaryElement, thirdNode.primaryElement)
        assertSame(secondNode, secondNode.primaryElement.parentNode)
        assertSame(thirdNode, thirdNode.primaryElement.parentNode)
    }

    @Test
    fun testMovingProvisionerToLocalAppliesLocalElements() {
        val network = network()
        val oldLocal = provisioner("Old local", UnicastRange(0x0001, 0x0FFF))
        val newLocal = provisioner("New local", UnicastRange(0x1000, 0x1FFF))
        network.add(provisioner = oldLocal, address = UnicastAddress(0x0001u))
        network.add(provisioner = newLocal, address = UnicastAddress(0x1000u))
        network._localElements = mutableListOf(
            Element(location = Location.MAIN),
            Element(location = Location.MAIN)
        )
        assertEquals(expected = 2, actual = assertNotNull(oldLocal.node).elements.size)
        assertEquals(expected = 1, actual = assertNotNull(newLocal.node).elements.size)

        network.move(provisioner = newLocal, to = 0)

        val newLocalNode = assertNotNull(newLocal.node)
        assertEquals(expected = 2, actual = newLocalNode.elements.size)
        assertEquals(expected = 0x00E0u.toUShort(), actual = newLocalNode.companyIdentifier)
        newLocalNode.elements.forEach { assertSame(newLocalNode, it.parentNode) }

        val oldLocalNode = assertNotNull(oldLocal.node)
        assertEquals(expected = 1, actual = oldLocalNode.elements.size)
        assertNull(oldLocalNode.companyIdentifier)
        // The demoted node must keep owning its primary element.
        assertSame(oldLocalNode, oldLocalNode.primaryElement.parentNode)
        assertEquals(expected = 0, actual = oldLocalNode.primaryElement.index)
    }
}
