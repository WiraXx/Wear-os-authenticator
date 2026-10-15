package proton.android.authenticator.shared.wearsync

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@Serializable
enum class WearEntryType {
    @SerialName("totp")
    TOTP,

    @SerialName("steam")
    STEAM
}

@Serializable
enum class WearEntryAlgorithm {
    @SerialName("sha1")
    SHA1,

    @SerialName("sha256")
    SHA256,

    @SerialName("sha512")
    SHA512
}

@Serializable
data class WearEntry(
    val id: String,
    val name: String,
    val issuer: String,
    val secret: String,
    val period: Int,
    val digits: Int,
    val algorithm: WearEntryAlgorithm,
    val type: WearEntryType,
    val position: Int
) {

    val title: String get() = issuer.ifBlank { name }

    val subtitle: String? get() = name.takeIf { issuer.isNotBlank() && it.isNotBlank() && it != issuer }

    // Never leak the secret in logs.
    override fun toString(): String = "WearEntry(id=$id)"

}

@Serializable
data class WearEntriesPayload(
    val version: Int = WearSyncProtocol.VERSION,
    val account: String? = null,
    val entries: List<WearEntry>
)

object WearSyncCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(payload: WearEntriesPayload): ByteArray = json
        .encodeToString(WearEntriesPayload.serializer(), payload)
        .toByteArray(Charsets.UTF_8)

    /** Returns `null` when [bytes] is malformed or comes from a newer, incompatible protocol. */
    fun decode(bytes: ByteArray): WearEntriesPayload? = try {
        json.decodeFromString(WearEntriesPayload.serializer(), bytes.toString(Charsets.UTF_8))
            .takeIf { payload -> payload.version <= WearSyncProtocol.VERSION }
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

}
