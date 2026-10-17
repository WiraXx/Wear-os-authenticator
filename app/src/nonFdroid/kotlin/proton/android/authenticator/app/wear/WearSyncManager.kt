package proton.android.authenticator.app.wear

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import proton.android.authenticator.business.entries.domain.EntryAlgorithm
import proton.android.authenticator.business.entries.domain.EntryType
import proton.android.authenticator.features.shared.entries.presentation.EntryModel
import proton.android.authenticator.features.shared.entries.usecases.ObserveEntryModelsUseCase
import proton.android.authenticator.features.shared.users.usecases.ObserveUserUseCase
import proton.android.authenticator.shared.wearsync.WearEntriesPayload
import proton.android.authenticator.shared.wearsync.WearEntry
import proton.android.authenticator.shared.wearsync.WearEntryAlgorithm
import proton.android.authenticator.shared.wearsync.WearEntryType
import proton.android.authenticator.shared.wearsync.WearSyncCodec
import proton.android.authenticator.shared.wearsync.WearSyncProtocol
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps every paired Wear OS watch running this app in sync with the phone's entries and account.
 *
 * Pushes the decrypted entries to the watch (Data Layer messages, transient) whenever they change,
 * whenever a watch (re)connects, and whenever the watch asks for them.
 */
@Singleton
internal class WearSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val observeEntryModels: ObserveEntryModelsUseCase,
    private val observeUser: ObserveUserUseCase
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val isStarted = AtomicBoolean(false)

    private val latestPayload = MutableStateFlow<ByteArray?>(null)

    fun start() {
        if (!isStarted.compareAndSet(false, true)) return

        scope.launch {
            observePayloads()
                .catch { error -> Timber.w(error, "Wear OS sync stopped") }
                .collect { payload ->
                    latestPayload.value = payload
                    pushToWatches(payload)
                }
        }

        scope.launch {
            observeWatches().collect { nodes ->
                val payload = latestPayload.value ?: return@collect
                nodes.forEach { node -> send(node.id, payload) }
            }
        }
    }

    fun onWatchRequest(nodeId: String) {
        start()

        scope.launch {
            withTimeoutOrNull(REQUEST_TIMEOUT_MILLIS) { latestPayload.filterNotNull().first() }
                ?.let { payload -> send(nodeId, payload) }
        }
    }

    private fun observePayloads(): Flow<ByteArray> = combine(
        observeEntryModels(),
        observeUser()
            .map { user -> user?.displayName?.takeIf(String::isNotBlank) }
            .catch { emit(null) }
            .onStart { emit(null) }
            .distinctUntilChanged(),
    ) { entries, account ->
        WearEntriesPayload(
            account = account,
            entries = entries.map(EntryModel::toWearEntry)
        )
    }
        .map(WearSyncCodec::encode)
        .distinctUntilChanged { old, new -> old.contentEquals(new) }

    private fun observeWatches(): Flow<Set<Node>> = callbackFlow<Set<Node>> {
        val listener = CapabilityClient.OnCapabilityChangedListener { info -> trySend(info.nodes) }

        Wearable.getCapabilityClient(context)
            .addListener(listener, WearSyncProtocol.WEAR_CAPABILITY)

        awaitClose { Wearable.getCapabilityClient(context).removeListener(listener) }
    }.catch { error -> Timber.w(error, "Unable to observe Wear OS watches") }

    private suspend fun pushToWatches(payload: ByteArray) {
        try {
            Wearable.getCapabilityClient(context)
                .getCapability(WearSyncProtocol.WEAR_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
                .await()
                .nodes
                .forEach { node -> send(node.id, payload) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Timber.w(error, "Unable to look up Wear OS watches")
        }
    }

    private suspend fun send(nodeId: String, payload: ByteArray) {
        if (payload.size > MAX_PAYLOAD_BYTES) {
            Timber.w("Wear OS payload too large (%d bytes), skipping", payload.size)
            return
        }

        try {
            Wearable.getMessageClient(context)
                .sendMessage(nodeId, WearSyncProtocol.PATH_ENTRIES, payload)
                .await()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            Timber.w(error, "Unable to send entries to Wear OS node")
        }
    }

    private companion object {

        private const val REQUEST_TIMEOUT_MILLIS = 15_000L

        // MessageClient messages are limited to 100 KB.
        private const val MAX_PAYLOAD_BYTES = 100_000

    }

}

private fun EntryModel.toWearEntry() = WearEntry(
    id = id,
    name = name,
    issuer = issuer,
    secret = secret,
    period = period,
    digits = digits,
    algorithm = when (algorithm) {
        EntryAlgorithm.SHA1 -> WearEntryAlgorithm.SHA1
        EntryAlgorithm.SHA256 -> WearEntryAlgorithm.SHA256
        EntryAlgorithm.SHA512 -> WearEntryAlgorithm.SHA512
    },
    type = when (type) {
        EntryType.TOTP -> WearEntryType.TOTP
        EntryType.STEAM -> WearEntryType.STEAM
    },
    position = position
)
