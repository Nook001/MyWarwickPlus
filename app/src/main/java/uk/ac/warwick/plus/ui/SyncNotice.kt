package uk.ac.warwick.plus.ui

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
        needsLogin -> "Sign in to update your information."
        failures.size > 1 -> "Couldn't update some information. Your saved data has been kept."
        else -> failures.single().second
    }
    return SyncNotice(id, text, action)
}
