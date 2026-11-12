package no.nordicsemi.android.nrfmesh.core.data

import no.nordicsemi.android.nrfmesh.core.common.Configuration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurationVirtualGroupUuidsTest {

    @Test
    fun testDebugGeneratesIncrementingUuidsFromOne() {
        val uuids = Configuration.Debug(virtualGroups = 3).generateVirtualGroupUuids()

        assertEquals(
            listOf(
                "00000000-0000-0000-0000-000000000001",
                "00000000-0000-0000-0000-000000000002",
                "00000000-0000-0000-0000-000000000003",
            ),
            uuids.map { it.toString() }
        )
    }

    @Test
    fun testDebugUuidsAreStableAcrossCalls() {
        val configuration = Configuration.Debug(virtualGroups = 4)
        assertEquals(
            configuration.generateVirtualGroupUuids(),
            configuration.generateVirtualGroupUuids()
        )
    }

    @Test
    fun testDebugGeneratesNoUuidsWithoutVirtualGroups() {
        assertTrue(Configuration.Debug(virtualGroups = 0).generateVirtualGroupUuids().isEmpty())
    }

    @Test
    fun testDebugUuidsAreUniqueBeyondOneByte() {
        val uuids = Configuration.Debug(virtualGroups = 300).generateVirtualGroupUuids()
        assertEquals(300, uuids.toSet().size)
        assertEquals("00000000-0000-0000-0000-00000000012c", uuids.last().toString())
    }

    @Test
    fun testCustomGeneratesRandomUuids() {
        val uuids = Configuration.Custom(virtualGroups = 3).generateVirtualGroupUuids()
        assertEquals(3, uuids.toSet().size)
    }

    @Test
    fun testEmptyGeneratesNoUuids() {
        assertTrue(Configuration.Empty.generateVirtualGroupUuids().isEmpty())
    }
}
