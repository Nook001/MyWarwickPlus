package uk.ac.warwick.plus.auth

import uk.ac.warwick.plus.R
import uk.ac.warwick.plus.config.AppActions
import uk.ac.warwick.plus.ui.UiText
import uk.ac.warwick.plus.ui.text
import uk.ac.warwick.plus.ui.render
import androidx.compose.ui.res.stringResource
import android.net.http.SslError
import android.graphics.Bitmap
import android.app.Activity
import android.annotation.SuppressLint
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import android.os.Bundle
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uk.ac.warwick.plus.ui.PlusTheme
import uk.ac.warwick.plus.PlusApplication
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Login stays on the official service. No JavaScript bridge or credential interception. */
class LoginActivity : ComponentActivity() {
    private var webView: WebView? = null
    private var checking = false
    private var loadingProgress by mutableIntStateOf(0)
    private var problem by mutableStateOf<UiText?>(null)
    private var pageHost by mutableStateOf("my.warwick.ac.uk")

    @SuppressLint("SetJavaScriptEnabled") // Warwick SSO requires JavaScript; navigation and permissions remain restricted.
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appearance by (application as PlusApplication).appearance.state.collectAsStateWithLifecycle()
            PlusTheme(appearance) {
                BackHandler {
                    if (webView?.canGoBack() == true) webView?.goBack() else finish()
                }
                Scaffold(topBar = {
                    TopAppBar(title = { Column {
                        Text(stringResource(R.string.sign_in_to_warwick), style = MaterialTheme.typography.titleMedium)
                        Text(pageHost, style = MaterialTheme.typography.labelSmall)
                    } }, navigationIcon = { TextButton(onClick = { finish() }) { Text(stringResource(AppActions.CLOSE)) } })
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        if (loadingProgress < 100) LinearProgressIndicator(Modifier.fillMaxWidth())
                        problem?.let { message ->
                            Text(message.render(), Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { problem = null; webView?.loadUrl(MY_WARWICK) }) { Text(stringResource(AppActions.RETRY)) }
                        }
                        AndroidView(modifier = Modifier.fillMaxSize(), factory = { context ->
                            WebView(context).apply {
                                webView = this
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = false
                                settings.allowContentAccess = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                settings.setSupportMultipleWindows(false)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView, value: Int) { loadingProgress = value }
                                    override fun onPermissionRequest(request: PermissionRequest) { request.deny() }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                        if (!request.isForMainFrame) return false
                                        if (AuthSession.isLoginUrl(request.url)) return false
                                        problem = text(R.string.login_external_link)
                                        return true
                                    }
                                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                        pageHost = url.toUri().host.orEmpty()
                                    }
                                    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                                        handler.cancel()
                                        problem = text(R.string.login_unverified_connection)
                                    }
                                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                        if (request.isForMainFrame) problem = text(R.string.login_connection_failed)
                                    }
                                    override fun onPageFinished(view: WebView, url: String) {
                                        CookieManager.getInstance().flush()
                                        if (url.toUri().host != "my.warwick.ac.uk" || checking) return
                                        checking = true
                                        lifecycleScope.launch {
                                            try {
                                                val api = (application as PlusApplication).api
                                                val signedIn = try {
                                                    api.request { api.user() }
                                                    true
                                                } catch (error: Exception) {
                                                    if (error is CancellationException) throw error
                                                    false
                                                }
                                                if (signedIn) {
                                                    setResult(Activity.RESULT_OK)
                                                    finish()
                                                }
                                            } finally { checking = false }
                                        }
                                    }
                                }
                                if (savedInstanceState == null) loadUrl(MY_WARWICK)
                                else restoreState(savedInstanceState)
                            }
                        })
                    }
                }
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        webView?.saveState(outState)
        super.onSaveInstanceState(outState)
    }
    override fun onDestroy() {
        webView?.apply { stopLoading(); destroy() }
        webView = null
        super.onDestroy()
    }
}
