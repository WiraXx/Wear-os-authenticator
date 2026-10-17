package proton.android.authenticator.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CompactChip
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import kotlinx.coroutines.delay
import proton.android.authenticator.shared.wearsync.TotpGenerator
import proton.android.authenticator.shared.wearsync.WearEntriesPayload
import proton.android.authenticator.shared.wearsync.WearEntry
import proton.android.authenticator.wear.R

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data object NoConnectedDevice : SyncStatus
    data object PhoneAppMissing : SyncStatus
    data class Failed(val code: String) : SyncStatus
}

private const val EXPIRING_SOON_SECONDS = 5

@Composable
fun AuthenticatorScreen(
    payload: WearEntriesPayload?,
    syncStatus: SyncStatus,
    onSync: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val epochSeconds by rememberEpochSeconds()

    MaterialTheme {
        Scaffold(
            timeText = { TimeText() },
            vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
            positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
        ) {
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp, start = 8.dp, end = 8.dp)
            ) {
                item {
                    ListHeader {
                        Text(
                            text = payload?.account ?: stringResource(R.string.app_name),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (syncStatus.isProblem) {
                    item { SyncProblem(syncStatus) }
                }

                when {
                    payload == null -> item { Message(stringResource(R.string.waiting_title), stringResource(R.string.waiting_message)) }
                    payload.entries.isEmpty() -> item { Message(stringResource(R.string.no_entries)) }
                    else -> items(payload.entries.sortedBy(WearEntry::position), key = WearEntry::id) { entry ->
                        EntryCard(entry = entry, epochSeconds = epochSeconds)
                    }
                }

                item {
                    CompactChip(
                        onClick = onSync,
                        enabled = syncStatus != SyncStatus.Syncing,
                        label = {
                            Text(
                                text = stringResource(
                                    if (syncStatus == SyncStatus.Syncing) R.string.syncing else R.string.sync
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

private val SyncStatus.isProblem: Boolean
    get() = this !is SyncStatus.Idle && this !is SyncStatus.Syncing

@Composable
private fun SyncProblem(status: SyncStatus) {
    val packageName = LocalContext.current.packageName

    when (status) {
        SyncStatus.NoConnectedDevice -> Message(
            title = stringResource(R.string.no_connected_device_title),
            message = stringResource(R.string.no_connected_device_message)
        )

        SyncStatus.PhoneAppMissing -> Message(
            title = stringResource(R.string.phone_app_missing_title),
            message = stringResource(R.string.phone_app_missing_message, packageName)
        )

        is SyncStatus.Failed -> Message(
            title = stringResource(R.string.sync_failed_title),
            message = status.code
        )

        else -> Unit
    }
}

@Composable
private fun Message(title: String, message: String? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title3
        )
        message?.let {
            Text(
                text = it,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body2
            )
        }
    }
}

@Composable
private fun EntryCard(entry: WearEntry, epochSeconds: Long) {
    val remaining = TotpGenerator.secondsRemaining(entry, epochSeconds)
    val color = if (remaining <= EXPIRING_SOON_SECONDS) {
        MaterialTheme.colors.error
    } else {
        MaterialTheme.colors.primary
    }

    Card(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = entry.title,
            style = MaterialTheme.typography.caption1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        entry.subtitle?.let { subtitle ->
            Text(
                text = subtitle,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = TotpGenerator.generate(entry, epochSeconds).grouped(),
            style = MaterialTheme.typography.title1,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colors.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = remaining / entry.period.coerceAtLeast(1).toFloat())
                    .height(3.dp)
                    .background(color)
            )
        }
    }
}

@Composable
private fun rememberEpochSeconds(): State<Long> = produceState(System.currentTimeMillis() / 1000) {
    while (true) {
        value = System.currentTimeMillis() / 1000
        delay(500)
    }
}

private fun String.grouped(): String = when (length) {
    6 -> "${take(3)} ${drop(3)}"
    8 -> "${take(4)} ${drop(4)}"
    else -> this
}
