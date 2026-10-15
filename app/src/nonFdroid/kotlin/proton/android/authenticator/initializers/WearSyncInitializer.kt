package proton.android.authenticator.initializers

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import proton.android.authenticator.app.wear.WearSyncManager

internal class WearSyncInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WearSyncInitializerDependencies::class.java
        ).getWearSyncManager().start()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    @[EntryPoint InstallIn(SingletonComponent::class)]
    internal interface WearSyncInitializerDependencies {

        fun getWearSyncManager(): WearSyncManager

    }

}
