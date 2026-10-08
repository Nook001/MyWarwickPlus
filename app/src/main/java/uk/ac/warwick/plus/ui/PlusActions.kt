package uk.ac.warwick.plus.ui

internal data class PlusActions(
    val refresh: () -> Unit,
    val signIn: () -> Unit,
    val signOut: () -> Unit,
    val refreshResource: (SyncResource) -> Unit,
    val loadOlderMessages: () -> Unit,
    val consumeNotice: (Long) -> Unit,
    val openExternal: ((String) -> Unit)? = null
)
