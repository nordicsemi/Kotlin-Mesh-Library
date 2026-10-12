package no.nordicsemi.android.nrfmesh.core.common.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.lifecycle.RetainedLifecycle
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.native
import no.nordicsemi.kotlin.ble.core.android.AndroidEnvironment
import no.nordicsemi.kotlin.ble.environment.android.NativeAndroidEnvironment
import javax.inject.Singleton

@Module
@InstallIn(ActivityRetainedComponent::class)
object CentralManagerModule {

    /**
     * Provides the Android environment.
     *
     * The environment is a process wide singleton that only holds receivers registered with the
     * application context, it is therefore not closed together with the central manager.
     */
    @Provides
    @ActivityRetainedScoped
    fun provideNativeAndroidEnvironment(
        @ApplicationContext context: Context,
    ): NativeAndroidEnvironment = NativeAndroidEnvironment.getInstance(
        context = context,
        isNeverForLocationFlagSet = false
    )

    /**
     * Provides the environment as the interface required by the Nordic permissions library.
     */
    @Provides
    fun provideAndroidEnvironment(
        environment: NativeAndroidEnvironment,
    ): AndroidEnvironment = environment

    @Provides
    @ActivityRetainedScoped
    fun provideCentralManager(
        environment: NativeAndroidEnvironment,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        lifecycle: ActivityRetainedLifecycle,
    ) = CentralManager.native(
        environment = environment,
        scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)
    ).also {
        // Close the manager when the lifecycle is destroyed
        // Also clear the coroutine scope
        lifecycle.addOnClearedListener {
            it.close()
            ioDispatcher.cancel()
        }
    }
}