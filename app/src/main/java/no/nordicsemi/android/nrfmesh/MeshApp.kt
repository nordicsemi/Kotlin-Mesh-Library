package no.nordicsemi.android.nrfmesh

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import no.nordicsemi.android.nrfmesh.network.navigation.NetworkScreenKey
import no.nordicsemi.android.nrfmesh.network.navigation.networkScreenEntry
import no.nordicsemi.android.nrfmesh.network.wizard.navigation.NetworkWizardKey
import no.nordicsemi.android.nrfmesh.network.wizard.navigation.networkWizardEntry

@Composable
fun MeshApp() {
    val backStack = rememberNavBackStack(NetworkScreenKey)
    NavDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            networkScreenEntry(
                // Replaces the whole back stack, so going back from the wizard is not possible. This
                // can be called more than once, so it must not add duplicates.
                navigateToWizard = {
                    backStack.clear()
                    backStack.add(NetworkWizardKey)
                }
            )
            networkWizardEntry(
                navigateToNetwork = {
                    backStack.clear()
                    backStack.add(NetworkScreenKey)
                }
            )
        }
    )
}