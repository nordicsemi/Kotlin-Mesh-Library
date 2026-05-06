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
                navigateToWizard = {
                    // Add the wizard to the back stack as the latest screen to be displayed
                    backStack.add(NetworkWizardKey)
                    // Remove the NetworkScreenKey from the backstack as navigating to the wizard
                    // should not allow going back
                    backStack.remove(NetworkScreenKey)
                }
            )
            networkWizardEntry(
                navigateToNetwork = {
                    backStack.add(NetworkScreenKey)
                    backStack.remove(NetworkWizardKey)
                }
            )
        }
    )
}