package proton.android.authenticator.wear.sync

import android.content.Context
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.cancellation.CancellationException
import proton.android.authenticator.shared.wearsync.WearSyncProtocol

object PhoneSync {

    /** Asks every reachable paired phone to push its entries. Returns `false` if none is reachable. */
    suspend fun requestSync(context: Context): Boolean = try {
        val nodes = Wearable.getCapabilityClient(context)
            .getCapability(WearSyncProtocol.PHONE_CAPABILITY, CapabilityClient.FILTER_REACHABLE)
            .await()
            .nodes

        nodes.forEach { node ->
            Wearable.getMessageClient(context)
                .sendMessage(node.id, WearSyncProtocol.PATH_REQUEST, ByteArray(0))
                .await()
        }

        nodes.isNotEmpty()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        false
    }

}
