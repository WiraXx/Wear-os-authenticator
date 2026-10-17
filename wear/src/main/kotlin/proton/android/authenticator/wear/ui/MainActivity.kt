package proton.android.authenticator.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
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
            syncStatus.value = when (val result = PhoneSync.requestSync(applicationContext)) {
                PhoneSyncResult.Requested -> SyncStatus.Idle
                PhoneSyncResult.NoConnectedDevice -> SyncStatus.NoConnectedDevice
                PhoneSyncResult.PhoneAppMissing -> SyncStatus.PhoneAppMissing
                is PhoneSyncResult.Failed -> SyncStatus.Failed(result.code)
            }
        }
    }

}
