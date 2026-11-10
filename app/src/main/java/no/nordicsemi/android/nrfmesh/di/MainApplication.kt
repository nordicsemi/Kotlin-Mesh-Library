package no.nordicsemi.android.nrfmesh.di

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import no.nordicsemi.android.nrfmesh.core.data.bearer.sniffer.Sniffer

@HiltAndroidApp
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Starts feeding the nRF Mesh Sniffer app, when it is installed.
        Sniffer.install(this)
    }
}
