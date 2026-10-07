package uk.ac.warwick.plus.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
import uk.ac.warwick.plus.data.*

@Composable
internal fun SyncProgressBar(progress: SyncProgress?) {
    if (progress == null) return
    key(progress.id) {
        var visible by remember { mutableStateOf(!progress.finished) }
        LaunchedEffect(progress.finished) {
            if (progress.finished) { delay(250); visible = false }
        }
        val description = progress.description.render()
        val fraction by animateFloatAsState(progress.fraction, animationSpec = tween(200), label = "API progress")
        if (visible) LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth()
            .semantics { contentDescription = description },
            color = if (progress.finished && progress.failures > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
    }
}
