package proton.android.authenticator.wear.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import proton.android.authenticator.shared.wearsync.WearEntriesPayload
import proton.android.authenticator.shared.wearsync.WearSyncCodec

/** Latest entries received from the phone, persisted encrypted so the watch works offline. */
class EntriesRepository internal constructor(context: Context) {

    private val store = SecureEntriesStore(context)

    private val mutablePayload = MutableStateFlow(store.read()?.let(WearSyncCodec::decode))

    val payload: StateFlow<WearEntriesPayload?> = mutablePayload.asStateFlow()

    fun onPayloadReceived(bytes: ByteArray) {
        val decoded = WearSyncCodec.decode(bytes) ?: return

        mutablePayload.value = decoded

        try {
            store.write(bytes)
        } catch (_: Exception) {
            // Keystore or disk failure: keep serving the in-memory copy, the phone resends on next launch.
        }
    }

}
