package uk.ac.warwick.plus.ui

import uk.ac.warwick.plus.R

internal fun TimetableState.completionNotice(
    id: Long, resource: SyncResource?, olderMessages: Boolean
): SyncNotice? {
    val failures = (resource?.let(::listOf) ?: SyncResource.entries).mapNotNull { kind ->
        issue(kind)?.let { kind to it }
    }
    if (failures.isEmpty()) return null
    val action = when {
        olderMessages -> RecoveryAction.OlderMessages
        resource != null -> RecoveryAction.Refresh(resource)
        else -> failures.singleOrNull()?.let { RecoveryAction.Refresh(it.first) } ?: RecoveryAction.RefreshAll
    }
    val text = when {
        needsLogin -> text(R.string.sign_in_update_information)
        failures.size > 1 -> text(R.string.information_update_failed)
        else -> failures.single().second
    }
    return SyncNotice(id, text, action)
}
