package uk.ac.warwick.plus.ui

import androidx.core.net.toUri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import uk.ac.warwick.plus.data.safeExternalUrl

/** One launcher for all user-triggered external navigation; callers choose visible feedback. */
@Composable
internal fun rememberBrowserOpener(onOpen: ((String) -> Unit)? = null): (String) -> Boolean {
    val context = LocalContext.current
    val currentOpen by rememberUpdatedState(onOpen)
    return remember(context) {
        { raw ->
            val url = safeExternalUrl(raw)
            if (url == null) false else try {
                val open = currentOpen
                if (open != null) open(url)
                else CustomTabsIntent.Builder().build().launchUrl(context, url.toUri())
                true
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                false
            }
        }
    }
}

/** Sheet-local feedback stays visible above the modal scrim. */
@Composable
internal fun ExternalLinkButton(url: String, label: String, onOpen: ((String) -> Unit)? = null) {
    val open = rememberBrowserOpener(onOpen)
    var failed by remember(url) { mutableStateOf(false) }
    OutlinedButton(onClick = { failed = !open(url) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
    if (failed) Text(androidx.compose.ui.res.stringResource(uk.ac.warwick.plus.R.string.link_open_failed), style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error)
}
