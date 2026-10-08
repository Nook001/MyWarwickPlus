package uk.ac.warwick.plus.ui

import androidx.compose.runtime.Composable

@Composable
internal fun MeTab(state: TimetableState, navigator: PlusNavigator, actions: PlusActions,
    recover: (SyncResource) -> Unit, open: (String) -> Unit) {
    when (val route = navigator.meRoute) {
        is MeRoute.Feed -> FeedContent(route.kind, state.feed(route.kind), state.busy, state.needsLogin,
            { recover(SyncResource.forFeed(route.kind)) }, actions.loadOlderMessages,
            { navigator.detail = DetailSelection.Feed(route.kind, it.id) }, open, actions.signIn)
        else -> MoreContent(state, actions.signIn, actions.signOut,
            { navigator.meRoute = MeRoute.Feed(it) }, open,
            onSettings = { navigator.meRoute = MeRoute.Settings }, onResourceRefresh = recover)
    }
}
