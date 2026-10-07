package uk.ac.warwick.plus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DetailsSheet(onDismiss: () -> Unit, modifier: Modifier = Modifier,
    contentPadding: PaddingValues = Spacing.detailPage, itemSpacing: Dp = 20.dp,
    content: LazyListScope.() -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss = rememberUpdatedState(onDismiss)
    var closing by remember { mutableStateOf(false) }
    val close: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                try {
                    sheetState.hide()
                    if (!sheetState.isVisible) dismiss.value()
                } finally { closing = false }
            }
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        CompositionLocalProvider(LocalDetailDismiss provides close) {
            LazyColumn(modifier, contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(itemSpacing), content = content)
        }
    }
}

private val LocalDetailDismiss = staticCompositionLocalOf<(() -> Unit)?> { null }

@Composable
internal fun DetailHeader(label: String, title: String, gap: Dp = 0.dp) {
    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
        DetailEyebrow(label)
        DetailTitle(title)
    }
}

@Composable
internal fun DetailEyebrow(label: String) = Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

@Composable
internal fun DetailTitle(title: String) = Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)

@Composable
internal fun DetailField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun DetailClose(label: String, onDismiss: () -> Unit) {
    TextButton(onClick = LocalDetailDismiss.current ?: onDismiss, modifier = Modifier.fillMaxWidth()) { Text(label) }
}
