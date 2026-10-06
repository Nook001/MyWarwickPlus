package uk.ac.warwick.plus.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call

// A request scope exists before dispatch, so cancellation cannot miss a newly registered call.
internal class CancellableRequests {
    private class Scope {
        private var cancelled = false
        private var call: Call? = null

        @Synchronized fun attach(next: Call) {
            if (cancelled) next.cancel() else call = next
        }
        @Synchronized fun cancel() {
            cancelled = true
            call?.cancel()
        }
        @Synchronized fun ensureActive() {
            if (cancelled) throw CancellationException("Request cancelled")
        }
    }

    private val current = ThreadLocal<Scope>()

    fun attach(call: Call) { current.get()?.attach(call) }

    suspend fun <T> run(operation: () -> T): T {
        val finished = CompletableDeferred<Unit>()
        try {
            return suspendCancellableCoroutine { continuation ->
                val scope = Scope()
                continuation.invokeOnCancellation { scope.cancel() }
                Dispatchers.IO.dispatch(continuation.context, Runnable {
                    current.set(scope)
                    try {
                        scope.ensureActive()
                        continuation.resumeWith(Result.success(operation()))
                    } catch (error: Throwable) {
                        continuation.resumeWith(Result.failure(error))
                    } finally {
                        current.remove()
                        finished.complete(Unit)
                    }
                })
            }
        } finally {
            // Join the cancelled IO work before releasing the store lock/clearing cookies.
            // This also prevents a late CookieJar callback from restoring the old session.
            withContext(NonCancellable) { finished.await() }
        }
    }
}
