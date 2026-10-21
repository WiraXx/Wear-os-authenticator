package proton.android.authenticator.wear.sync

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import proton.android.authenticator.shared.wearsync.WearSyncProtocol
import kotlin.coroutines.cancellation.CancellationException

sealed interface PhoneSyncResult {

    /**
     * The request was sent. [viaCapability] is `true` when the phone advertised the app; otherwise it
     * was sent blindly to every connected [devices] and only the phone app (same package + signature)
     * can answer it.
     */
    data class Requested(val viaCapability: Boolean, val devices: String) : PhoneSyncResult

    /** The watch is not connected to any device (Bluetooth off, not paired, phone away). */
    data object NoConnectedDevice : PhoneSyncResult

    data class Failed(val code: String) : PhoneSyncResult

}

object PhoneSync {

    /**
     * Asks the paired phone to push its entries. Prefers the phones advertising the app; when none
     * does (capabilities can be slow to propagate) the request goes to every connected device.
     */
    suspend fun requestSync(context: Context): PhoneSyncResult = try {
        val phones = Wearable.getCapabilityClient(context)
            .getCapability(WearSyncProtocol.PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
            .nodes

        val targets = if (phones.isNotEmpty()) {
            phones.toList()
        } else {
            Wearable.getNodeClient(context).connectedNodes.await()
        }

        if (targets.isEmpty()) {
            PhoneSyncResult.NoConnectedDevice
        } else {
            targets.forEach { node ->
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, WearSyncProtocol.PATH_REQUEST, ByteArray(0))
                    .await()
            }

            PhoneSyncResult.Requested(
                viaCapability = phones.isNotEmpty(),
                devices = targets.joinToString { node -> node.displayName }
            )
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: ApiException) {
        PhoneSyncResult.Failed("API ${error.statusCode}")
    } catch (error: Exception) {
        PhoneSyncResult.Failed(error.javaClass.simpleName)
    }

}
