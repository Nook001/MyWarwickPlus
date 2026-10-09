package uk.ac.warwick.plus.auth

import android.annotation.SuppressLint
import android.content.Context
import android.net.http.SslError
import android.os.SystemClock
import android.webkit.*
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import uk.ac.warwick.plus.debugLog
import kotlin.coroutines.resume

/**
 * Follows the refresh URL from `/user/info` in an offscreen WebView, as the official app lets
 * its page do. It completes only when Warwick SSO can sign in without the user; any page that
 * needs a password or MFA simply times out and the normal sign-in prompt remains.
 */
class SessionRefresher(private val context: Context) {
    private val mutex = Mutex()
    private var failedAt = 0L

    suspend fun refresh(url: String): Boolean = mutex.withLock {
        val uri = url.toUri()
        if (!AuthSession.isLoginUrl(uri)) return false
        // Each resource authenticates separately; one failed round trip must not stall them all.
        val now = SystemClock.elapsedRealtime()
        if (failedAt != 0L && now - failedAt < RETRY_AFTER_MILLIS) return false
        val reached = withTimeoutOrNull(TIMEOUT_MILLIS) { load(url) } == true
        failedAt = if (reached) 0L else now
        debugLog { "Silent session refresh ${if (reached) "returned to MyWarwick" else "needs interactive sign-in"}" }
        reached
    }

    @SuppressLint("SetJavaScriptEnabled") // SSO hand-offs auto-submit forms; navigation stays on login hosts.
    private suspend fun load(url: String): Boolean = withContext(Dispatchers.Main) {
        ServiceWorkerController.getInstance().serviceWorkerWebSettings.blockNetworkLoads = true
        val view = WebView(context.applicationContext)
        try {
            suspendCancellableCoroutine { continuation ->
                fun finish(result: Boolean) { if (continuation.isActive) continuation.resume(result) }
                view.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    setSupportMultipleWindows(false)
                }
                CookieManager.getInstance().setAcceptThirdPartyCookies(view, true)
                view.webChromeClient = object : WebChromeClient() {
                    override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
                }
                view.webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame || AuthSession.isLoginUrl(request.url)) return false
                        finish(false)
                        return true
                    }
                    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                        handler.cancel()
                        finish(false)
                    }
                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (request.isForMainFrame) finish(false)
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        CookieManager.getInstance().flush()
                        if (url.toUri().host == "my.warwick.ac.uk") finish(true)
                    }
                }
                view.loadUrl(url)
            }
        } finally {
            view.stopLoading()
            view.clearCache(true)
            WebStorage.getInstance().deleteOrigin(MY_WARWICK)
            view.destroy()
        }
    }

    private companion object {
        const val TIMEOUT_MILLIS = 20_000L
        const val RETRY_AFTER_MILLIS = 10 * 60_000L
    }
}
