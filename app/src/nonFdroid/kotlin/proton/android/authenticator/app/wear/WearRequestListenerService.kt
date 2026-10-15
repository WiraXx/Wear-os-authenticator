package proton.android.authenticator.app.wear

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import proton.android.authenticator.shared.wearsync.WearSyncProtocol

/** Answers the watch's "send me the entries" requests, even when the app is not running. */
class WearRequestListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WearSyncProtocol.PATH_REQUEST) return

        EntryPointAccessors.fromApplication(
            applicationContext,
            WearRequestListenerServiceDependencies::class.java
        ).getWearSyncManager().onWatchRequest(messageEvent.sourceNodeId)
    }

    @[EntryPoint InstallIn(SingletonComponent::class)]
    internal interface WearRequestListenerServiceDependencies {

        fun getWearSyncManager(): WearSyncManager

    }

}
