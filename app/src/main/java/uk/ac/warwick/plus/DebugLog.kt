package uk.ac.warwick.plus

import android.util.Log

internal inline fun debugLog(message: () -> String) {
    if (BuildConfig.DEBUG) Log.i("MyWarwickPlus", message())
}

internal fun reportSyncFailure(error: Exception) {
    // Only the exception type: no response contents, credentials or personal fields.
    if (BuildConfig.DEBUG) Log.w("MyWarwickPlus", "Student data sync failed: ${error.javaClass.simpleName}")
}
