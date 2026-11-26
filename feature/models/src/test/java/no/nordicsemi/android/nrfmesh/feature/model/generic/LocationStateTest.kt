package no.nordicsemi.android.nrfmesh.feature.model.generic

import no.nordicsemi.kotlin.mesh.core.messages.Altitude
import no.nordicsemi.kotlin.mesh.core.messages.FloorNumber
import no.nordicsemi.kotlin.mesh.core.messages.Latitude
import no.nordicsemi.kotlin.mesh.core.messages.LocationUncertainty
import no.nordicsemi.kotlin.mesh.core.messages.Longitude
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalStatus
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LocationStateTest {

    private val latitude = assertNotNull(Latitude.fromDegrees(59.9139))
    private val longitude = assertNotNull(Longitude.fromDegrees(10.7522))
    private val altitude = Altitude(23)
    private val global = GenericLocationGlobalStatus(latitude, longitude, altitude)
    private val local = GenericLocationLocalStatus(
        north = 1,
        east = 2,
        altitude = 3,
        floor = FloorNumber.GROUND_FLOOR_0,
        uncertainty = LocationUncertainty.of(isMobile = false)
    )

    private fun state() = LocationState().also {
        it.global = global
        it.local = local
    }

    @Test
    fun testAnUnacknowledgedGlobalSetForgetsTheGlobalLocationOnly() {
        val state = state()
        state.forgetAfter(GenericLocationGlobalSetUnacknowledged(latitude, longitude, altitude))
        assertNull(state.global)
        assertNotNull(state.local)
    }

    @Test
    fun testAnUnacknowledgedLocalSetForgetsTheLocalLocationOnly() {
        val state = state()
        state.forgetAfter(
            GenericLocationLocalSetUnacknowledged(
                north = 1,
                east = 2,
                altitude = 3,
                floor = FloorNumber.GROUND_FLOOR_0,
                uncertainty = LocationUncertainty.of(isMobile = false)
            )
        )
        assertNotNull(state.global)
        assertNull(state.local)
    }

    @Test
    fun testAnAcknowledgedSetKeepsTheLocationUntilItsResponseArrives() {
        val state = state()
        state.forgetAfter(GenericLocationGlobalSet(latitude, longitude, altitude))
        state.forgetAfter(
            GenericLocationLocalSet(
                north = 1,
                east = 2,
                altitude = 3,
                floor = FloorNumber.GROUND_FLOOR_0,
                uncertainty = LocationUncertainty.of(isMobile = false)
            )
        )
        assertNotNull(state.global)
        assertNotNull(state.local)
    }

    @Test
    fun testOtherMessagesKeepTheLocation() {
        val state = state()
        state.forgetAfter(GenericLocationGlobalGet())
        state.forgetAfter(null)
        assertNotNull(state.global)
        assertNotNull(state.local)
    }
}
