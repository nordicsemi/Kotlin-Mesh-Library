package no.nordicsemi.android.nrfmesh.core.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineDispatcher
import no.nordicsemi.android.nrfmesh.core.common.di.IoDispatcher
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.storage.SceneStatesDataStoreStorage
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager

@InstallIn(ActivityRetainedComponent::class)
@Module
object CoreDataRepositoryModule {

    @ActivityRetainedScoped
    @Provides
    fun provideCoreDataRepository(
        preferences: DataStore<Preferences>,
        centralManager: CentralManager,
        meshNetworkManager: MeshNetworkManager,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        storage: SceneStatesDataStoreStorage,
        lifecycle: ActivityRetainedLifecycle,
    ) = CoreDataRepository(
        preferences = preferences,
        centralManager = centralManager,
        meshNetworkManager = meshNetworkManager,
        storage = storage,
        ioDispatcher = ioDispatcher
    ).also {
        lifecycle.addOnClearedListener { it.close() }
    }
}