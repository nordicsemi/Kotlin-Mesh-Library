package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.kotlin.mesh.core.NetworkEvent
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionStatus
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthCurrentStatus
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFault
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultStatus
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthStatusTrackerTest {
    private val tracker = HealthStatusTracker()

    private fun received(source: UShort, message: no.nordicsemi.kotlin.mesh.core.messages.MeshMessage) =
        NetworkEvent.MeshMessageReceived(
            source = source,
            destination = UnicastAddress(address = 0x0001u),
            message = message
        )

    @Test
    fun testCurrentStatusIsStoredBySource() {
        tracker.onEvent(
            received(0x0002u, HealthCurrentStatus(1u, 0x0059u, listOf(HealthFault.OverheatError)))
        )
        val faults = tracker.faults.value.getValue(0x0002u)
        assertEquals(listOf<HealthFault>(HealthFault.OverheatError), faults.faults)
        assertEquals(1u.toUByte(), faults.testId)
        assertTrue(faults.isCurrent)
        assertNull(tracker.faults.value[0x0003u])
    }

    @Test
    fun testFaultStatusReplacesCurrentStatus() {
        tracker.onEvent(
            received(0x0002u, HealthCurrentStatus(1u, 0x0059u, listOf(HealthFault.OverheatError)))
        )
        tracker.onEvent(received(0x0002u, HealthFaultStatus(2u, 0x0059u, emptyList())))
        val faults = tracker.faults.value.getValue(0x0002u)
        assertTrue(faults.faults.isEmpty())
        assertEquals(2u.toUByte(), faults.testId)
        assertFalse(faults.isCurrent)
    }

    @Test
    fun testOtherMessagesAreIgnored() {
        tracker.onEvent(received(0x0002u, HealthAttentionStatus(3u)))
        tracker.onEvent(NetworkEvent.NetworkUpdated)
        assertTrue(tracker.faults.value.isEmpty())
    }

    @Test
    fun testClearForgetsAllFaults() {
        tracker.onEvent(received(0x0002u, HealthFaultStatus(0u, 0x0059u, emptyList())))
        tracker.clear()
        assertTrue(tracker.faults.value.isEmpty())
    }
}
