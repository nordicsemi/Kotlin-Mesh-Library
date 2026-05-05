package no.nordicsemi.android.nrfmesh.core.data.di

import android.content.Context
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import no.nordicsemi.android.nrfmesh.core.common.di.IoDispatcher
import no.nordicsemi.android.nrfmesh.core.data.storage.ProtoSecurePropertiesMapSerializer
import javax.inject.Singleton

private const val DATA_STORE_FILE_NAME = "secure_properties.pb"

@InstallIn(SingletonComponent::class)
@Module
object MeshNetworkPropertiesStorageModule {

    @Singleton
    @Provides
    fun provideMeshSecurePropertiesStorage(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) = DataStoreFactory.create(
        serializer = ProtoSecurePropertiesMapSerializer,
        produceFile = { context.dataStoreFile(DATA_STORE_FILE_NAME) },
        corruptionHandler = null,
        scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)
    )
}