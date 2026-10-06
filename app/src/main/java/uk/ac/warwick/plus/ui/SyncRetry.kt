package uk.ac.warwick.plus.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import uk.ac.warwick.plus.data.ServiceException
import java.io.IOException

private val RetryDelays = longArrayOf(2_000, 5_000)

// The budget belongs to one resource (including its account check), never to the full refresh.
internal suspend fun <T> withSyncRetry(onRetry: (Int) -> Unit, operation: suspend () -> T): T {
    for (attempt in 0..RetryDelays.size) {
        currentCoroutineContext().ensureActive()
        try { return operation() } catch (error: Exception) {
            val transient = error is IOException || error is ServiceException &&
                (error.status == 408 || error.status in 500..599)
            if (error is CancellationException || !transient || attempt == RetryDelays.size) throw error
            onRetry(attempt + 1)
            delay(RetryDelays[attempt])
        }
    }
    error("Unreachable retry state")
}
