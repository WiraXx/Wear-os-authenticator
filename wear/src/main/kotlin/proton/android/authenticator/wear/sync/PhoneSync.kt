package proton.android.authenticator.wear.sync

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import proton.android.authenticator.shared.wearsync.WearSyncProtocol
import kotlin.coroutines.cancellation.CancellationException

sealed interface PhoneSyncResult {

    /** The request reached at least one phone running the matching app. */
    data object Requested : PhoneSyncResult

    /** The watch is not connected to any device (Bluetooth off, not paired, phone away). */
    data object NoConnectedDevice : PhoneSyncResult

    /** A device is connected but the matching phone app (same package + signature) is not there. */
    data object PhoneAppMissing : PhoneSyncResult

    data class Failed(val code: String) : PhoneSyncResult

}

object PhoneSync {

    /** Asks every reachable paired phone running this app to push its entries. */
    suspend fun requestSync(context: Context): PhoneSyncResult = try {
        val phones = Wearable.getCapabilityClient(context)
            .getCapability(WearSyncProtocol.PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
            .nodes

        if (phones.isEmpty()) {
            val connected = Wearable.getNodeClient(context).connectedNodes.await()

            if (connected.isEmpty()) PhoneSyncResult.NoConnectedDevice else PhoneSyncResult.PhoneAppMissing
        } else {
            phones.forEach { node ->
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, WearSyncProtocol.PATH_REQUEST, ByteArray(0))
                    .await()
            }

            PhoneSyncResult.Requested
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: ApiException) {
        PhoneSyncResult.Failed("API ${error.statusCode}")
    } catch (error: Exception) {
        PhoneSyncResult.Failed(error.javaClass.simpleName)
    }

}
