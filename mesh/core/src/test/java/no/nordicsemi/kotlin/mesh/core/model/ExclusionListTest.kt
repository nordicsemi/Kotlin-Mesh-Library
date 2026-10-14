package no.nordicsemi.kotlin.mesh.core.model

import kotlin.uuid.Uuid
import kotlin.test.Test
import kotlin.test.assertEquals

class ExclusionListTest {

    private val elements = arrayListOf(
        Element(
            location = Location.UNKNOWN,
            _models = mutableListOf(
                Model(SigModelId(Model.CONFIGURATION_SERVER_MODEL_ID)),
                Model(SigModelId(Model.CONFIGURATION_CLIENT_MODEL_ID))
            )
        ),
        Element(
            location = Location.UNKNOWN,
            _models = mutableListOf(
                Model(SigModelId(Model.CONFIGURATION_SERVER_MODEL_ID)),
                Model(SigModelId(Model.CONFIGURATION_CLIENT_MODEL_ID))
            )
        ),
        Element(
            location = Location.UNKNOWN,
            _models = mutableListOf(
                Model(SigModelId(Model.CONFIGURATION_SERVER_MODEL_ID)),
                Model(SigModelId(Model.CONFIGURATION_CLIENT_MODEL_ID))
            )
        )
    )

    private val node = Node(
        uuid = Uuid.random(),
        deviceKey = byteArrayOf(),
        _primaryUnicastAddress = UnicastAddress(address = 1u),
        _elements = elements,
        _netKeys = mutableListOf(),
        _appKeys = mutableListOf()
    ).apply {
        name = "Node"
    }

    @Test
    fun testExcludeUnicast() {
        val exclusionList = ExclusionList(ivIndex = 1u)
        exclusionList.exclude(address = UnicastAddress(address = 1u))
        assertEquals(UnicastAddress(address = 1u), exclusionList._addresses[0])
    }

    @Test
    fun testExcludeNode() {
        val exclusionList = ExclusionList(ivIndex = 1u)
        exclusionList.exclude(node = node)
        assertEquals(UnicastAddress(address = 1u), exclusionList._addresses[0])
        assertEquals(UnicastAddress(address = 2u), exclusionList._addresses[1])
        assertEquals(UnicastAddress(address = 3u), exclusionList._addresses[2])
    }

    @Test
    fun testIsExcluded() {
        val exclusionList = ExclusionList(ivIndex = 1u)
        val expected = exclusionList.exclude(address = UnicastAddress(address = 1u))
        assertEquals(
            expected,
            exclusionList.isExcluded(address = UnicastAddress(address = 1u))
        )
    }
}