package proton.android.authenticator.wear.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import proton.android.authenticator.shared.wearsync.WearEntriesPayload
import proton.android.authenticator.shared.wearsync.WearSyncCodec

/** Latest entries received from the phone, persisted encrypted so the watch works offline. */
class EntriesRepository internal constructor(context: Context) {

    private val store = SecureEntriesStore(context)

    private val mutablePayload = MutableStateFlow(store.read()?.let(WearSyncCodec::decode))

    val payload: StateFlow<WearEntriesPayload?> = mutablePayload.asStateFlow()

    private val mutableReceivedCount = MutableStateFlow(0)

    /** Increments on every payload received, even an identical one, to detect a phone answer. */
    val receivedCount: StateFlow<Int> = mutableReceivedCount.asStateFlow()

    fun onPayloadReceived(bytes: ByteArray) {
        val decoded = WearSyncCodec.decode(bytes) ?: return

        mutablePayload.value = decoded
        mutableReceivedCount.update { count -> count + 1 }

        try {
            store.write(bytes)
        } catch (_: Exception) {
            // Keystore or disk failure: keep serving the in-memory copy, the phone resends on next launch.
        }
    }

}
