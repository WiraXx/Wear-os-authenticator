package proton.android.authenticator.wear.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import proton.android.authenticator.shared.wearsync.WearSyncProtocol
import proton.android.authenticator.wear.entriesRepository

/** Receives the entries pushed by the phone, even when the watch app is not running. */
class PhoneMessageService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == WearSyncProtocol.PATH_ENTRIES) {
            entriesRepository.onPayloadReceived(messageEvent.data)
        }
    }

}
