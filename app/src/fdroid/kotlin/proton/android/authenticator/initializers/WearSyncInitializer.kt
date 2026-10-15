package proton.android.authenticator.initializers

import android.content.Context
import androidx.startup.Initializer

internal class WearSyncInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // fdroid no-op: the Wearable Data Layer requires Google Play Services
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

}
