package uk.ac.warwick.plus

import org.junit.Assert.*
import org.junit.Test
import uk.ac.warwick.plus.data.FeedEntry
import uk.ac.warwick.plus.data.FeedKind
import uk.ac.warwick.plus.ui.*

class RecoveryActionTest {
    @Test fun delayedActionRespectsCurrentSessionAndOperationGuards() {
        val retry = RecoveryAction.Refresh(SyncResource.COURSEWORK)
        val ready = TimetableState(signedIn = true)
        assertEquals(retry, retry.resolve(ready))
        assertEquals(RecoveryAction.SignIn, retry.resolve(ready.copy(needsLogin = true, signedIn = false)))
        assertEquals(RecoveryAction.RefreshAll, RecoveryAction.SignIn.resolve(ready))
        assertNull(retry.resolve(ready.copy(busy = true)))
        assertNull(retry.resolve(ready.copy(signingOut = true)))
        assertNull(retry.resolve(ready.copy(logoutFailed = true, needsLogin = true)))
    }

    @Test fun anOldPaginationNoticeCannotReuseAClearedCursor() {
        val failedPage = FeedState(entries = listOf(FeedEntry().apply { id = "cursor" }),
            hasMore = true, olderPageFailed = true)
        val state = TimetableState(feeds = mapOf(FeedKind.MESSAGES to failedPage))
        assertEquals(RecoveryAction.OlderMessages, RecoveryAction.OlderMessages.resolve(state))
        for (replacement in listOf(FeedState(), failedPage.copy(olderPageFailed = false),
            failedPage.copy(hasMore = false))) {
            assertEquals(RecoveryAction.Refresh(SyncResource.MESSAGES),
                RecoveryAction.OlderMessages.resolve(state.copy(feeds = mapOf(FeedKind.MESSAGES to replacement))))
        }
    }
}
