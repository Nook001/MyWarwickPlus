package uk.ac.warwick.plus.auth

import android.app.Activity
import android.net.Uri
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.ac.warwick.plus.data.MyWarwickApi
import uk.ac.warwick.plus.ui.PlusTheme
import uk.ac.warwick.plus.PlusApplication
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** Login stays on the official service. No JavaScript bridge or credential interception. */
class LoginActivity : ComponentActivity() {
    private var webView: WebView? = null
    private var checking = false
    private var loadingProgress by mutableIntStateOf(0)
    private var problem by mutableStateOf<String?>(null)
    private var pageHost by mutableStateOf("my.warwick.ac.uk")

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
                        Text("Sign in to Warwick", style = MaterialTheme.typography.titleMedium)
                        Text(pageHost, style = MaterialTheme.typography.labelSmall)
                    } }, navigationIcon = { TextButton(onClick = { finish() }) { Text("Close") } })
                }) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        if (loadingProgress < 100) LinearProgressIndicator(Modifier.fillMaxWidth())
                        problem?.let { text ->
                            Text(text, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                            TextButton(onClick = { problem = null; webView?.loadUrl(MY_WARWICK) }) { Text("Try again") }
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
                                        problem = "This link is outside the Warwick sign-in service."
                                        return true
                                    }
                                    override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                                        pageHost = Uri.parse(url).host.orEmpty()
                                    }
                                    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: android.net.http.SslError) {
                                        handler.cancel()
                                        problem = "The connection couldn't be verified. Please try again later."
                                    }
                                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                                        if (request.isForMainFrame) problem = "Couldn't reach Warwick. Check your connection and try again."
                                    }
                                    override fun onPageFinished(view: WebView, url: String) {
                                        CookieManager.getInstance().flush()
                                        if (Uri.parse(url).host != "my.warwick.ac.uk" || checking) return
                                        checking = true
                                        lifecycleScope.launch {
                                            val signedIn = withContext(Dispatchers.IO) {
                                                runCatching { MyWarwickApi(AuthSession()).user() }.isSuccess
                                            }
                                            checking = false
                                            if (signedIn) {
                                                setResult(Activity.RESULT_OK)
                                                finish()
                                            }
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
