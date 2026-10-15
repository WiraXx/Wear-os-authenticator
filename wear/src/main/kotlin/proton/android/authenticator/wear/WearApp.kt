package proton.android.authenticator.wear

import android.app.Application
import android.content.Context
import proton.android.authenticator.wear.data.EntriesRepository

class WearApp : Application() {

    val entriesRepository: EntriesRepository by lazy { EntriesRepository(applicationContext) }

}

val Context.entriesRepository: EntriesRepository
    get() = (applicationContext as WearApp).entriesRepository
