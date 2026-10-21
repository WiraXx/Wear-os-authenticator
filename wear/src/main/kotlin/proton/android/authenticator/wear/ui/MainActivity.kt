package proton.android.authenticator.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import proton.android.authenticator.wear.entriesRepository
import proton.android.authenticator.wear.sync.PhoneSync
import proton.android.authenticator.wear.sync.PhoneSyncResult

class MainActivity : ComponentActivity() {

    private val syncStatus = mutableStateOf<SyncStatus>(SyncStatus.Idle)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val payload by entriesRepository.payload.collectAsState()

            AuthenticatorScreen(
                payload = payload,
                syncStatus = syncStatus.value,
                onSync = ::requestSync
            )
        }
    }

    override fun onStart() {
        super.onStart()

        requestSync()
    }

    private fun requestSync() {
        lifecycleScope.launch {
            syncStatus.value = SyncStatus.Syncing

            val receivedCount = entriesRepository.receivedCount
            val countBefore = receivedCount.value

            syncStatus.value = when (val result = PhoneSync.requestSync(applicationContext)) {
                is PhoneSyncResult.Requested -> {
                    val hasReply = withTimeoutOrNull(REPLY_TIMEOUT_MILLIS) {
                        receivedCount.first { count -> count > countBefore }
                    } != null

                    if (hasReply || result.viaCapability) {
                        SyncStatus.Idle
                    } else {
                        SyncStatus.PhoneAppMissing(devices = result.devices)
                    }
                }

                PhoneSyncResult.NoConnectedDevice -> SyncStatus.NoConnectedDevice
                is PhoneSyncResult.Failed -> SyncStatus.Failed(result.code)
            }
        }
    }

    private companion object {

        private const val REPLY_TIMEOUT_MILLIS = 8_000L

    }

}
