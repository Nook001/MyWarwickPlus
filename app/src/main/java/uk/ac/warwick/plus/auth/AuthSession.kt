package uk.ac.warwick.plus.auth

import kotlinx.coroutines.suspendCancellableCoroutine

import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.withContext

import android.net.Uri

import android.webkit.WebView

import android.webkit.WebStorage

import android.content.Context

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import uk.ac.warwick.plus.traceWork

const val MY_WARWICK = "https://my.warwick.ac.uk"

/** WebView is the cookie authority. Never copy a Warwick-wide cookie to another host. */
class AuthSession(providedManager: CookieManager? = null) : CookieJar {
    // Do not load the WebView provider while constructing the UI's repository.
    // Native requests first access this on IO; explicit sign-out still runs on Main.
    // Cookie acceptance defaults to true; LoginActivity configures third-party SSO cookies.
    private val manager by lazy {
        traceWork("MWP.CookieManager.init") { providedManager ?: CookieManager.getInstance() }
    }
    suspend fun clear(context: Context) = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { continuation ->
            manager.removeAllCookies { if (continuation.isActive) continuation.resumeWith(Result.success(Unit)) }
        }
        manager.flush()
        WebStorage.getInstance().deleteAllData()
        WebView(context.applicationContext).apply { clearCache(true); clearHistory(); destroy() }
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!isApiOrigin(url)) return emptyList()
        return manager.getCookie(url.toString()).orEmpty().split(';').mapNotNull { part ->
            val pair = part.trim().split('=', limit = 2)
            if (pair.size != 2) return@mapNotNull null
            // CookieManager already selected domain/path/expiry. Restrict the copied cookie
            // to this exact host; HttpOnly does not prohibit access by the Android host app.
            runCatching {
                Cookie.Builder().name(pair[0]).value(pair[1])
                    .hostOnlyDomain(url.host).path("/").secure().build()
            }.getOrNull()
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (!isApiOrigin(url)) return
        cookies.forEach { manager.setCookie(url.toString(), it.toString()) }
        if (cookies.isNotEmpty()) manager.flush()
    }

    companion object {
        fun isApiOrigin(url: HttpUrl) = url.scheme == "https" && url.host == "my.warwick.ac.uk" && url.port == 443
        fun isLoginUrl(url: Uri): Boolean = url.scheme == "https" && url.port in listOf(-1, 443) &&
            (url.host == "warwick.ac.uk" || url.host?.endsWith(".warwick.ac.uk") == true ||
                url.host in setOf("login.microsoftonline.com", "login.live.com"))
    }
}
