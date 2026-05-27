package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import kotlin.test.Test
import kotlin.test.assertTrue

class GroupTest {

    private val networkManager = MeshNetworkManager(
        storage = TestStorage(),
        secureProperties = TestPropertiesStorage(),
        ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
    )
    private lateinit var meshNetwork: MeshNetwork

    private fun setup() {
        val jsonBytes =
            this.javaClass.classLoader.getResourceAsStream("cdb_json.json")?.readAllBytes()
        runBlocking {
            meshNetwork = networkManager.import(jsonBytes!!)
        }
    }

    @Test
    fun isUsed() {
        setup()
        val parentGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC023u)
        }!!
        assertTrue(parentGroup.isUsed)
    }

    @Test
    fun testIsDirectChildOf() {
        setup()
        val parentGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC023u)
        }!!
        val childGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC024u)
        }!!
        assertTrue(childGroup.isDirectChildOf(parentGroup))
    }

    @Test
    fun testIsDirectParentOf() {
        setup()
        val parentGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC023u)
        }!!
        val childGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC024u)
        }!!
        assertTrue(parentGroup.isDirectParentOf(childGroup))
    }

    @Test
    fun testIsChildOf() {
        setup()
        val parentGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC023u)
        }!!
        val childGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC026u)
        }!!
        assertTrue(childGroup.isChildOf(parentGroup))
    }

    @Test
    fun testIsParentOf() {
        setup()
        val parentGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC023u)
        }!!
        val childGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC026u)
        }!!
        assertTrue(parentGroup.isParentOf(childGroup))
    }

    @Test
    fun testSetAsChildOf() {
        setup()
        val parentGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC023u)
        }!!
        val childGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC103u)
        }!!
        childGroup.setAsChildOf(parentGroup)
        assertTrue(childGroup.isChildOf(parentGroup))
    }

    @Test
    fun testSetAsParentOf() {
        setup()
        val parentGroup = meshNetwork._groups.find {
            it.address is VirtualAddress
        }!!
        val childGroup = meshNetwork._groups.find {
            it.address == GroupAddress(0xC103u)
        }!!
        parentGroup.setAsParentOf(childGroup)
        assertTrue(parentGroup.isParentOf(childGroup))
    }

}