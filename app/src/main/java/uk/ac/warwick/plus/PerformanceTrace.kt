package uk.ac.warwick.plus

import android.os.Trace

/** Fixed labels only; synchronous regions must begin/end on the same thread. */
internal inline fun <T> traceWork(label: String, operation: () -> T): T {
    if (!BuildConfig.PERFORMANCE_TRACING) return operation()
    Trace.beginSection(label)
    return try { operation() } finally { Trace.endSection() }
}
