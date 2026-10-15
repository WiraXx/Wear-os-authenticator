package proton.android.authenticator.shared.wearsync

/**
 * Wire contract between the phone app and the Wear OS app (Wearable Data Layer).
 *
 * The watch asks for the entries with [PATH_REQUEST]; the phone answers with, and pushes on every
 * change, a [WearEntriesPayload] on [PATH_ENTRIES]. Messages are transient: nothing is persisted
 * by Google Play Services, the watch keeps its own encrypted copy.
 */
object WearSyncProtocol {

    const val PHONE_CAPABILITY: String = "proton_authenticator_phone"

    const val WEAR_CAPABILITY: String = "proton_authenticator_wear"

    const val PATH_PREFIX: String = "/proton_authenticator"

    const val PATH_ENTRIES: String = "$PATH_PREFIX/entries"

    const val PATH_REQUEST: String = "$PATH_PREFIX/request"

    const val VERSION: Int = 1

}
