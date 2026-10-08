package uk.ac.warwick.plus.ui.components

import uk.ac.warwick.plus.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.ac.warwick.plus.config.AppActions

private val SearchIcon = ImageVector.Builder("Search", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
        moveTo(16f, 10f); curveTo(16f, 13.3f, 13.3f, 16f, 10f, 16f)
        curveTo(6.7f, 16f, 4f, 13.3f, 4f, 10f); curveTo(4f, 6.7f, 6.7f, 4f, 10f, 4f)
        curveTo(13.3f, 4f, 16f, 6.7f, 16f, 10f); close()
        moveTo(14.5f, 14.5f); lineTo(21f, 21f)
    }
}.build()

private val ClearIcon = ImageVector.Builder("Clear", 24.dp, 24.dp, 24f, 24f).apply {
    path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 2f) {
        moveTo(6f, 6f); lineTo(18f, 18f); moveTo(18f, 6f); lineTo(6f, 18f)
    }
}.build()

@Composable
internal fun SearchField(query: String, onQueryChange: (String) -> Unit, placeholder: String,
    modifier: Modifier = Modifier, compact: Boolean = false, onSearch: () -> Unit) {
    val textStyle = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium
    AppCard(shape = AppShapes.search, tone = CardTone.Quiet, modifier = modifier.fillMaxWidth()) {
        BasicTextField(query, onQueryChange, singleLine = true,
            textStyle = textStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier.fillMaxWidth(), decorationBox = { input ->
                Row(Modifier.heightIn(min = 48.dp).padding(start = 14.dp, end = if (query.isEmpty()) 14.dp else 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(SearchIcon, placeholder, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        if (query.isEmpty()) Text(placeholder, style = textStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        input()
                    }
                    if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(48.dp)) {
                        Icon(ClearIcon, stringResource(AppActions.CLEAR_SEARCH), Modifier.size(18.dp))
                    }
                }
            })
    }
}
