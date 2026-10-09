package uk.ac.warwick.plus

import uk.ac.warwick.plus.ui.PlusActions
import uk.ac.warwick.plus.ui.SyncResource

internal fun testActions(refresh: () -> Unit = {}, signIn: () -> Unit = {}, signOut: () -> Unit = {},
    refreshResource: (SyncResource) -> Unit = {}, loadOlderMessages: () -> Unit = {}, consumeNotice: (Long) -> Unit = {},
    openExternal: ((String) -> Unit)? = null) =
    PlusActions(refresh, signIn, signOut, refreshResource, loadOlderMessages, consumeNotice, openExternal)
