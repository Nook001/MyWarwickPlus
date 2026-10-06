package uk.ac.warwick.plus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.IOException
import kotlinx.coroutines.*
import uk.ac.warwick.plus.config.AppActions
import uk.ac.warwick.plus.config.AppLabels
import uk.ac.warwick.plus.data.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiProbeSheet(probe: (ProbeEndpoint) -> ProbeResult, onLogin: () -> Unit, onDismiss: () -> Unit) {
    var endpoint by remember { mutableStateOf(ProbeEndpoint.COURSEWORK) }
    var result by remember { mutableStateOf<ProbeResult?>(null) }
    var problem by remember { mutableStateOf<String?>(null) }
    var loginRequired by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    DetailsSheet(onDismiss, itemSpacing = 16.dp) {
        item {
            Text(AppLabels.DEVELOPER_TOOLS, style = MaterialTheme.typography.headlineSmall)
            Text("Debug build · manual, read-only requests. Shows field names and counts; no credentials or personal values.",
                style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Column {
                ProbeEndpoint.entries.forEach { option ->
                    FilterChip(selected = endpoint == option, enabled = !busy,
                        onClick = { endpoint = option; result = null; problem = null; loginRequired = false },
                        label = { Text(option.tile.replaceFirstChar { it.uppercase() }) })
                }
            }
        }
        item {
            Text("GET ${endpoint.path}", style = MaterialTheme.typography.bodySmall)
            Button(enabled = !busy, onClick = {
                busy = true; result = null; problem = null; loginRequired = false
                val requested = endpoint
                scope.launch {
                    try { result = withContext(Dispatchers.IO) { probe(requested) } }
                    catch (error: Exception) {
                        if (error is CancellationException) throw error
                        loginRequired = error is SignInRequiredException
                        problem = when (error) {
                            is SignInRequiredException -> "Sign in to make this request."
                            is ServiceException -> "HTTP ${error.status} · service unavailable"
                            is IOException -> "Connection failed"
                            else -> "The response couldn't be interpreted."
                        }
                    } finally { busy = false }
                }
            }) { Text(if (busy) "Requesting…" else "Run request") }
        }
        if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        result?.let { value -> item {
            Text("HTTP ${value.httpStatus} · ${value.elapsedMillis} ms\nEnvelope success: ${value.success}\n" +
                "Items: ${value.itemCount ?: "not provided"}\nContent fields: ${value.contentFields.joinToString()}\n" +
                "First item fields: ${value.itemFields.joinToString().ifBlank { "no item available" }}",
                style = MaterialTheme.typography.bodyMedium)
            if (value.itemCount == 0) Text("An empty list doesn't establish the item schema or confirm full service coverage.",
                style = MaterialTheme.typography.bodySmall)
        } }
        if (problem != null) item { Text(problem!!); if (loginRequired) TextButton(onClick = { onDismiss(); onLogin() }) { Text(AppActions.SIGN_IN) } }
        item { TextButton(onClick = onDismiss) { Text(AppActions.CLOSE) } }
    }
}
