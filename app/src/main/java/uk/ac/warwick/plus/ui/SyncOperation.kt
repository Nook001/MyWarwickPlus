package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.FeedKind

sealed interface SyncOperation {
    data class Refresh(val target: SyncResource) : SyncOperation
    data object OlderMessages : SyncOperation

    val resource: SyncResource get() = when (this) {
        is Refresh -> target
        OlderMessages -> SyncResource.MESSAGES
    }
    val label: String get() = when (this) {
        is Refresh -> target.label
        OlderMessages -> AppLabels.OLDER_MESSAGES
    }
}

sealed interface RecoveryAction {
    data object RefreshAll : RecoveryAction
    data object SignIn : RecoveryAction
    data class Refresh(val resource: SyncResource) : RecoveryAction
    data object OlderMessages : RecoveryAction
}

// Resolve at click time, after a Snackbar may have waited through a session/cache change.
internal fun RecoveryAction.resolve(state: TimetableState): RecoveryAction? {
    if (state.signingOut || state.logoutFailed || state.busy) return null
    if (state.needsLogin) return RecoveryAction.SignIn
    return when (this) {
        RecoveryAction.SignIn -> RecoveryAction.RefreshAll
        RecoveryAction.OlderMessages -> {
            val messages = state.feed(FeedKind.MESSAGES)
            if (messages.olderPageFailed && messages.hasMore && messages.entries.isNotEmpty()) this
            else RecoveryAction.Refresh(SyncResource.MESSAGES)
        }
        else -> this
    }
}
